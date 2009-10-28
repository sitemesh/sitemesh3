package org.sitemesh.webapp.contentfilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;

/**
 * Abstract {@link Filter} implementation that writes the main content
 * of a response to a temporary buffer where it can then be post-processed
 * before being served.
 * <p>
 * Subclasses should:
 * <ul>
 * <li>Pass in a {@link Selector} to the constructor: Which provides rules for
 * selecting which requests this filter should be applied to.
 * For a basic implementation, use  {@link BasicSelector}.</li>
 * <li>Implement {@link #postProcess(String, CharBuffer, HttpServletRequest, HttpServletResponse, ResponseMetaData)}:
 * Perform the actual post processing of the content that was buffered.</li>
 * </ul> 
 * <h2>Example</h2>
 * <p>This primitive example creates a Filter that will intercept responses
 * with a MIME type of text/plain, and replace all occurrences of the word
 * 'sheep' with 'cheese'. Yes, it's pointless, but should illustrate usage.</p>
 * <pre>
 * public class SheepToCheeseFilter extends ContentBufferingFilter {
 *   public SheepToCheeseFilter() {
 *     super(new BasicSelector("text/plain"));
 *   }
 *   public boolean postProcess(String contentType, CharBuffer buffer,
 *                              HttpServletRequest request, HttpServletResponse response) {
 *     String text = buffer.toString();
 *     if (!text.contains("sheep")) {
 *       // If no modification is required, returning false will signal
 *       // ContentBufferingFilter to write the original buffer back out.
 *       return false;
 *     }
 *     text = text.replaceAll("sheep", "cheese");
 *     response.getWriter().print(text);
 *     return true;
 *   }
 * }
 * </pre>
 *
 * @author Joe Walnes
 * @author Scott Farquhar
 */
public abstract class ContentBufferingFilter implements Filter {

    private final Selector selector;

    protected ContentBufferingFilter(Selector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector cannot be null");
        }
        this.selector = selector;
    }

    /**
     * @return Whether the content was processed. If false, the original content shall
     *         be written back out.
     */
    protected abstract boolean postProcess(String contentType, CharBuffer buffer,
                                           HttpServletRequest request, HttpServletResponse response,
                                           ResponseMetaData responseMetaData)
            throws IOException, ServletException;

    private FilterConfig filterConfig;
    private ContainerTweaks containerTweaks;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        this.containerTweaks = initContainerTweaks();
    }

    public void destroy() {
        filterConfig = null;
        containerTweaks = null;
    }

    protected ContainerTweaks initContainerTweaks() {
        // TODO: Use correct implementation based on container.
        return new ContainerTweaks();
    }

    protected FilterConfig getFilterConfig() {
        return filterConfig;
    }

    protected ContainerTweaks getContainerTweaks() {
        return containerTweaks;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        FilterConfig filterConfig = getFilterConfig();
        if (filterConfig == null) {
            // TODO: Is this really necessary? Can we survive without init() being called?
            throw new ServletException(getClass().getName() + ".init() has not been called.");
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        ServletContext servletContext = filterConfig.getServletContext();

        if (!selector.shouldBufferForRequest(request)) {
            // Optimization: If the content doesn't need to be buffered,
            // skip the rest of this filter.
            filterChain.doFilter(request, response);
            return;
        }

        if (containerTweaks.shouldAutoCreateSession()) {
            // Some containers (such as Tomcat 4) will not allow sessions
            // to be created in the decorator. (i.e after the
            // response has been committed).
            request.getSession(true);
        }

        try {

            // The main work.
            bufferAndPostProcess(filterChain, request, response, selector);

        } catch (IllegalStateException e) {
            // Some containers (such as WebLogic) throw an IllegalStateException when an error page is served.
            // It may be ok to ignore this. However, for safety it is propegated if possible.
            if (!containerTweaks.shouldIgnoreIllegalStateExceptionOnErrorPage()) {
                throw e;
            }
        } catch (RuntimeException e) {
            if (containerTweaks.shouldLogUnhandledExceptions()) {
                // Some containers (such as Tomcat 4) swallow RuntimeExceptions in filters.
                servletContext.log("Unhandled exception occurred whilst decorating page", e);
            }
            throw e;
        }
    }

    /**
     * Apply next filter/servlet to the buffer, post process the response and
     * send to the real response.
     */
    protected void bufferAndPostProcess(FilterChain filterChain, final HttpServletRequest request,
                                        final HttpServletResponse response, Selector selector) throws IOException, ServletException {

        // Apply next filter/servlet, writing response to buffer.
        final ResponseMetaData metaData = new ResponseMetaData();
        HttpServletResponseBuffer responseBuffer = new HttpServletResponseBuffer(response, metaData, selector) {
            @Override
            public void preCommit() {
                // Ensure both content and decorators are used to generate HTTP caching headers.
                long lastModified = metaData.getLastModified();
                long ifModifiedSince = request.getDateHeader("If-Modified-Since");
                if (lastModified > -1 && !response.containsHeader("Last-Modified")) {
                    if (ifModifiedSince < (lastModified / 1000 * 1000)) {
                        response.setDateHeader("Last-Modified", lastModified);
                    } else {
                        response.reset();
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    }
                }
            }
        };

        filterChain.doFilter(wrapRequest(request), responseBuffer);
        CharBuffer buffer = responseBuffer.getBuffer();

        // If content was buffered, post-process it.
        boolean processed = false;
        if (buffer != null && !responseBuffer.bufferingWasDisabled()) {
            processed = postProcess(responseBuffer.getContentType(), buffer, request, response, metaData);
        }

        if (!response.isCommitted()) {
            responseBuffer.preCommit();
        }

        // If no decoratoes applied, and we have some buffered content, write the original.
        if (buffer != null && !processed) {
            writeOriginal(response, buffer, responseBuffer);
        }

    }

    /**
     * Write out the original unmodified buffer.
     */
    protected void writeOriginal(HttpServletResponse response,
                                 CharBuffer buffer,
                                 HttpServletResponseBuffer responseBuffer) throws IOException {
        if (responseBuffer.isBufferStreamBased()) {
            PrintWriter writer = new PrintWriter(response.getOutputStream());
            writer.append(buffer);
            writer.flush(); // Flush writer to underlying outputStream.
            response.getOutputStream().flush();
        } else {
            PrintWriter writer = response.getWriter();
            writer.append(buffer);
            response.getWriter().flush();
        }
    }

    /**
     * Override to wrap the HttpServletRequest sent to the end point to be buffered.
     */
    protected HttpServletRequest wrapRequest(HttpServletRequest request) {
        return new HttpServletRequestFilterable(request);
    }
}
