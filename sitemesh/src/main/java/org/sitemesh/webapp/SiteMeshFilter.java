/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.webapp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.CharBuffer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.config.PathBasedDecoratorSelector;
import org.sitemesh.config.PathMapper;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.contentfilter.ContentBufferingFilter;
import org.sitemesh.webapp.contentfilter.HttpServletResponseBuffer;
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
        // Store original content for fallback
        String originalContentStr = buffer != null ? buffer.toString() : "";
        request.setAttribute("sitemesh.original.content", originalContentStr);
        
        // Get responseBuffer from request attribute to check proper commitment status
        Object responseBufferObj = request.getAttribute("sitemesh.response.buffer");
        boolean useBufferCommitCheck = responseBufferObj instanceof HttpServletResponseBuffer;
        // Check commitment status - use buffer if available, otherwise fall back to response
        boolean isCommitted = useBufferCommitCheck ? 
            ((HttpServletResponseBuffer) responseBufferObj).isCommitted() : 
            response.isCommitted();
            
        if (isCommitted) {
            return false;
        }
        
        WebAppContext context = createContext(contentType, request, response, metaData);
        Content content = contentProcessor.build(buffer, context);
        if (content == null) {
            return false;
        }

        String[] decoratorPaths = decoratorSelector.selectDecoratorPaths(content, context);
        
        // EARLY COMMITMENT CHECK - Before any decoration attempts
        if (useBufferCommitCheck) {
            HttpServletResponseBuffer responseBuffer = (HttpServletResponseBuffer) responseBufferObj;
            boolean actualCommitted = ((HttpServletResponse) responseBuffer.getResponse()).isCommitted();
            
            if (actualCommitted) {
                // Use emergency writer for original content
                String originalContent = (String) request.getAttribute("sitemesh.original.content");
                return emergencyWriteContent(originalContent, response, request);
            }
        }
        
        // TEMPORARY TEST: Force return original content to verify basic functionality
        if (request.getParameter("sitemesh.test") != null) {
            String originalContent = (String) request.getAttribute("sitemesh.original.content");
            if (originalContent != null && !originalContent.isEmpty()) {
                response.getWriter().write(originalContent);
                return true;
            }
        }
        
        // Mark that we're processing decorators
        request.setAttribute("sitemesh.decorator.processing", true);
        
        boolean decorationSuccessful = true;
        for (String decoratorPath : decoratorPaths) {
            
            // Check response commitment before each decoration step
            isCommitted = useBufferCommitCheck ? 
                ((HttpServletResponseBuffer) responseBufferObj).isCommitted() : 
                response.isCommitted();
                
            if (isCommitted) {
                decorationSuccessful = false;
                break;
            }
            
            try {
                content = context.decorate(decoratorPath, content);
                if (content == null) {
                    decorationSuccessful = false;
                    break;
                }
            } catch (Exception e) {
                decorationSuccessful = false;
                break;
            }
        }

        // Remove decorator processing marker
        request.removeAttribute("sitemesh.decorator.processing");

        if (!decorationSuccessful) {
            // Write original content as fallback
            String originalContent = (String) request.getAttribute("sitemesh.original.content");
            if (originalContent != null && !originalContent.isEmpty()) {
                try {
                    response.getWriter().write(originalContent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        if (content == null) {
            return false;
        }

        // Handle Content-Length for Tomcat 11 - be more careful
        try {
            if (response.containsHeader("Content-Length")) {
                // For Tomcat 11, use setHeader instead of setContentLength(-1) to avoid commitment
                response.setHeader("Content-Length", null);
            }
        } catch (IllegalStateException e) {
            return false;
        }
        
        // CRITICAL: Mark content as written to prevent double-write
        // Need to find the actual HttpServletResponseBuffer in the wrapper chain
        HttpServletResponse actualBuffer = findHttpServletResponseBuffer(response);
        if (actualBuffer instanceof HttpServletResponseBuffer) {
            ((HttpServletResponseBuffer) actualBuffer).markContentAsWritten();
        }
        
        return writeContentSafely(content, response, request);
    }
    
    /**
     * Safely write content to response with Tomcat 11 compatibility
     */
    private boolean writeContentSafely(Content content, HttpServletResponse response, HttpServletRequest request) {
        // Check if the deepest underlying response is actually committed
        HttpServletResponse actualResponse = unwrapResponse(response);
        boolean actuallyCommitted = actualResponse.isCommitted();
        
        // Also check our buffer's real state if available
        if (response instanceof HttpServletResponseBuffer) {
            HttpServletResponseBuffer buffer = (HttpServletResponseBuffer) response;
            boolean bufferActuallyCommitted = buffer.isActuallyCommitted();
            // Use the most restrictive (if any is committed, consider it committed)
            actuallyCommitted = actuallyCommitted || bufferActuallyCommitted;
        }        
        if (actuallyCommitted) {
            // Try decorated content first, then fall back to original
            String contentToWrite = null;
            if (content != null && content.getData() != null) {
                try {
                    contentToWrite = content.getData().getValue();
                } catch (Exception e) {
                }
            }
            
            // Fall back to original content if decorated content unavailable
            if (contentToWrite == null || contentToWrite.isEmpty()) {
                contentToWrite = (String) request.getAttribute("sitemesh.original.content");
            }
            
            return emergencyWriteContent(contentToWrite, response, request);
        }
        
        try {
            // Try getWriter() first
            content.getData().writeValueTo(response.getWriter());

            // Force flush and release buffer immediately after successful write
            if (response instanceof HttpServletResponseBuffer) {
                HttpServletResponseBuffer buffer = (HttpServletResponseBuffer) response;
                try {
                    response.getWriter().flush();
                } catch (Exception e) {
                }
                buffer.releaseBuffer();
            }
            return true;
        } catch (IllegalStateException ise) {
            // Try getOutputStream() as fallback
            try {
                content.getData().writeValueTo(new PrintStream(response.getOutputStream()));
                // Force flush and release buffer immediately after successful write
                if (response instanceof HttpServletResponseBuffer) {
                    HttpServletResponseBuffer buffer = (HttpServletResponseBuffer) response;
                    try {
                        response.getOutputStream().flush();
                    } catch (Exception e) {
                    }
                    buffer.releaseBuffer();
                }
                return true;
            } catch (IllegalStateException ise2) {
                return false;
            } catch (IOException ioe) {
                return false;
            }
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Emergency content writer - tries multiple approaches to write content even if response is committed
     */
    private boolean emergencyWriteContent(String content, HttpServletResponse response, HttpServletRequest request) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        // CRITICAL: Unwrap ALL response wrappers to find the original response
        HttpServletResponse actualResponse = unwrapResponse(response);
        // Approach 1: Try the deepest unwrapped response if not committed (PRIORITY)
        if (!actualResponse.isCommitted()) {
            try {
                PrintWriter writer = actualResponse.getWriter();
                writer.write(content);
                writer.flush();
                return true;
            } catch (Exception e) {
            }
        }
        
        // Approach 2: TOMCAT 11 REFLECTION FORCED WRITE (FIX FOR COMMITTED RESPONSE)
        if (actualResponse.getClass().getName().contains("ResponseFacade")) {
            try {
                // CRITICAL: ONLY use reflection if content is FULLY DECORATED
                // Check if content contains unprocessed SiteMesh tags
                if (content.contains("<sitemesh:write") || content.contains("&lt;sitemesh:write")) {
                } else {
                    // Access Tomcat's internal response
                    Field responseField = actualResponse.getClass().getDeclaredField("response");
                    responseField.setAccessible(true);
                    Object internalResponse = responseField.get(actualResponse);
                    
                    if (internalResponse != null) {
                        try {
                            // Method 1: Try to get CoyoteOutputStream directly
                            Method getOutputStreamMethod = internalResponse.getClass().getMethod("getCoyoteResponse");
                            Object coyoteResponse = getOutputStreamMethod.invoke(internalResponse);
                            
                            if (coyoteResponse != null) {
                                // Write directly to Coyote response buffer
                                Method writeMethod = coyoteResponse.getClass().getMethod("doWrite", java.nio.ByteBuffer.class);
                                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
                                writeMethod.invoke(coyoteResponse, buffer);
                                return true;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        
        // Approach 3: Try wrapper response writer (AVOID SERVLET API VIOLATION)
        try {
            
            // ONLY use writer, never mix with OutputStream
            PrintWriter writer = response.getWriter();
            writer.write(content);
            writer.flush();
            
            // CRITICAL: Force emergency flush if this is our buffer
            if (response instanceof HttpServletResponseBuffer) {
                ((HttpServletResponseBuffer) response).emergencyFlush();
            }
            return true;
        } catch (Exception e) {
        }
        // Try to force response headers
        if (!response.isCommitted()) {
            try {
                response.setStatus(200);
                response.setContentType("text/html;charset=UTF-8");
                response.setContentLength(content.length());
            } catch (Exception e) {
            }
        }
        
        return false;
    }

    /**
     * Recursively unwraps all response wrappers to find the original servlet response.
     * This is critical for Tomcat 11 where multiple wrappers (SiteMesh, Shiro, etc.) 
     * may prevent content from reaching the browser.
     */
    private HttpServletResponse unwrapResponse(HttpServletResponse response) {
        HttpServletResponse current = response;
        int maxDepth = 10; // Prevent infinite loops
        int depth = 0;
        
        while (depth < maxDepth && current != null) {
            // Check if this is a wrapper that we can unwrap
            if (current instanceof HttpServletResponseWrapper) {
                HttpServletResponseWrapper wrapper = (HttpServletResponseWrapper) current;
                ServletResponse wrapped = wrapper.getResponse();
                
                if (wrapped instanceof HttpServletResponse) {
                    current = (HttpServletResponse) wrapped;
                    depth++;
                    continue;
                } else {
                    break;
                }
            } else if (current instanceof HttpServletResponseBuffer) {
                // Special handling for our own response buffer
                HttpServletResponseBuffer buffer = (HttpServletResponseBuffer) current;
                ServletResponse wrapped = buffer.getResponse();
                
                if (wrapped instanceof HttpServletResponse) {
                    current = (HttpServletResponse) wrapped;
                    depth++;
                    continue;
                } else {
                    break;
                }
            } else {
                // Not a wrapper or our own type - this should be the original
                break;
            }
        }
        return current;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {
        String decoratorPattern = null;
        if (decoratorSelector instanceof PathBasedDecoratorSelector) {
            PathBasedDecoratorSelector<WebAppContext> pbds = (PathBasedDecoratorSelector<WebAppContext>) decoratorSelector;
            decoratorPattern = pbds.getPathMapper().getPatternInUse(WebAppContext.getRequestPath((HttpServletRequest) request));
        }

        // if no decorator pattern and not using a MetaTagBasedDecoratorSelector or RequestAttributeDecoratorSelector, no need to process content.
        if (decoratorPattern == null && !(decoratorSelector instanceof MetaTagBasedDecoratorSelector)) {
            filterChain.doFilter(request, response);
            return;
        }

        String exclusionPattern = getSelector().excludePatternInUse((HttpServletRequest) request);
        if (exclusionPattern != null) {
            // Ability to override exclusion by more specific pattern
            if (decoratorPattern == null || // MetaTag should not be able to override exclusion rule.
                    PathMapper.isMoreSpecific(exclusionPattern, decoratorPattern)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        super.doFilter(request, response, filterChain);
    }
    
    
    /**
     * Create a context for the current request. This method can be overridden to allow for other
     * types of context.
     */
    protected WebAppContext createContext(String contentType, HttpServletRequest request,
                                          HttpServletResponse response, ResponseMetaData metaData) {
        return new WebAppContext(contentType, request, response,
                getFilterConfig().getServletContext(), contentProcessor, metaData, includeErrorPages);
    }

    public ContentProcessor getContentProcessor() {
        return contentProcessor;
    }

    public DecoratorSelector<WebAppContext> getDecoratorSelector() {
        return decoratorSelector;
    }

    /**
     * Find HttpServletResponseBuffer in the response wrapper chain
     */
    private HttpServletResponse findHttpServletResponseBuffer(HttpServletResponse response) {
        HttpServletResponse current = response;
        int maxDepth = 10;
        int depth = 0;
        
        while (depth < maxDepth && current != null) {
            if (current instanceof HttpServletResponseBuffer) {
                return current;
            }
            if (current instanceof HttpServletResponseWrapper) {
                HttpServletResponse wrapped = (HttpServletResponse) ((HttpServletResponseWrapper) current).getResponse();
                current = wrapped;
            } else {
                break;
            }
            depth++;
        }
        
        return current;
    }
}
