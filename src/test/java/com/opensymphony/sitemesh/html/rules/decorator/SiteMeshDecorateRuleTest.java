package com.opensymphony.sitemesh.html.rules.decorator;

import junit.framework.TestCase;
import com.opensymphony.sitemesh.ContextStub;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.InMemoryContent;
import com.opensymphony.sitemesh.tagprocessor.TagProcessor;

import java.nio.CharBuffer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

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
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new ContextStub() {
            @Override
            public Content decorate(String decoratorName, Content content) throws IOException {
                capturedContentRef.set(content);
                return new InMemoryContent();
            }
        }));
        tagProcessor.process();

        Content content = capturedContentRef.get();
        assertNotNull(content);
        assertEquals("<b>Some content</b>", content.getProperty("body").value());
        assertEquals("<b>Some content</b>", content.getOriginal().value());
        assertEquals("foo", content.getProperty("title").value());
        assertEquals("bar", content.getProperty("cheese").value());
    }

    public void testAllowsContextToWriteToPage() throws IOException {
        String in = "BEFORE" +
                "<sitemesh:decorate decorator='x'><b>Some content</b></sitemesh:decorate>" +
                "AFTER";

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new ContextStub() {
            @Override
            public Content decorate(String decoratorName, Content content) throws IOException {
                Content result = new InMemoryContent();
                result.addProperty("body", "-DECORATED-");
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
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new ContextStub() {
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
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new ContextStub() {
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
