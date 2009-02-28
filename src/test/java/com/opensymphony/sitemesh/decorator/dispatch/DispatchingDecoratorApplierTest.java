package com.opensymphony.sitemesh.decorator.dispatch;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.decorator.map.PathBasedDecoratorSelector;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.webapp.BaseSiteMeshFilter;
import com.opensymphony.sitemesh.webapp.WebAppContext;
import com.opensymphony.sitemesh.webapp.WebEnvironment;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;
import junit.framework.TestCase;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This is a coarse grained integration test that deploys SiteMesh and a real Servlet into
 * a container and tests them end-to-end.
 *
 * @see WebEnvironment
 *
 * @author Joe Walnes
 */
public class DispatchingDecoratorApplierTest extends TestCase {

    public void testDispatchesToServletToApplyDecorator() throws Exception {

        HttpServlet decoratorServlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                Content content = (Content) request.getAttribute(DispatchingDecoratorApplier.CONTENT_KEY);
                PrintWriter out = response.getWriter();
                out.println("Title = " + content.getProperty("title"));
            }
        };

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new BaseSiteMeshFilter(
                        new BasicSelector("text/html"),
                        new HtmlContentProcessor<WebAppContext>(),
                        new PathBasedDecoratorSelector().put("/*", "/mydecorator"),
                        new DispatchingDecoratorApplier()))
                .addStaticContent("/mycontent", "text/html", "<title>Some title</title>")
                .addServlet("/mydecorator", decoratorServlet)
                .create();

        webEnvironment.doGet("/mycontent");
        assertEquals(
                "HTTP/1.1 200 OK\n" +
                        "Content-Type: text/html\n" +
                        "Content-Length: 19\n" +
                        "\n" +
                        "Title = Some title",
                webEnvironment.getRawResponse());
    }

}
