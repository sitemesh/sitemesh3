package com.opensymphony.sitemesh.webapp;

import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.DecoratorApplier;
import com.opensymphony.sitemesh.DecoratorSelector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.Writer;

/**
 * SiteMesh {@link Context} implementation specifically for webapps running in a Servlet
 * container. Makes {@link HttpServletRequest}, {@link HttpServletResponse} and
 * {@link ServletContext} available to web-app specific SiteMesh components.
 *
 * @author Joe Walnes
 * @author Mike Cannon-Brookes
 */
public class WebAppContext implements Context {

    private final String contentType;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ServletContext servletContext;
    private final DecoratorApplier<WebAppContext> decoratorApplier;
    private final DecoratorSelector<WebAppContext> decoratorSelector;

    public WebAppContext(String contentType, HttpServletRequest request,
                         HttpServletResponse response, ServletContext servletContext,
                         DecoratorApplier<WebAppContext> decoratorApplier,
                         DecoratorSelector<WebAppContext> decoratorSelector) {
        this.contentType = contentType;
        this.request = request;
        this.response = response;
        this.servletContext = servletContext;
        this.decoratorApplier = decoratorApplier;
        this.decoratorSelector = decoratorSelector;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public String getRequestPath() {
        String result = request.getServletPath();
        // getServletPath() returns null unless the mapping corresponds to a servlet
        if (result == null) {
            String requestURI = request.getRequestURI();
            if (request.getPathInfo() != null) {
                // strip the pathInfo from the requestURI
                return requestURI.substring(0, requestURI.indexOf(request.getPathInfo()));
            } else {
                return requestURI;
            }
        } else if ("".equals(result)) {
            // in servlet 2.4, if a request is mapped to '/*', getServletPath returns null (SIM-130)
            return request.getPathInfo();
        } else {
            return result;
        }
    }

    @Override
    public boolean applyDecorator(Content content, Writer out) throws IOException {
        return content != null
                && applyDecorator(decoratorSelector.selectDecoratorPath(content, this), content, out);
    }

    @Override
    public boolean applyDecorator(String decoratorName, Content content, Writer out) throws IOException {
        return decoratorName != null
                && decoratorApplier.decorate(decoratorName, content, this, out);
    }
}
