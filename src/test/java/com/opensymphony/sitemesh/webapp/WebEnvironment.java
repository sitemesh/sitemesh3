package com.opensymphony.sitemesh.webapp;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpParser;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.log.Log;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

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
 * System.out.println(webEnvironment.getBody());
 * </pre>
 *
 * @author Joe Walnes
 */
public class WebEnvironment {

    private final LocalConnector connector;
    private String rawResponse;
    private int status;
    private String body;

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
        String request = "GET " + path + " HTTP/1.1\r\nHost: localhost\r\n\r\n";
        String response = connector.getResponses(request);
        rawResponse = unixLineEndings(response);

        new HttpParser(new ByteArrayBuffer(response), new HttpParser.EventHandler() {
            @Override
            public void content(Buffer buffer) throws IOException {
                body = buffer.toString();
            }

            @Override
            public void startRequest(Buffer method, Buffer url, Buffer version) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void startResponse(Buffer version, int status, Buffer reason) throws IOException {
                WebEnvironment.this.status = status;
            }
        }).parse();
    }

    /**
     * Returns the raw HTTP response from the last request. This includes HTTP status code, headers
     * and content.
     */
    public String getRawResponse() {
        return rawResponse;
    }

    public String getBody() {
        if (status != 200) {
            throw new IllegalStateException("Bad response status: " + status);
        }
        return body;
    }

    private String unixLineEndings(String string) {
        return string.replaceAll("\r\n", "\n").trim();
    }

    public int getStatus() {
        return status;
    }

    public static class Builder {
        private final Server server;
        private final Context context;
        private LocalConnector connector;

        public Builder() {
            Log.setLog(null);
            server = new Server();
            connector = new LocalConnector();
            context = new Context(Context.SESSIONS);
            server.setSendServerVersion(false);
            server.addConnector(connector);
            server.addHandler(context);
        }

        public Builder addServlet(String path, HttpServlet servlet) {
            context.addServlet(new ServletHolder(servlet), path);
            return this;
        }

        public Builder addServlet(String path, Class<? extends HttpServlet> servletClass, Map<String,String> params) {
            ServletHolder servletHolder = new ServletHolder(servletClass);
            servletHolder.setInitParameters(params);
            context.addServlet(servletHolder, path);
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

        public Builder serveResourcesFrom(String path) {
            context.setResourceBase(path);
            return this;
        }

        public Builder addFilter(String path, Filter filter) {
            context.addFilter(new FilterHolder(filter), path, Handler.DEFAULT);
            return this;
        }

        public Builder addFilter(String path, Class<? extends Filter> filterClass, Map<String,String> params) {
            FilterHolder filterHolder = new FilterHolder(filterClass);
            filterHolder.setInitParameters(params);
            context.addFilter(filterHolder, path, Handler.DEFAULT);
            return this;
        }

        public Builder addListener(ServletContextListener listener) {
            context.addEventListener(listener);
            return this;
        }

        public WebEnvironment create() throws Exception {
            server.start();
            return new WebEnvironment(connector);
        }

    }

}