package com.opensymphony.sitemesh.webapp;

import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.DecoratorApplier;
import com.opensymphony.sitemesh.ContentProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.CharArrayWriter;
import java.nio.CharBuffer;

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
    private final ContentProcessor<WebAppContext> contentProcessor;

    public WebAppContext(String contentType, HttpServletRequest request,
                         HttpServletResponse response, ServletContext servletContext,
                         DecoratorApplier<WebAppContext> decoratorApplier,
                         ContentProcessor<WebAppContext> contentProcessor) {
        this.contentType = contentType;
        this.request = request;
        this.response = response;
        this.servletContext = servletContext;
        this.decoratorApplier = decoratorApplier;
        this.contentProcessor = contentProcessor;
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
        return getRequestPath(request);
    }

    public static String getRequestPath(HttpServletRequest request) {
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
    public Content decorate(String decoratorName, Content content) throws IOException {
        if (decoratorName == null) {
            return null;
        }

        class CharBufferWriter extends CharArrayWriter {
            public CharBuffer toCharBuffer() {
                return CharBuffer.wrap(this.buf, 0, this.count);
            }
        }
        CharBufferWriter out = new CharBufferWriter();
        decoratorApplier.decorate(decoratorName, content, this, out);

        CharBuffer decorated = out.toCharBuffer();

        // TODO: Don't reprocess the content properties.
        return contentProcessor.build(decorated, this);
    }
}
