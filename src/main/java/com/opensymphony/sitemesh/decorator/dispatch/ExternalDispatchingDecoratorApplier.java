package com.opensymphony.sitemesh.decorator.dispatch;

import com.opensymphony.sitemesh.webapp.WebAppContext;

import javax.servlet.ServletContext;

/**
 * Dispatches to another path in another web application (in the same server) to render a decorator.
 *
 * Extension to {@link DispatchingDecoratorApplier} that dispatches to servlets in another web application
 * context in the same container. This allows multiple web apps to share the
 * same decorator. In order to do this, it's important that the SiteMesh library
 * is loaded in a shared class loader so the objects can be passed across.</p>
 *
 * @author Joe Walnes
 */
public class ExternalDispatchingDecoratorApplier extends DispatchingDecoratorApplier {

    private final String webApp;

    public ExternalDispatchingDecoratorApplier(String webApp) {
        this.webApp = webApp;
    }

    /**
     * Look up the appropriate ServletContext. If webApp is null, the standard ServletContext
     * is returned. Otherwise it will attempt to look up the context of another path.
     */
    @Override
    protected ServletContext getContextForWebApp(WebAppContext webAppContext) {
        ServletContext servletContext = webAppContext.getServletContext();
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

}
