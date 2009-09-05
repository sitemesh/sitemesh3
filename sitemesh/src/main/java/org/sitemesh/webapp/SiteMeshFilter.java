package org.sitemesh.webapp;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.contentfilter.ContentBufferingFilter;
import org.sitemesh.webapp.contentfilter.Selector;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.CharBuffer;

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

    /**
     * @param selector Provides the rules for whether SiteMesh should be
     *                 used for a specific request. For a basic implementation, use
     *                 {@link org.sitemesh.webapp.contentfilter.BasicSelector}.
     */
    public SiteMeshFilter(Selector selector,
                          ContentProcessor contentProcessor,
                          DecoratorSelector<WebAppContext> decoratorSelector) {
        super(selector);
        if (contentProcessor == null) {
            throw new IllegalArgumentException("contentProcessor cannot be null");
        }
        if (decoratorSelector == null) {
            throw new IllegalArgumentException("decoratorSelector cannot be null");
        }
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
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

        content.getData().writeValueTo(response.getWriter());
        return true;
    }

    /**
     * Create a context for the current request. This method can be overriden to allow for other
     * types of context.
     */
    protected WebAppContext createContext(String contentType, HttpServletRequest request,
                                          HttpServletResponse response, ResponseMetaData metaData) {
        return new WebAppContext(contentType, request, response,
                getFilterConfig().getServletContext(), contentProcessor, metaData);
    }

}
