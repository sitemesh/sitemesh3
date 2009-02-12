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

/**
 * Dispatches to another path in the Servlet engine to render a decorator.
 *
 * <p>This path may anything that handles a standard request (e.g. Servlet,
 * JSP, MVC framework, etc).</p>
 *
 * <p>The end point can access the {@link Content} and {@link Context} by using
 * looking them up as {@link HttpServletRequest} attributes under the keys
 * {@link DispatchingDecoratorApplier#CONTENT_KEY} and
 * {@link DispatchingDecoratorApplier#CONTEXT_KEY} respectively.</p>
 *
 * <p>Optionally, this may also dispatch to servlets in another web application
 * context in the same container. This allows multiple web apps to share the
 * same decorator. In order to do this, it's important that the SiteMesh library
 * is loaded in a shared class loader so the objects can be passed across.</p>
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

    private final String path;
    private final String webApp;

    /**
     * Dispatch to a path in the same web-app.
     *
     * @param path e.g. "/my-servet", "/some/action.do", "/foo.jsp"...
     */
    public DispatchingDecoratorApplier(String path) {
        this(path, null);
    }

    /**
     * Dispatch to a path in a different web-app (though it has to be running in
     * the same container).
     *
     * @param path e.g. "/my-servet", "/some/action.do", "/foo.jsp"...
     * @param webApp e.g. "/anotherapp"
     */
    public DispatchingDecoratorApplier(String path, String webApp) {
        this.path = path;
        this.webApp = webApp;
    }

    /**
     * See class JavaDoc.
     */
    @Override
    public boolean decorate(Content content, WebAppContext context) throws IOException {
        HttpServletRequest request = context.getRequest();
        HttpServletResponse response = context.getResponse();
        ServletContext servletContext = context.getServletContext();

        // It's possible that this is reentrant, so we need to take a copy
        // of additional request attributes so we can restore them afterwards.
        Object oldContent = request.getAttribute(CONTENT_KEY);
        Object oldContext = request.getAttribute(CONTEXT_KEY);

        request.setAttribute(CONTENT_KEY, content);
        request.setAttribute(CONTEXT_KEY, context);

        try {
            // Main dispatch.
            ServletContext destinationServletContext = getContextForWebApp(servletContext, webApp);
            dispatch(request, response, destinationServletContext, path);
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
     * Look up the appropriate ServletContext. If webApp is null, the standard ServletContext
     * is returned. Otherwise it will attempt to look up the context of another path.
     */
    protected ServletContext getContextForWebApp(ServletContext servletContext, String webApp) {
        if (webApp == null) {
            return servletContext;
        } else {
            ServletContext result = servletContext.getContext(webApp);
            if (result == null) {
                // in a security conscious environment, the servlet container
                // may return null for a given URL
                throw new SecurityException("Cannot obtain ServletContext for web-app : " + webApp);
            }
            return result;
        }
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
