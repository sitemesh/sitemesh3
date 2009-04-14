package com.opensymphony.sitemesh3.html.rules;

import com.opensymphony.sitemesh3.ContentProcessor;
import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.ContentProperty;
import com.opensymphony.sitemesh3.html.HtmlContentProcessor;
import com.opensymphony.sitemesh3.tagprocessor.State;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Daniel Bodart
 */
public class DivExtractingRuleTest extends TestCase {

    private ContentProcessor contentProcessor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        contentProcessor = new HtmlContentProcessor() {
            @Override
            protected void setupRules(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
                super.setupRules(defaultState, contentProperty, siteMeshContext);
                defaultState.addRule("div", new DivExtractingRule(contentProperty.getChild("div")));
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
        ContentProperty out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals(body, out.getChild("body").getValue());
        assertEquals(outer, out.getChild("div").getChild("outer").getValue());
        assertEquals(inner, out.getChild("div").getChild("inner").getValue());
    }

    public void testDoesNotConsumeDivWhenExtracting() throws IOException {
        // setup
        String html = "<html><body><div id='target'>content</div></body></html>";

        // execute
        ContentProperty out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals("<div id='target'>content</div>", out.getChild("body").getValue());
    }

    public void testExtractsDivsWithIds() throws IOException {
        // setup
        String html = "<html><body><div id='target'>content</div></body></html>";

        // execute
        ContentProperty out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals("content", out.getChild("div").getChild("target").getValue());
    }

}
