package com.opensymphony.sitemesh.webapp.contentfilter;

import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.log.Log;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.Filter;

import java.io.IOException;

/**
 *
 */
public class WebEnvironment {

    private final LocalConnector connector;
    private String rawResponse;

    // Use createWebEnvironment() instead.
    private WebEnvironment(LocalConnector connector) {
        this.connector = connector;
    }

    public static Builder createWebEnvironment() {
        return new Builder();
    }

    public void doGet(String path) throws Exception {
        connector.reopen();
        rawResponse = cleanUp(connector.getResponses("GET " + path + " HTTP/1.1\r\nHost: localhost\r\n\r\n"));
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public static class Builder {
        private final Server server;
        private final Context context;
        private LocalConnector connector;

        public Builder() {
            Log.setLog(null);
            server = new Server();
            connector = new LocalConnector();
            context = new Context();
            server.setSendServerVersion(false);
            server.addConnector(connector);
            server.addHandler(context);
        }

        public Builder addServlet(String path, HttpServlet servlet) {
            context.addServlet(new ServletHolder(servlet), path);
            return this;
        }

        public Builder addStaticContent(String path, final String contentType, final String content) {
            addServlet(path, new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    response.setContentType(contentType);
                    response.getOutputStream().print(content);
                }
            });
            return this;
        }

        public Builder addFilter(String path, Filter filter) {
            context.addFilter(new FilterHolder(filter), path, Handler.DEFAULT);
            return this;
        }

        public WebEnvironment start() throws Exception {
            server.start();
            return new WebEnvironment(connector);
        }

    }

    public static String cleanUp(String string) {
        return string.replaceAll("\r\n", "\n").trim();
    }
}