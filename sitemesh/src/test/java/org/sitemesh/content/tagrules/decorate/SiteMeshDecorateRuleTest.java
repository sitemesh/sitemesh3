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

package org.sitemesh.content.tagrules.decorate;

import org.sitemesh.SiteMeshContextStub;
import org.sitemesh.content.Content;
import org.sitemesh.content.memory.InMemoryContent;
import org.sitemesh.tagprocessor.TagProcessor;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Joe Walnes
 */
public class SiteMeshDecorateRuleTest extends TestCase {

    public void testPassesContentWithBodyAndPropertiesToContext() throws IOException {
        String in = "BEFORE" +
                "<sitemesh:decorate decorator='/mydecorator' title='foo' cheese='bar'><b>Some content</b></sitemesh:decorate>" +
                "AFTER";

        final AtomicReference<Content> capturedContentRef = new AtomicReference<Content>();

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new SiteMeshContextStub() {
            @Override
            public Content decorate(String decoratorName, Content content) throws IOException {
                capturedContentRef.set(content);
                return new InMemoryContent();
            }
        }));
        tagProcessor.process();

        Content content = capturedContentRef.get();
        assertNotNull(content);
        assertEquals("<b>Some content</b>", content.getExtractedProperties().getChild("body").getValue());
        assertEquals("<b>Some content</b>", content.getData().getValue());
        assertEquals("foo", content.getExtractedProperties().getChild("title").getValue());
        assertEquals("bar", content.getExtractedProperties().getChild("cheese").getValue());
    }

    public void testAllowsContextToWriteToPage() throws IOException {
        String in = "BEFORE" +
                "<sitemesh:decorate decorator='x'><b>Some content</b></sitemesh:decorate>" +
                "AFTER";

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new SiteMeshContextStub() {
            @Override
            public Content decorate(String decoratorName, Content content) throws IOException {
                Content result = new InMemoryContent();
                result.getExtractedProperties().getChild("body").setValue("-DECORATED-");
                return result;
            }
        }));
        tagProcessor.process();

        assertEquals("BEFORE-DECORATED-AFTER", tagProcessor.getDefaultBufferContents().toString());
    }

    public void testWritesBodyIfDecoratorCannotBeApplied() throws IOException {
        String in = "BEFORE" +
                "<sitemesh:decorate decorator=x><b>Some content</b></sitemesh:decorate>" +
                "AFTER";

        final AtomicBoolean wasCalled = new AtomicBoolean(false);

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new SiteMeshContextStub() {
            @Override
            public Content decorate(String decoratorName, Content content) throws IOException {
                wasCalled.set(true);
                return null;
            }
        }));
        tagProcessor.process();

        assertTrue("applyDecorator() should have been called", wasCalled.get());
        assertEquals("BEFORE<b>Some content</b>AFTER", tagProcessor.getDefaultBufferContents().toString());
    }

    public void testSkipsTagWithoutDecoratorAttribute() throws IOException {
        String in = "BEFORE" +
                "<sitemesh:decorate><b>Some content</b></sitemesh:decorate>" +
                "AFTER";

        final AtomicBoolean wasCalled = new AtomicBoolean(false);

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new SiteMeshContextStub() {
            @Override
            public Content decorate(String decoratorName, Content content) throws IOException {
                wasCalled.set(true);
                return null;
            }
        }));
        tagProcessor.process();

        assertFalse("applyDecorator() should NOT have been called", wasCalled.get());
        assertEquals("BEFORE<b>Some content</b>AFTER", tagProcessor.getDefaultBufferContents().toString());
    }

}
