package org.sitemesh.tagprocessor;

import junit.framework.TestCase;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 */
public class TagProcessorTest extends TestCase {

    public void testAllowsRulesToModifyAttributes() throws IOException {
        TagProcessor processor = new TagProcessor(CharBuffer.wrap("<hello><a href=\"modify-me\">world</a></hello>"));
        processor.addRule("a", new BasicRule() {
            @Override
            public void process(Tag tag) throws IOException {
                CustomTag customTag = new CustomTag(tag);
                String href = customTag.getAttributeValue("href", false);
                if (href != null) {
                    href = href.toUpperCase();
                    customTag.setAttributeValue("href", true, href);
                }
                customTag.writeTo(tagProcessorContext.currentBuffer());
            }
        });

        processor.process();
        assertEquals("<hello><a href=\"MODIFY-ME\">world</a></hello>",
                processor.getDefaultBufferContents().toString());
    }

    public void testCanAddAttributesToCustomTag() throws IOException {
        TagProcessor processor = new TagProcessor(CharBuffer.wrap("<h1>Headline</h1>"));
        processor.addRule("h1", new BasicRule() {
            @Override
            public void process(Tag tag) throws IOException {
                if (tag.getType() == Tag.Type.OPEN) {
                    CustomTag ctag = new CustomTag(tag);
                    ctag.addAttribute("class", "y");
                    assertEquals(1, ctag.getAttributeCount());
                    tag = ctag;
                }
                tag.writeTo(tagProcessorContext.currentBuffer());
            }
        });
        processor.process();
        assertEquals("<h1 class=\"y\">Headline</h1>",
                processor.getDefaultBufferContents().toString());
    }
}
