package com.opensymphony.sitemesh.examples.webapp;

import com.opensymphony.sitemesh.webapp.BaseSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.decorator.simple.SimpleDecoratorApplier;
import com.opensymphony.sitemesh.decorator.map.PathBasedDecoratorSelector;

import javax.servlet.Filter;

/**
 * Simple example of running SiteMesh in a web-app as a Servlet Filter.
 *
 * @author Joe Walnes
 */
public class WebAppExample1 {

    /**
     * Really simple HTML decorator. Uses syntax of {@link SimpleDecoratorApplier}.
     * For more functionality you can something like JSP, Velocity, FreeMarker, etc.
     */
    private static final String SIMPLE_DECORATOR = "" +
            "<html>\n" +
            "  <head>\n" +
            "    <title>{{title}}</title>\n" +
            "    <style type='text/css'>body { font-family: verdana; }</style>\n" +
            "    {{head}}\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <h1>{{title}}</h1>\n" +
            "    <div id='body'>\n" +
            "      {{body}}\n" +
            "    </div>\n" +
            "  </body>\n" +
            "</html>\n";

    public static void main(String[] args) throws Exception {

        // Configure SiteMesh filter.
        Filter siteMeshFilter = new BaseSiteMeshFilter(
                // Applies to text/html MIME times.
                new BasicSelector("text/html"),
                // Process the data as HTML, exposing relevant properties.
                new HtmlContentProcessor<WebAppContext>(),
                new PathBasedDecoratorSelector()
                    .put("/*", "mydecorator"),
                // Apply the simple decorator (defined above).
                new SimpleDecoratorApplier()
                    .put("mydecorator", SIMPLE_DECORATOR));

        // Configure an in-process Servlet container.
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                // SiteMesh filters everything.
                .addFilter("/*", siteMeshFilter)
                // Some static content pages.
                .addStaticContent("/hello", "text/html",
                        "<html><head><title>Test1</title></head><body>Hello <b>World</b>!</body></html>")
                .addStaticContent("/bye", "text/html",
                        "<html><head><title>Test2</title></head><body><b>Byeee</b></body></html>")
                .create();

        // Now fetch the the static content from the server. It will be decorated by SiteMesh.

        webEnvironment.doGet("/hello");
        System.out.println("---- GET /hello ----");
        System.out.println(webEnvironment.getRawResponse());

        webEnvironment.doGet("/bye");
        System.out.println("---- GET /bye ----");
        System.out.println(webEnvironment.getRawResponse());

    }
}
