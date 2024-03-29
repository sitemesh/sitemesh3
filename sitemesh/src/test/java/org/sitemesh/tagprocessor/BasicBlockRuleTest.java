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

import java.nio.CharBuffer;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Joe Walnes
 */
public class BasicBlockRuleTest extends TestCase {

    public void testTransfersDataFromStartToEndTagEvenWhenNested() throws IOException {
        String in = "aaa<tag type=outer>bbb<tag type=inner>ccc</tag>bbb</tag>aaa";
        final Map<String, String> out = new HashMap<String, String>();

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("tag", new BasicBlockRule<String>() {
            @Override
            protected String processStart(Tag tag) throws IOException {
                tagProcessorContext.pushBuffer();
                return tag.getAttributeValue("type", false);
            }

            @Override
            protected void processEnd(Tag tag, String data) throws IOException {
                out.put(data, tagProcessorContext.currentBufferContents().toString());
                tagProcessorContext.popBuffer();
            }
        });
        tagProcessor.process();

        assertEquals("bbbbbb", out.get("outer"));
        assertEquals("ccc", out.get("inner"));
    }

    public void testDoesNotProcessEndTagsThatDoNotHaveAStartTag() throws IOException {
        String in = "<x> <x></x> </x> skip next</x> <x><x><x></x></x></x> skip next</x> <x>";
        final AtomicInteger startCount = new AtomicInteger();
        final AtomicInteger endCount = new AtomicInteger();

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("x", new BasicBlockRule() {
            @Override
            protected String processStart(Tag tag) throws IOException {
                startCount.incrementAndGet();
                return null;
            }

            @Override
            protected void processEnd(Tag tag, Object data) throws IOException {
                endCount.incrementAndGet();
            }
        });
        tagProcessor.process();

        assertEquals(6, startCount.intValue());
        assertEquals(5, endCount.intValue());
    }
}
