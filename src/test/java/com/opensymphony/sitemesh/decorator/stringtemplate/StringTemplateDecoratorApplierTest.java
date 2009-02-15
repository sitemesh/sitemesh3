package com.opensymphony.sitemesh.decorator.stringtemplate;

import com.opensymphony.sitemesh.ContentStub;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.ContextStub;
import com.opensymphony.sitemesh.DecoratorApplier;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * @author Joe Walnes
 */
public class StringTemplateDecoratorApplierTest extends TestCase {

    public void testSubstitutesTokensWithContentProperties() throws IOException {
        StringTemplateDecoratorApplier decoratorApplier = new StringTemplateDecoratorApplier(
                "Hello $name$.\n$message$");

        ContentStub content = new ContentStub();
        content.addProperty("name", "You");
        content.addProperty("message", "How are you?");

        assertDecoratedContent("Hello You.\nHow are you?", decoratorApplier, content);
    }

    public void testSkipsMissingProperties() throws IOException {
        StringTemplateDecoratorApplier decoratorApplier = new StringTemplateDecoratorApplier(
                "Hello $unknown$!");

        ContentStub content = new ContentStub();
        assertDecoratedContent("Hello !", decoratorApplier, content);
    }

    public void testReplacesDotBasedPropertiesWithUnderscores() throws IOException {
        StringTemplateDecoratorApplier decoratorApplier = new StringTemplateDecoratorApplier(
                "Hello $name_first$ $name_last$. $name_missing$ $missing_name$");

        ContentStub content = new ContentStub();
        content.addProperty("name.first", "You");
        content.addProperty("name.last", "There");

        assertDecoratedContent("Hello You There.  ", decoratorApplier, content);
    }

    public void testDoesNotEscapeValues() throws IOException {
        StringTemplateDecoratorApplier decoratorApplier = new StringTemplateDecoratorApplier(
                "Hello $thing$");

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
