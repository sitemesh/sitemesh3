package com.opensymphony.sitemesh.decorator.velocity;

import com.opensymphony.sitemesh.ContextStub;
import com.opensymphony.sitemesh.DecoratorApplier;
import com.opensymphony.sitemesh.InMemoryContent;
import com.opensymphony.sitemesh.Content;
import junit.framework.TestCase;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Joe Walnes
 */
public class VelocityDecoratorApplierTest extends TestCase {

    private StringResourceRepository velocityRepository;

    private DecoratorApplier decoratorApplier;
    private Content content;
    private ContextStub context;

    private static final String DECORATOR_NAME = "mydecorator.vm";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Velocity setup for in-memory use.
        velocityRepository = new StringResourceRepositoryImpl();
        VelocityEngine velocityEngine = new VelocityEngine();
        String repositoryName = "inmemoryrepository";
        velocityEngine.setProperty("resource.loader", "string");
        velocityEngine.setProperty("string.resource.loader.class", StringResourceLoader.class.getName());
        velocityEngine.setProperty("string.resource.loader.repository.static", false);
        velocityEngine.setProperty("string.resource.loader.repository.name", repositoryName);
        velocityEngine.setApplicationAttribute(repositoryName, velocityRepository);
        velocityEngine.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute.class.getName());
        velocityEngine.init();

        // SiteMesh test objects.
        content = new InMemoryContent();
        context = new ContextStub();
        decoratorApplier = new VelocityDecoratorApplier(velocityEngine);
    }

    public void testSubstitutesTokensWithContentProperties() throws IOException {
        setupDecorator("Hello $content.name.\n$content.message");

        content.addProperty("name", "You");
        content.addProperty("message", "How are you?");

        assertEquals("Hello You.\nHow are you?", applyDecorator());
    }

    public void testSkipsMissingProperties() throws IOException {
        setupDecorator("Hello $content.unknowna $!content.unknownb!");

        assertEquals("Hello  !", applyDecorator());
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
        setupDecorator("$sitemeshContext.stuff");

        context = new MyContext();

        assertEquals("Some stuff", applyDecorator());
    }

    public void testDoesNotEscapeValues() throws IOException {
        setupDecorator("Hello ${content.thing}");

        content.addProperty("thing", "<tag/>\\ \"&'");

        assertEquals("Hello <tag/>\\ \"&'", applyDecorator());
    }

    private String applyDecorator() throws IOException {
        StringWriter out = new StringWriter();
        decoratorApplier.decorate(DECORATOR_NAME, content, context, out);
        return out.toString();
    }

    private void setupDecorator(String templateContents) {
        velocityRepository.putStringResource(DECORATOR_NAME, templateContents);
    }

}
