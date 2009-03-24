package com.opensymphony.sitemesh.decorator.dispatch;

import com.opensymphony.sitemesh.DecoratorApplier;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.webapp.WebAppContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.io.Writer;

/**
 * Dispatches to another path to render a decorator.
 *
 * <p>This path may anything that handles a standard request (e.g. Servlet,
 * JSP, MVC framework, etc).</p>
 *
 * <p>The end point can access the {@link Content} and {@link Context} by using
 * looking them up as {@link HttpServletRequest} attributes under the keys
 * {@link DispatchingDecoratorApplier#CONTENT_KEY} and
 * {@link DispatchingDecoratorApplier#CONTEXT_KEY} respectively.</p>
 *
 * <p>It is also possible to dispatch to other web-applications running in
 * the same server, using {@link ExternalDispatchingDecoratorApplier}.
 *
 * @author Joe Walnes
 */
public class DispatchingDecoratorApplier implements DecoratorApplier<WebAppContext> {

    /**
     * Key that the {@link Content} is stored under in the {@link HttpServletRequest}
     * attribute. It is "com.opensymphony.sitemesh.Content".
     */
    public static final String CONTENT_KEY = Content.class.getName();

    /**
     * Key that the {@link Context} is stored under in the {@link HttpServletRequest}
     * attribute. It is "com.opensymphony.sitemesh.Context".
     */
    public static final String CONTEXT_KEY = Context.class.getName();

    /**
     * See class JavaDoc.
     */
    @Override
    public boolean decorate(String decoratorPath, Content content, WebAppContext context, Writer out)
            throws IOException {
        HttpServletRequest request = context.getRequest();
        HttpServletResponse response = context.getResponse();

        if (response.getWriter() != out) {
            // TODO: This needs to be supported.
            throw new UnsupportedOperationException();
        }

        // It's possible that this is reentrant, so we need to take a copy
        // of additional request attributes so we can restore them afterwards.
        Object oldContent = request.getAttribute(CONTENT_KEY);
        Object oldContext = request.getAttribute(CONTEXT_KEY);

        request.setAttribute(CONTENT_KEY, content);
        request.setAttribute(CONTEXT_KEY, context);

        try {
            // Main dispatch.
            ServletContext destinationServletContext = getContextForWebApp(context);
            dispatch(request, response, destinationServletContext, decoratorPath);
        } catch (ServletException e) {
            throw new IOException("Could not dispatch to decorator: ", e);
        } finally {
            // Restore previous state.
            request.setAttribute(CONTENT_KEY, oldContent);
            request.setAttribute(CONTEXT_KEY, oldContext);
        }

        return true;
    }

    /**
     * Find ServletContext to dispatch requests to.
     */
    protected ServletContext getContextForWebApp(WebAppContext webAppContext) {
        return webAppContext.getServletContext();
    }

    /**
     * Dispatch to the actual path.
     */
    protected void dispatch(HttpServletRequest request, HttpServletResponse response,
                            ServletContext destinationServletContext, String path)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = destinationServletContext.getRequestDispatcher(path);
        dispatcher.include(request, response);
    }

}
