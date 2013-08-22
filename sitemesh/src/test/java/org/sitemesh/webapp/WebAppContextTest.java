package org.sitemesh.webapp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.sitemesh.config.PathBasedDecoratorSelector;
import org.sitemesh.config.PathMapper;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.webapp.contentfilter.BasicSelector;

/**
 * @author Joe Walnes
 */
public class WebAppContextTest extends TestCase {

    public void testDispatchesToServletToApplyDecorator() throws Exception {

        HttpServlet decoratorServlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                Content content = (Content) request.getAttribute(WebAppContext.CONTENT_KEY);
                PrintWriter out = response.getWriter();
                out.println("Title = " + content.getExtractedProperties().getChild("title").getValue());
            }
        };

        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilter(
                        new BasicSelector(new PathMapper<Boolean>(), "text/html"),
                        new TagBasedContentProcessor(new CoreHtmlTagRuleBundle()),
                        new PathBasedDecoratorSelector().put("/*", "/mydecorator"),
                        false
                ))
                .addStaticContent("/mycontent", "text/html", "<title>Some title</title>")
                .addServlet("/mydecorator", decoratorServlet)
                .create();

        webEnvironment.doGet("/mycontent");
        assertEquals("Title = Some title", webEnvironment.getBody().trim());
    }

    public void testSupportsDecoratingInlineContent() throws Exception {

        ContentProcessor processor = new TagBasedContentProcessor(
                new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());

        WebEnvironment web = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilter(
                        new BasicSelector(new PathMapper<Boolean>(), "text/html"),
                        processor,
                        new PathBasedDecoratorSelector()
                                .put("/*", "/decorators/page.html"),
                        false
                ))
                .addStaticContent("/hello.html", "text/html", "" +
                        "<body>\n" +
                        "CONTENT\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block A'><b>A</b></sitemesh:decorate>\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block B'><i>B</i></sitemesh:decorate>\n" +
                        "</body>\n")
                .addStaticContent("/decorators/page.html", "text/html",
                        "PAGE\n<sitemesh:write property='body'/>\n/PAGE")
                .addStaticContent("/decorators/inline.html", "text/html", "" +
                        "INLINE Title:<sitemesh:write property='title'/> " +
                        "Body:<sitemesh:write property='body'/> /INLINE")
                .create();

        web.doGet("/hello.html");

        String expected = "" +
                "PAGE\n" +
                "\n" +
                "CONTENT\n" +
                "INLINE Title:block A Body:<b>A</b> /INLINE\n" +
                "INLINE Title:block B Body:<i>B</i> /INLINE\n" +
                "\n" +
                "/PAGE";

        assertEquals(expected, web.getBody());
    }

    public void testSupportsDecoratingInlineContentInDecorators() throws Exception {

        ContentProcessor processor = new TagBasedContentProcessor(
                new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());

        WebEnvironment web = new WebEnvironment.Builder()
                .addFilter("/*", new SiteMeshFilter(
                        new BasicSelector(new PathMapper<Boolean>(), "text/html"),
                        processor,
                        new PathBasedDecoratorSelector()
                                .put("/*", "/decorators/page.html"),
                        false
                ))
                .addStaticContent("/hello.html", "text/html", "" +
                        "<body>\n" +
                        "CONTENT\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block A'><b>A</b></sitemesh:decorate>\n" +
                        "<sitemesh:decorate decorator='/decorators/inline.html' title='block B'><i>B</i></sitemesh:decorate>\n" +
                        "</body>\n")
                .addStaticContent("/decorators/page.html", "text/html", "" +
                        "PAGE\n<sitemesh:decorate decorator='/decorators/inner.html'>" +
                        "<sitemesh:write property='body'/></sitemesh:decorate>\n/PAGE")
                .addStaticContent("/decorators/inline.html", "text/html", "" +
                        "INLINE Title:<sitemesh:write property='title'/> " +
                        "Body:<sitemesh:decorate decorator='/decorators/inner.html'>" +
                        "<sitemesh:write property='body'/></sitemesh:decorate> /INLINE")
                .addStaticContent("/decorators/inner.html", "text/html", "" +
                        "INNER<sitemesh:write property='body'/>/INNER")
                .create();

        web.doGet("/hello.html");

        String expected = "" +
                "PAGE\n" +
                "INNER\n" +
                "CONTENT\n" +
                "INLINE Title:block A Body:INNER<b>A</b>/INNER /INLINE\n" +
                "INLINE Title:block B Body:INNER<i>B</i>/INNER /INLINE\n" +
                "/INNER\n" +
                "/PAGE";

        assertEquals(expected, web.getBody());
    }

}
