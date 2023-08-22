package org.sitemesh.webapp;

import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.PathResource;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;
import java.net.URISyntaxException;

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

    private final ServerConnector connector;
    private String rawResponse;
    private int status;
    private String body;
    private Map<String,String> headers;

    /**
     * Use {@link WebEnvironment.Builder} to create a WebEnvironment.
     */
    private WebEnvironment(ServerConnector connector) {
        this.connector = connector;
    }

    /**
     * Perform an HTTP request for a path. The result can be accessed using
     * {@link #getRawResponse()}.
     *
     * @param path e.g. "/some/servlet?foo=x"
     */
    public void doGet(String path, String... headerPairs) throws Exception {
        if (connector.isOpen()) {
            connector.close();
        }
        connector.open();
        StringBuilder request = new StringBuilder("GET ")
                .append(path)
                .append(" HTTP/1.1\r\n");

        addHeader(request, "Host", "localhost");
        if (headerPairs.length % 2 != 0) {
            throw new IllegalArgumentException("headerPairs should be an even number of values");
        }
        for (int i = 0; i < headerPairs.length; i += 2) {
            addHeader(request, headerPairs[i], headerPairs[i + 1]);
        }

        request.append("\r\n");

        LocalConnector localConnector = new LocalConnector(connector.getServer());
        localConnector.start();
        String response = localConnector.getResponse(request.toString());
        rawResponse = unixLineEndings(response);

        HttpTester.Response resp = HttpTester.parseResponse(response);
        body = resp.getContent();
        headers = new HashMap<String, String>();
        for (String header : resp.getFieldNamesCollection()) {
            headers.put(header, resp.get(header));
        }
        status = resp.getStatus();
    }

    private void addHeader(StringBuilder out, String name, String value) {
        out.append(name).append(": ").append(value).append("\r\n"); // TODO: escape values.
    }

    /**
     * Returns the raw HTTP response from the last request. This includes HTTP status code, headers
     * and content.
     */
    public String getRawResponse() {
        return rawResponse
                .replaceFirst("\\W+Date: .+","")
                .replaceFirst("\\W+Server: Jetty\\(\\d+\\.\\d+\\.\\d+\\.\\w+\\)","");
    }

    public String getBody() {
        if (status != 200) {
            throw new IllegalStateException("Bad response status: " + status
                    + "\n----- Raw HTTP response -----\n" + getRawResponse());
        }
        return body;
    }

    private String unixLineEndings(String string) {
        return string.replaceAll("\r\n", "\n").trim();
    }

    public int getStatus() {
        return status;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public static class Builder {
        private final Server server;
        private final ServletContextHandler context;
        private ServerConnector connector;
        private EnumSet DEFAULT = EnumSet.of(DispatcherType.REQUEST);

        public Builder() throws IOException, URISyntaxException {
            server = new Server();
            connector = new ServerConnector(server);
            context = new ServletContextHandler();
            server.addConnector(connector);
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

        public Builder addStatusCodeFail(String path, final int statusCode, final String contentType, final String content) {
            addServlet(path, new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    response.setStatus(statusCode);
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
            context.addFilter(new FilterHolder(filter), path, DEFAULT);
            return this;
        }

        public Builder addFilter(String path, Class<? extends Filter> filterClass, Map<String,String> params) {
            FilterHolder filterHolder = new FilterHolder(filterClass);
            filterHolder.setInitParameters(params);
            context.addFilter(filterHolder, path, DEFAULT);
            return this;
        }

        public Builder addListener(ServletContextListener listener) {
            context.addEventListener(listener);
            return this;
        }

        public Builder setRootDir(File dir) throws IOException, URISyntaxException {
            context.setBaseResource(new PathResource(dir.toURI().toURL()));
            return this;
        }

        public WebEnvironment create() throws Exception {
            server.setHandler(new HandlerList(context));
            server.start();
            return new WebEnvironment(connector);
        }

    }

}