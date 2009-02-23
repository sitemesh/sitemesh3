package com.opensymphony.sitemesh.decorator.stringtemplate;

import com.opensymphony.sitemesh.ContentStub;
import com.opensymphony.sitemesh.ContextStub;
import junit.framework.TestCase;

import java.io.IOException;

import org.antlr.stringtemplate.StringTemplate;

/**
 * @author Joe Walnes
 */
public class StringTemplateDecoratorApplierTest extends TestCase {
    private ContentStub content;
    private StringTemplateDecoratorApplier decoratorApplier;

    private static final String DECORATOR_NAME = "mydecorator";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        decoratorApplier = new StringTemplateDecoratorApplier();
        content = new ContentStub();
    }

    public void testSubstitutesTokensWithContentProperties() throws IOException {
        setupDecorator("Hello $name$.\n$message$");

        content.addProperty("name", "You");
        content.addProperty("message", "How are you?");

        assertEquals("Hello You.\nHow are you?", applyDecorator());
    }

    public void testSkipsMissingProperties() throws IOException {
        setupDecorator("Hello $unknown$!");

        assertEquals("Hello !", applyDecorator());
    }

    public void testReplacesDotBasedPropertiesWithUnderscores() throws IOException {
        setupDecorator("Hello $name_first$ $name_last$. $name_missing$ $missing_name$");

        content.addProperty("name.first", "You");
        content.addProperty("name.last", "There");

        assertEquals("Hello You There.  ", applyDecorator());
    }

    public void testDoesNotEscapeValues() throws IOException {
        setupDecorator("Hello $thing$");

        content.addProperty("thing", "<tag/>\\ \"&'");

        assertEquals("Hello <tag/>\\ \"&'", applyDecorator());
    }

    private void setupDecorator(String templateContents) {
        decoratorApplier.put(DECORATOR_NAME, new StringTemplate(templateContents));
    }

    private String applyDecorator() throws IOException {
        ContextStub context = new ContextStub();
        decoratorApplier.decorate(DECORATOR_NAME, content, context);
        return context.getWrittenData();
    }

}
