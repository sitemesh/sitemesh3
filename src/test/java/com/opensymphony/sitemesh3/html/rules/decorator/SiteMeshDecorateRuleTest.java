package com.opensymphony.sitemesh3.html.rules.decorator;

import junit.framework.TestCase;
import com.opensymphony.sitemesh3.SiteMeshContextStub;
import com.opensymphony.sitemesh3.ContentProperty;
import com.opensymphony.sitemesh3.InMemoryContentProperty;
import com.opensymphony.sitemesh3.tagprocessor.TagProcessor;

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

        final AtomicReference<ContentProperty> capturedContentPropertyRef = new AtomicReference<ContentProperty>();

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new SiteMeshContextStub() {
            @Override
            public ContentProperty decorate(String decoratorName, ContentProperty contentProperty) throws IOException {
                capturedContentPropertyRef.set(contentProperty);
                return new InMemoryContentProperty();
            }
        }));
        tagProcessor.process();

        ContentProperty contentProperty = capturedContentPropertyRef.get();
        assertNotNull(contentProperty);
        assertEquals("<b>Some content</b>", contentProperty.getChild("body").getValue());
        assertEquals("<b>Some content</b>", contentProperty.getOriginal().getValue());
        assertEquals("foo", contentProperty.getChild("title").getValue());
        assertEquals("bar", contentProperty.getChild("cheese").getValue());
    }

    public void testAllowsContextToWriteToPage() throws IOException {
        String in = "BEFORE" +
                "<sitemesh:decorate decorator='x'><b>Some content</b></sitemesh:decorate>" +
                "AFTER";

        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:decorate", new SiteMeshDecorateRule(new SiteMeshContextStub() {
            @Override
            public ContentProperty decorate(String decoratorName, ContentProperty contentProperty) throws IOException {
                ContentProperty result = new InMemoryContentProperty();
                result.getChild("body").setValue("-DECORATED-");
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
            public ContentProperty decorate(String decoratorName, ContentProperty contentProperty) throws IOException {
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
            public ContentProperty decorate(String decoratorName, ContentProperty contentProperty) throws IOException {
                wasCalled.set(true);
                return null;
            }
        }));
        tagProcessor.process();

        assertFalse("applyDecorator() should NOT have been called", wasCalled.get());
        assertEquals("BEFORE<b>Some content</b>AFTER", tagProcessor.getDefaultBufferContents().toString());
    }

}
