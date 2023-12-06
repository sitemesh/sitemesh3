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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
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

        // Some servlet container's (Tomcat >8.5) will set the content length to the size of the decorator
        // if it is a static file. Check if content length has already been set and if so, clear it.
        if (response.containsHeader("Content-Length")) {
            response.setContentLength(-1);
        }
        try {
            content.getData().writeValueTo(response.getWriter());
        } catch (IllegalStateException ise) {  // If getOutputStream() has already been called
            content.getData().writeValueTo(new PrintStream(response.getOutputStream()));
        }
        return true;
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
}
