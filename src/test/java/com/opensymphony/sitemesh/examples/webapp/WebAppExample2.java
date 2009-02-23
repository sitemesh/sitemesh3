package com.opensymphony.sitemesh.examples.webapp;

import com.opensymphony.sitemesh.decorator.dispatch.DispatchingDecoratorApplier;
import com.opensymphony.sitemesh.decorator.map.PathBasedDecoratorSelector;
import com.opensymphony.sitemesh.html.HtmlContent;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.webapp.BaseSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Simple example of running SiteMesh in a web-app as a Servlet Filter.
 * This demonstrates dispatching to another path to handle the decorator
 * (as done in SiteMesh 2).
 *
 * @author Joe Walnes
 */
public class WebAppExample2 {

    /**
     * Sample Servlet to use for decoration. In a real app this may be handled
     * by a JSP, MVC Framework, custom templating Servlet, etc.
     */
    public static class MyDecoratorServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            // Fetch the Content to be decorated. Because HtmlContentProcessor is used
            // in the SiteMesh filter, it can be casted directly to HtmlContent, giving
            // us some convenient methods.
            HtmlContent content = (HtmlContent) request.getAttribute(DispatchingDecoratorApplier.CONTENT_KEY);

            response.getWriter().printf("Title: %s\nBody:\n%s",
                    content.getTitle().value(),
                    content.getBody().value());
        }
    }

    public static void main(String[] args) throws Exception {

        // Configure SiteMesh filter.
        Filter siteMeshFilter = new BaseSiteMeshFilter(
                // Applies to text/html MIME times.
                new BasicSelector("text/html"),
                // Process the data as HTML, exposing relevant properties.
                new HtmlContentProcessor<WebAppContext>(),
                new PathBasedDecoratorSelector().put("/*", "/mydecorator"),
                // Dispatch to another
                new DispatchingDecoratorApplier());

        // Configure an in-process Servlet container.
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                // SiteMesh filters everything.
                .addFilter("/*", siteMeshFilter)
                // Some static content pages.
                .addStaticContent("/hello", "text/html",
                        "<html><head><title>Test1</title></head><body>Hello <b>World</b>!</body></html>")
                .addStaticContent("/bye", "text/html",
                        "<html><head><title>Test2</title></head><body><b>Byeee</b></body></html>")
                .addServlet("/my-decorator", new MyDecoratorServlet())
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