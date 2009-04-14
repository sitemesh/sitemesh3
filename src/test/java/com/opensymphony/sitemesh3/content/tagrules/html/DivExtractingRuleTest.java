package com.opensymphony.sitemesh3.content.tagrules.html;

import com.opensymphony.sitemesh3.content.ContentProcessor;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.content.tagrules.TagBasedContentProcessor;
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
        contentProcessor = new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new DivExtractingTagRuleBundle());
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
