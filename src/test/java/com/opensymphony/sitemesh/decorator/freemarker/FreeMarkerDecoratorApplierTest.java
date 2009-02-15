package com.opensymphony.sitemesh.decorator.freemarker;

import com.opensymphony.sitemesh.ContentStub;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.ContextStub;
import com.opensymphony.sitemesh.DecoratorApplier;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author Joe Walnes
 */
public class FreeMarkerDecoratorApplierTest extends TestCase {

    public void testSubstitutesTokensWithContentProperties() throws IOException {
        FreeMarkerDecoratorApplier decoratorApplier = new FreeMarkerDecoratorApplier(
                new StringReader("Hello ${content.name}.\n${content.message}"));

        ContentStub content = new ContentStub();
        content.addProperty("name", "You");
        content.addProperty("message", "How are you?");

        assertDecoratedContent("Hello You.\nHow are you?", decoratorApplier, content);
    }

    public void testSkipsMissingProperties() throws IOException {
        FreeMarkerDecoratorApplier decoratorApplier = new FreeMarkerDecoratorApplier(
                new StringReader("Hello ${content.unknown}!"));

        ContentStub content = new ContentStub();
        assertDecoratedContent("Hello !", decoratorApplier, content);
    }

    public void testDealsWithPropertiesWithDots() throws IOException {
        FreeMarkerDecoratorApplier decoratorApplier = new FreeMarkerDecoratorApplier(
                new StringReader("Hello ${content.name.first} ${content.name.last}. " +
                        "${content.name.missing} ${content.missing.name}"));

        ContentStub content = new ContentStub();
        content.addProperty("name.first", "You");
        content.addProperty("name.last", "There");

        assertDecoratedContent("Hello You There.  ", decoratorApplier, content);
    }

    public static class MyContext extends ContextStub {
        public String getStuff() {
            return "Some stuff";
        }
    }

    public void testExposesValuesOfSitemeshContextToTemplate() throws IOException {
        FreeMarkerDecoratorApplier decoratorApplier = new FreeMarkerDecoratorApplier(
                new StringReader("${sitemeshContext.stuff}"));

        ContentStub content = new ContentStub();
        ContextStub context = new MyContext();
        decoratorApplier.decorate(content, context);
        assertEquals("Some stuff", context.toString());
    }

    public void testDoesNotEscapeValues() throws IOException {
        FreeMarkerDecoratorApplier decoratorApplier = new FreeMarkerDecoratorApplier(
                new StringReader("Hello ${content.thing}"));

        ContentStub content = new ContentStub();
        content.addProperty("thing", "<tag/>\\ \"&'");

        assertDecoratedContent("Hello <tag/>\\ \"&'", decoratorApplier, content);
    }

    private void assertDecoratedContent(String expected,
                                        DecoratorApplier<Context> decoratorApplier,
                                        ContentStub content) throws IOException {
        ContextStub context = new ContextStub();
        decoratorApplier.decorate(content, context);
        assertEquals(expected, context.toString());
    }

}
