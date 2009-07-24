package org.sitemesh.config.properties;

import org.sitemesh.builder.BaseSiteMeshBuilder;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.config.ObjectFactory;

import java.util.Map;

/**
 * @author Joe Walnes
 */
public class PropertiesConfigurator {

    public static final String TAG_RULE_BUNDLES_PARAM = "tagRuleBundles";
    public static final String CONTENT_PROCESSOR_PARAM = "contentProcessor";
    public static final String DECORATOR_MAPPINGS_PARAM = "decoratorMappings";

    private final ObjectFactory objectFactory;
    private final PropertiesParser properties;

    public PropertiesConfigurator(ObjectFactory objectFactory, Map<String, String> properties) {
        this.objectFactory = objectFactory;
        this.properties = new PropertiesParser(properties);
    }

    public void configureCommon(BaseSiteMeshBuilder builder) {

        // TagRuleBundles
        String[] ruleSetNames = properties.getStringArray(TAG_RULE_BUNDLES_PARAM);
        // TODO: Support clearTagRuleBundles()
        for (String ruleSetName : ruleSetNames) {
            builder.addTagRuleBundle((TagRuleBundle) objectFactory.create(ruleSetName));
        }

        // Custom ContentProcessor
        String contentProcessorName = properties.getString(CONTENT_PROCESSOR_PARAM);
        if (contentProcessorName != null) {
            builder.setCustomContentProcessor(
                    (ContentProcessor) objectFactory.create(contentProcessorName));
        }

        // Decorator mappings
        Map<String, String[]> decoratorsMappings = properties.getStringMultiMap(DECORATOR_MAPPINGS_PARAM);
        if (decoratorsMappings != null) {
            for (Map.Entry<String, String[]> entry : decoratorsMappings.entrySet()) {
                builder.addDecoratorPaths(entry.getKey(), entry.getValue());
            }
        }

    }
}
