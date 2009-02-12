package com.opensymphony.sitemesh.webapp;

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
 * Sets up a complete web-environment in-process. This includes a Servlet container, Filters, static
 * content and an HTTP API. No actual TCP sockets are opened, or files accessed to do this.
 *
 * <p>Useful for automated tests and experimentation.</p>
 * <p>Under the hood, it uses Jetty with and a Jetty LocalConnector.</p>
 * <p>To set up, use {@link WebEnvironment.Builder}.
 *
 * <h3>Example</h3>
 * <pre>
 * WebEnvironment webEnvironment = new WebEnvironment.Builder()
 * &nbsp;    .addServlet("/sheep", new SheepServlet())
 * &nbsp;    .addServlet("*.cheese", new CheeseServlet())
 * &nbsp;    .addFilter("/*", new MegaFilter())
 * &nbsp;    .addStaticContent("/help", "text/html", "Here is some help text.")
 * &nbsp;    .create();
 *
 * webEnvironment.doGet("/help");
 * System.out.println(webEnvironment.getRawResponse());
 * </pre>
 *
 * @author Joe Walnes
 */
public class WebEnvironment {

    private final LocalConnector connector;
    private String rawResponse;

    /**
     * Use {@link WebEnvironment.Builder} to create a WebEnvironment.
     */
    private WebEnvironment(LocalConnector connector) {
        this.connector = connector;
    }

    /**
     * Perform an HTTP request for a path. The result can be accessed using
     * {@link #getRawResponse()}.
     *
     * @param path e.g. "/some/servlet?foo=x"
     */
    public void doGet(String path) throws Exception {
        connector.reopen();
        rawResponse = unixLineEndings(connector.getResponses("GET " + path + " HTTP/1.1\r\nHost: localhost\r\n\r\n"));
    }

    /**
     * Returns the raw HTTP response from the last request. This includes HTTP status code, headers
     * and content.
     */
    public String getRawResponse() {
        return rawResponse;
    }

    private String unixLineEndings(String string) {
        return string.replaceAll("\r\n", "\n").trim();
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

        public WebEnvironment create() throws Exception {
            server.start();
            return new WebEnvironment(connector);
        }

    }

}