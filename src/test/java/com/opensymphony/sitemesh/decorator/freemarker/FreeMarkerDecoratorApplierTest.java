package com.opensymphony.sitemesh.decorator.freemarker;

import com.opensymphony.sitemesh.ContextStub;
import com.opensymphony.sitemesh.DecoratorApplier;
import com.opensymphony.sitemesh.InMemoryContent;
import com.opensymphony.sitemesh.Content;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Joe Walnes
 */
public class FreeMarkerDecoratorApplierTest extends TestCase {

    private DecoratorApplier decoratorApplier;
    private Content content;
    private ContextStub context;
    private StringTemplateLoader templateLoader;

    private static final String DECORATOR_NAME = "mydecorator.ftl";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Configuration configuration = new Configuration();
        templateLoader = new StringTemplateLoader();
        configuration.setTemplateLoader(templateLoader);
        decoratorApplier = new FreeMarkerDecoratorApplier(configuration);
        content = new InMemoryContent();
        context = new ContextStub();
    }

    public void testSubstitutesTokensWithContentProperties() throws IOException {
        setupDecorator("Hello ${content.name}.\n${content.message}");

        content.addProperty("name", "You");
        content.addProperty("message", "How are you?");

        assertEquals("Hello You.\nHow are you?", applyDecorator());
    }

    public void testSkipsMissingProperties() throws IOException {
        setupDecorator("Hello ${content.unknown}!");

        assertEquals("Hello !", applyDecorator());
    }

    public void testDealsWithPropertiesWithDots() throws IOException {
        setupDecorator("Hello ${content.name.first} ${content.name.last}. " +
                        "${content.name.missing} ${content.missing.name}");

        content.addProperty("name.first", "You");
        content.addProperty("name.last", "There");

        assertEquals("Hello You There.  ", applyDecorator());
    }

    public static class MyContext extends ContextStub {
        public String getStuff() {
            return "Some stuff";
        }
    }

    public void testExposesValuesOfSitemeshContextToTemplate() throws IOException {
        setupDecorator("${sitemeshContext.stuff}");

        context = new MyContext();
        assertEquals("Some stuff", applyDecorator());
    }

    public void testDoesNotEscapeValues() throws IOException {
        setupDecorator("Hello ${content.thing}");

        content.addProperty("thing", "<tag/>\\ \"&'");

        assertEquals("Hello <tag/>\\ \"&'", applyDecorator());
    }

    private void setupDecorator(String templateContents) {
        templateLoader.putTemplate(DECORATOR_NAME, templateContents);
    }

    private String applyDecorator() throws IOException {
        StringWriter out = new StringWriter();
        decoratorApplier.decorate(DECORATOR_NAME, content, context, out);
        return out.toString();
    }


}
