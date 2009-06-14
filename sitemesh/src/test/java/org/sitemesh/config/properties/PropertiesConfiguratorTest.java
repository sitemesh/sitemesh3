package org.sitemesh.config.properties;

import junit.framework.TestCase;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.memory.InMemoryContent;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.SiteMeshContextStub;
import org.sitemesh.DecoratorSelector;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.builder.BaseSiteMeshBuilder;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.tagprocessor.State;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Joe Walnes
 */
public class PropertiesConfiguratorTest extends TestCase {

    protected Map<String, String> properties;
    private PropertiesConfigurator propertiesConfigurator;
    private BaseSiteMeshBuilder builder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        properties = new HashMap<String, String>();
        propertiesConfigurator = createConfigurator(new ObjectFactory.Default(), properties);
        builder = new SiteMeshFilterBuilder();
    }

    protected PropertiesConfigurator createConfigurator(ObjectFactory objectFactory, Map<String, String> properties) {
        return new PropertiesConfigurator(objectFactory, properties);
    }

    public void testAddsTagRuleBundles() {
        // Sanity check - there should be 2 default bundles already.
        TagBasedContentProcessor contentProcessor = (TagBasedContentProcessor) builder.getContentProcessor();
        TagRuleBundle[] ruleBundles = contentProcessor.getTagRuleBundles();
        assertEquals(2, ruleBundles.length);
        assertEquals(CoreHtmlTagRuleBundle.class, ruleBundles[0].getClass());
        assertEquals(DecoratorTagRuleBundle.class, ruleBundles[1].getClass());

        // Configure with param.
        String tagRulesValue = MyTagRuleBundle.class.getName() + ", " + AnotherTagRuleBundle.class.getName();
        properties.put(PropertiesConfigurator.TAG_RULE_BUNDLES_PARAM, tagRulesValue);
        propertiesConfigurator.configureCommon(builder);

        // There should now be the original 2, plus the 2 new ones.
        contentProcessor = (TagBasedContentProcessor) builder.getContentProcessor();
        ruleBundles = contentProcessor.getTagRuleBundles();
        assertEquals(4, ruleBundles.length);
        assertEquals(CoreHtmlTagRuleBundle.class, ruleBundles[0].getClass());
        assertEquals(DecoratorTagRuleBundle.class, ruleBundles[1].getClass());
        assertEquals(MyTagRuleBundle.class, ruleBundles[2].getClass());
        assertEquals(AnotherTagRuleBundle.class, ruleBundles[3].getClass());

        // Check that the bundles have actually been added to the ContentProcessor.
        assertEquals(TagBasedContentProcessor.class, builder.getContentProcessor().getClass());
        contentProcessor = (TagBasedContentProcessor) builder.getContentProcessor();
        ruleBundles = contentProcessor.getTagRuleBundles();
        assertEquals(4, ruleBundles.length);
        assertEquals(CoreHtmlTagRuleBundle.class, ruleBundles[0].getClass());
        assertEquals(DecoratorTagRuleBundle.class, ruleBundles[1].getClass());
        assertEquals(MyTagRuleBundle.class, ruleBundles[2].getClass());
        assertEquals(AnotherTagRuleBundle.class, ruleBundles[3].getClass());
    }

    // Supports previous test.
    public static class MyTagRuleBundle implements TagRuleBundle {
        public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
            // no-op.
        }

        public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
            // No op.
        }
    }

    // Supports previous test.
    public static class AnotherTagRuleBundle implements TagRuleBundle {
        public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
            // no-op.
        }

        public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
            // No op.
        }
    }

    public void testSetsContentProcessor() throws IOException {
        properties.put(PropertiesConfigurator.CONTENT_PROCESSOR_PARAM, MyContentProcessor.class.getName());
        propertiesConfigurator.configureCommon(builder);

        assertEquals(MyContentProcessor.class, builder.getContentProcessor().getClass());
        assertSame(MyContentProcessor.MY_RETURNED_CONTENT, builder.getContentProcessor().build(null, null));
    }

    // Supports previous test.
    public static class MyContentProcessor implements ContentProcessor {
        public static final Content MY_RETURNED_CONTENT = new InMemoryContent();

        public Content build(CharBuffer data, SiteMeshContext context) throws IOException {
            return MY_RETURNED_CONTENT;
        }
    }

    @SuppressWarnings("unchecked")
    public void testAddsDecoratorMappings() throws IOException {
        properties.put(PropertiesConfigurator.DECORATOR_MAPPINGS_PARAM, "" +
                "/a/*=/decorator/a," +
                "/b/*=/decorator/b,");
        propertiesConfigurator.configureCommon(builder);

        Content someContent = new InMemoryContent();
        DecoratorSelector<SiteMeshContext> decoratorSelector = builder.getDecoratorSelector();
        assertArrayEquals(
                decoratorSelector.selectDecoratorPaths(someContent, new SiteMeshContextStub().withPath("/a/foo")),
                "/decorator/a");
        assertArrayEquals(
                decoratorSelector.selectDecoratorPaths(someContent, new SiteMeshContextStub().withPath("/b/foo")),
                "/decorator/b");
        assertArrayEquals(
                decoratorSelector.selectDecoratorPaths(someContent, new SiteMeshContextStub().withPath("/c/foo"))
                /* nothing */);
    }

    @SuppressWarnings("unchecked")
    public void testSupportsMultipleDecoratorMappingsForASinglePath() throws IOException {
        properties.put(PropertiesConfigurator.DECORATOR_MAPPINGS_PARAM, "" +
                "/a/*=/decorator/a1|/decorator/a2," +
                "/b/*=/decorator/b1|/decorator/b2," +
                "/c/*=/decorator/c,");
        propertiesConfigurator.configureCommon(builder);

        Content someContent = new InMemoryContent();
        DecoratorSelector<SiteMeshContext> decoratorSelector = builder.getDecoratorSelector();
        assertArrayEquals(
                decoratorSelector.selectDecoratorPaths(someContent, new SiteMeshContextStub().withPath("/a/foo")),
                "/decorator/a1", "/decorator/a2");
        assertArrayEquals(
                decoratorSelector.selectDecoratorPaths(someContent, new SiteMeshContextStub().withPath("/b/foo")),
                "/decorator/b1", "/decorator/b2");
        assertArrayEquals(
                decoratorSelector.selectDecoratorPaths(someContent, new SiteMeshContextStub().withPath("/c/foo")),
                "/decorator/c");
        assertArrayEquals(
                decoratorSelector.selectDecoratorPaths(someContent, new SiteMeshContextStub().withPath("/d/foo"))
                /* nothing */);
    }

    public void testCanBeHookedUpToOtherObjectFactories() {
        final ContentProcessor aContentProcessor = new MyContentProcessor();
        ObjectFactory someDependencyInjectionFramework = new ObjectFactory() {
            public Object create(String className) {
                if (className.equals("foo")) {
                    return aContentProcessor;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        };

        properties.put(PropertiesConfigurator.CONTENT_PROCESSOR_PARAM, "foo");
        propertiesConfigurator = createConfigurator(someDependencyInjectionFramework, properties);
        propertiesConfigurator.configureCommon(builder);

        assertSame(aContentProcessor, builder.getContentProcessor());
    }

    // Test helpers.

    protected void assertArrayEquals(String actual[], String... expected) {
        assertEquals(join(expected), join(actual));
    }

    protected String join(String[] strings) {
        StringBuilder result = new StringBuilder();
        for (String string : strings) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(string);
        }
        return result.toString();
    }
}
