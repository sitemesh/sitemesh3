package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.html.HtmlContentProcessor;
import com.opensymphony.sitemesh.tagprocessor.State;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Daniel Bodart
 */
public class DivExtractingRuleTest extends TestCase {

    private ContentProcessor<?> contentProcessor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        contentProcessor = new HtmlContentProcessor<Context>() {
            @Override
            protected void setupRules(State defaultState, Content content, Context context) {
                super.setupRules(defaultState, content, context);
                defaultState.addRule("div", new DivExtractingRule(content));
            }
        };
    }

    public void testHandlesNestedDivs() throws IOException {
        // setup
        String inner = "<div>inner</div>";
        String outer = "content<div id='inner'>" + inner + "</div>";
        String body = "<div><div id='outer'>" + outer + "</div></div>";
        String html = "<html><body>" + body + "</body></html>";

        // execute
        Content out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals(body, out.getProperty("body").value());
        assertEquals(outer, out.getProperty("div.outer").value());
        assertEquals(inner, out.getProperty("div.inner").value());
    }

    public void testDoesNotConsumeDivWhenExtracting() throws IOException {
        // setup
        String html = "<html><body><div id='target'>content</div></body></html>";

        // execute
        Content out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals("<div id='target'>content</div>", out.getProperty("body").value());
    }

    public void testExtractsDivsWithIds() throws IOException {
        // setup
        String html = "<html><body><div id='target'>content</div></body></html>";

        // execute
        Content out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals("content", out.getProperty("div.target").value());
    }

}
