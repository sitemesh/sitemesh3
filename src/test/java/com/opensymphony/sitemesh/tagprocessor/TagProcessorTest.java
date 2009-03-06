package com.opensymphony.sitemesh.tagprocessor;

import com.opensymphony.sitemesh.html.rules.TagReplaceRule;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 */
public class TagProcessorTest extends TestCase {

    public void testSupportsConventionalReaderAndWriter() throws IOException {
        TagProcessor processor = new TagProcessor(CharBuffer.wrap("<hello><b id=\"something\">world</b></hello>"));
        processor.addRule(new TagReplaceRule("b", "strong"));

        processor.process();
        assertEquals("<hello><strong id=\"something\">world</strong></hello>",
                processor.getDefaultBufferContents().toString());
    }

    public void testAllowsRulesToModifyAttributes() throws IOException {
        TagProcessor processor = new TagProcessor(CharBuffer.wrap("<hello><a href=\"modify-me\">world</a></hello>"));
        processor.addRule(new BasicRule("a") {
            @Override
            public void process(Tag tag) throws IOException {
                CustomTag customTag = new CustomTag(tag);
                String href = customTag.getAttributeValue("href", false);
                if (href != null) {
                    href = href.toUpperCase();
                    customTag.setAttributeValue("href", true, href);
                }
                customTag.writeTo(context.currentBuffer());
            }
        });

        processor.process();
        assertEquals("<hello><a href=\"MODIFY-ME\">world</a></hello>",
                processor.getDefaultBufferContents().toString());
    }

    public void testCanAddAttributesToCustomTag() throws IOException {
        TagProcessor processor = new TagProcessor(CharBuffer.wrap("<h1>Headline</h1>"));
        processor.addRule(new BasicRule() {
            @Override
            public boolean shouldProcess(String tag) {
                return tag.equalsIgnoreCase("h1");
            }

            @Override
            public void process(Tag tag) throws IOException {
                if (tag.getType() == Tag.Type.OPEN) {
                    CustomTag ctag = new CustomTag(tag);
                    ctag.addAttribute("class", "y");
                    assertEquals(1, ctag.getAttributeCount());
                    tag = ctag;
                }
                tag.writeTo(context.currentBuffer());
            }
        });
        processor.process();
        assertEquals("<h1 class=\"y\">Headline</h1>",
                processor.getDefaultBufferContents().toString());
    }
}
