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

package org.sitemesh.content.tagrules.html;

import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
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
        Content out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals(body, out.getExtractedProperties().getChild("body").getValue());
        assertEquals(outer, out.getExtractedProperties().getChild("div").getChild("outer").getValue());
        assertEquals(inner, out.getExtractedProperties().getChild("div").getChild("inner").getValue());
    }

    public void testDoesNotConsumeDivWhenExtracting() throws IOException {
        // setup
        String html = "<html><body><div id='target'>content</div></body></html>";

        // execute
        Content out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals("<div id='target'>content</div>", out.getExtractedProperties().getChild("body").getValue());
    }

    public void testExtractsDivsWithIds() throws IOException {
        // setup
        String html = "<html><body><div id='target'>content</div></body></html>";

        // execute
        Content out = contentProcessor.build(CharBuffer.wrap(html), null);

        // verify
        assertEquals("content", out.getExtractedProperties().getChild("div").getChild("target").getValue());
    }

}
