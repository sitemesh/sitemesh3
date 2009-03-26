package com.opensymphony.sitemesh.decorator.simple;

import com.opensymphony.sitemesh.ContextStub;
import com.opensymphony.sitemesh.InMemoryContent;
import com.opensymphony.sitemesh.Content;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Joe Walnes
 */
public class SimpleDecoratorApplierTest extends TestCase {

    private Content content;
    private SimpleDecoratorApplier decoratorApplier;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        content = new InMemoryContent();
        decoratorApplier = new SimpleDecoratorApplier();
    }

    public void testSubstitutesTokensWithContentProperties() throws IOException {
        setupDecorator("Hello {{name.first}} {{name.last}}!\n{{message}}");

        content.addProperty("name.first", "You");
        content.addProperty("name.last", "There");
        content.addProperty("message", "How are you?");

        assertEquals("Hello You There!\nHow are you?", applyDecorator());
    }

    public void testSkipsMissingProperties() throws IOException {
        setupDecorator("Hello {{unknown}} {{this.too}}!");

        assertEquals("Hello  !", applyDecorator());
    }

    public void testHandlesEdgeCasingParsing() throws IOException {
        content.addProperty("t", "T");

        setupDecorator("");
        assertEquals("", applyDecorator());

        setupDecorator("\n");
        assertEquals("\n", applyDecorator());

        setupDecorator("  ");
        assertEquals("  ", applyDecorator());

        setupDecorator("{{t}}");
        assertEquals("T", applyDecorator());

        setupDecorator(" {{t}}");
        assertEquals(" T", applyDecorator());

        setupDecorator("{{t}} ");
        assertEquals("T ", applyDecorator());

        setupDecorator("{{t}}{{t}}");
        assertEquals("TT", applyDecorator());
    }

    private void setupDecorator(String templateContents) {
        decoratorApplier.put("mydecorator", templateContents);
    }

    private String applyDecorator() throws IOException {
        StringWriter out = new StringWriter();
        ContextStub context = new ContextStub();
        decoratorApplier.decorate("mydecorator", content, context, out);
        return out.toString();
    }

}