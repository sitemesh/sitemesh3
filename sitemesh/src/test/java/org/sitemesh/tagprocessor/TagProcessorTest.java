/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
