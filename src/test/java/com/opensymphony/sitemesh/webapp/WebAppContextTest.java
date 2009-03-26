package com.opensymphony.sitemesh.webapp;

import junit.framework.TestCase;
import com.opensymphony.sitemesh.webapp.contentfilter.BasicSelector;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.html.rules.DecorateRule;
import com.opensymphony.sitemesh.decorator.map.PathBasedDecoratorSelector;
import com.opensymphony.sitemesh.decorator.simple.SimpleDecoratorApplier;
import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.tagprocessor.State;

/**
 * @author Joe Walnes
 */
public class WebAppContextTest extends TestCase {

    public void testSupportsDecoratingInlineContent() throws Exception {

        ContentProcessor<WebAppContext> processor = new HtmlContentProcessor<WebAppContext>() {
            @Override
            protected void setupRules(State defaultState, Content content, WebAppContext context) {
                super.setupRules(defaultState, content, context);
                defaultState.addRule(new DecorateRule(context));
            }
        };

        WebEnvironment web = new WebEnvironment.Builder()
                .addFilter("/*", new BaseSiteMeshFilter(
                        new BasicSelector("text/html"),
                        processor,
                        new PathBasedDecoratorSelector()
                                .put("/*", "page"),
                        new SimpleDecoratorApplier()
                                .put("page", "PAGE\n{{body}}\n/PAGE")
                                .put("inline", "INLINE Title:{{title}} " +
                                        "Body:{{body}} /INLINE")
                ))
                .addStaticContent("/hello.html", "text/html", "" +
                        "<body>\n" +
                        "CONTENT\n" +
                        "<decorate decorator='inline' title='block A'><b>A</b></decorate>\n" +
                        "<decorate decorator='inline' title='block B'><i>B</i></decorate>\n" +
                        "</body>\n")
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

        ContentProcessor<WebAppContext> processor = new HtmlContentProcessor<WebAppContext>() {
            @Override
            protected void setupRules(State defaultState, Content content, WebAppContext context) {
                super.setupRules(defaultState, content, context);
                defaultState.addRule(new DecorateRule(context));
            }
        };

        WebEnvironment web = new WebEnvironment.Builder()
                .addFilter("/*", new BaseSiteMeshFilter(
                        new BasicSelector("text/html"),
                        processor,
                        new PathBasedDecoratorSelector()
                                .put("/*", "page"),
                        new SimpleDecoratorApplier()
                                .put("page", "PAGE\n<decorate decorator='inner'>{{body}}</decorate>\n/PAGE")
                                .put("inline", "INLINE Title:{{title}} " +
                                        "Body:<decorate decorator='inner'>{{body}}</decorate> /INLINE")
                                .put("inner", "INNER{{body}}/INNER")
                ))
                .addStaticContent("/hello.html", "text/html", "" +
                        "<body>\n" +
                        "CONTENT\n" +
                        "<decorate decorator='inline' title='block A'><b>A</b></decorate>\n" +
                        "<decorate decorator='inline' title='block B'><i>B</i></decorate>\n" +
                        "</body>\n")
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
