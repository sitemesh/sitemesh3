package com.opensymphony.sitemesh.examples.webapp.extras;

import com.opensymphony.sitemesh.decorator.velocity.VelocityDecoratorApplier;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.webapp.BaseSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;

import javax.servlet.Filter;

/**
 * Demonstrates using a <a href="http://velocity.apache.org">Apache Velocity</a> template
 * as a decorator.
 *
 * @author Joe Walnes
 */
public class VelocityExample {

    /**
     * Really simple Velocity based decorator.
     */
    private static final String SIMPLE_DECORATOR = "" +
            "<html>\n" +
            "  <head>\n" +
            "    <title>$content.title</title>\n" +
            "    <style type='text/css'>body { font-family: verdana; }</style>\n" +
            "    $content.head\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <h1>$content.title</h1>\n" +
            "    <div id='body'>\n" +
            "      $content.body\n" +
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
                // Apply the simple decorator (defined above).
                new VelocityDecoratorApplier(SIMPLE_DECORATOR));

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
