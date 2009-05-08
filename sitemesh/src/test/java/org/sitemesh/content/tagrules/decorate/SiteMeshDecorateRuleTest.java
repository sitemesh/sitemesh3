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
