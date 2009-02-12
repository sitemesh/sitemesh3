package com.opensymphony.sitemesh.webapp;

import com.opensymphony.sitemesh.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * SiteMesh {@link Context} implementation specifically for webapps running in a Servlet
 * container. Makes {@link HttpServletRequest}, {@link HttpServletResponse} and
 * {@link ServletContext} available to web-app specific SiteMesh components.
 *
 * @author Joe Walnes
 */
public class WebAppContext implements Context {

    private final String contentType;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ServletContext servletContext;

    public WebAppContext(String contentType, HttpServletRequest request,
                         HttpServletResponse response, ServletContext servletContext) {
        this.contentType = contentType;
        this.request = request;
        this.response = response;
        this.servletContext = servletContext;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return getResponse().getWriter();
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

}
