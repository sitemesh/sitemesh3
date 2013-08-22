package org.sitemesh.webapp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.CharBuffer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.config.PathBasedDecoratorSelector;
import org.sitemesh.config.PathMapper;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.contentfilter.ContentBufferingFilter;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;
import org.sitemesh.webapp.contentfilter.Selector;

/**
 * The main SiteMesh Filter.
 *
 * <p>For this to be functional it requires a {@link Selector}, {@link DecoratorSelector}
 * and {@link ContentProcessor}. These must be passed in through the constructor.</p>
 *
 * <p>This filter will not work on its own in a typical Servlet container as the container
 * will not know how to pass in the dependencies. It is designed for programmatic use, or
 * to work with frameworks that can inject dependencies. Alternatively, it can be
 * subclassed.</p>
 *
 * <p>For an easy to configure implementation, use
 * {@link org.sitemesh.config.ConfigurableSiteMeshFilter}.</p>
 *
 * @author Joe Walnes
 * @author Scott Farquhar
 */
public class SiteMeshFilter extends ContentBufferingFilter {

    private final ContentProcessor contentProcessor;
    private final DecoratorSelector<WebAppContext> decoratorSelector;
    private final boolean includeErrorPages;

    /**
     * @param selector Provides the rules for whether SiteMesh should be
     *                 used for a specific request. For a basic implementation, use
     *                 {@link org.sitemesh.webapp.contentfilter.BasicSelector}.
     */
    public SiteMeshFilter(Selector selector,
                          ContentProcessor contentProcessor,
                          DecoratorSelector<WebAppContext> decoratorSelector, boolean includeErrorPages) {
        super(selector);
        if (contentProcessor == null) {
            throw new IllegalArgumentException("contentProcessor cannot be null");
        }
        if (decoratorSelector == null) {
            throw new IllegalArgumentException("decoratorSelector cannot be null");
        }
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
        this.includeErrorPages = includeErrorPages;
    }

    /**
     * @return Whether the content was processed. If false, the original content shall
     *         be written back out.
     */
    @Override
    protected boolean postProcess(String contentType, CharBuffer buffer,
                                  HttpServletRequest request, HttpServletResponse response,
                                  ResponseMetaData metaData)
            throws IOException, ServletException {
        WebAppContext context = createContext(contentType, request, response, metaData);
        Content content = contentProcessor.build(buffer, context);
        if (content == null) {
            return false;
        }

        String[] decoratorPaths = decoratorSelector.selectDecoratorPaths(content, context);
        for (String decoratorPath : decoratorPaths) {
            content = context.decorate(decoratorPath, content);
        }

        if (content == null) {
            return false;
        }
        try {
            content.getData().writeValueTo(response.getWriter());
        } catch (IllegalStateException ise) {  // If getOutputStream() has already been called
            content.getData().writeValueTo(new PrintStream(response.getOutputStream()));
        }
        return true;
    }

    
    @Override public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException,
            ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String exclusionPattern = getSelector().excludePatternInUse(request);
        if (exclusionPattern != null) {
            // Ability to override exclusion by more specific pattern
            if (decoratorSelector instanceof PathBasedDecoratorSelector) {
                PathBasedDecoratorSelector<WebAppContext> pbds = (PathBasedDecoratorSelector<WebAppContext>) decoratorSelector;
                String decoratorPattern = pbds.getPathMapper().getPatternInUse(WebAppContext.getRequestPath(request));
                if(decoratorPattern == null) {
                    // there is no decorator rule for this exclusion pattern
                    filterChain.doFilter(request, response);
                    return;
                }
                if(PathMapper.isMoreSpecific(exclusionPattern, decoratorPattern)){
                    // if the exclusion type is more specific
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }
        super.doFilter(servletRequest, servletResponse, filterChain);
    }
    
    
    /**
     * Create a context for the current request. This method can be overriden to allow for other
     * types of context.
     */
    protected WebAppContext createContext(String contentType, HttpServletRequest request,
                                          HttpServletResponse response, ResponseMetaData metaData) {
        return new WebAppContext(contentType, request, response,
                getFilterConfig().getServletContext(), contentProcessor, metaData, includeErrorPages);
    }

}
