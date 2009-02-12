package com.opensymphony.sitemesh.webapp.decorator;

import com.opensymphony.sitemesh.ContentStub;
import com.opensymphony.sitemesh.ContextStub;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * @author Joe Walnes
 */
public class SimpleDecoratorApplierTest extends TestCase {

    public void testSubstitutesTokensWithContentProperties() throws IOException {
        SimpleDecoratorApplier decoratorApplier = new SimpleDecoratorApplier(
                "Hello {{name.first}} {{name.last}}!\n{{message}}");

        ContentStub content = new ContentStub();
        content.addProperty("name.first", "You");
        content.addProperty("name.last", "There");
        content.addProperty("message", "How are you?");

        assertDecoratedContent("Hello You There!\nHow are you?", decoratorApplier, content);
    }

    public void testSkipsMissingProperties() throws IOException {
        SimpleDecoratorApplier decoratorApplier = new SimpleDecoratorApplier(
                "Hello {{unknown}} {{this.too}}!");

        ContentStub content = new ContentStub();
        assertDecoratedContent("Hello  !", decoratorApplier, content);
    }

    public void testHandlesEdgeCasingParsing() throws IOException {
        ContentStub content = new ContentStub();
        content.addProperty("t", "T");
        assertDecoratedContent("", new SimpleDecoratorApplier(""), content);
        assertDecoratedContent("\n", new SimpleDecoratorApplier("\n"), content);
        assertDecoratedContent("  ", new SimpleDecoratorApplier("  "), content);
        assertDecoratedContent("T", new SimpleDecoratorApplier("{{t}}"), content);
        assertDecoratedContent(" T", new SimpleDecoratorApplier(" {{t}}"), content);
        assertDecoratedContent("T ", new SimpleDecoratorApplier("{{t}} "), content);
        assertDecoratedContent("TT", new SimpleDecoratorApplier("{{t}}{{t}}"), content);
    }

    private void assertDecoratedContent(String expected, SimpleDecoratorApplier decoratorApplier,
                                        ContentStub content) throws IOException {
        ContextStub context = new ContextStub();
        decoratorApplier.decorate(content, context);
        assertEquals(expected, context.toString());
    }

}
