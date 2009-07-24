package org.sitemesh.config.xml;

import org.sitemesh.config.ObjectFactory;
import org.sitemesh.builder.BaseSiteMeshBuilder;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.ContentProcessor;
import org.w3c.dom.Element;

import java.util.List;
import java.util.ArrayList;

public class XmlConfigurator {

    private final Xml xml;
    private final ObjectFactory objectFactory;

    public XmlConfigurator(ObjectFactory objectFactory, Element siteMeshElement) {
        this.objectFactory = objectFactory;
        this.xml = new Xml(siteMeshElement);
    }

    public void configureCommon(BaseSiteMeshBuilder builder) {

        // TagRuleBundles
        // TODO: Support clearTagRuleBundles()
        for(Xml tagRuleBundle : xml.child("content-processor").children("tag-rule-bundle")) {
            String tagRuleBundleName = tagRuleBundle.attribute("class");
            if (tagRuleBundleName != null) {
                builder.addTagRuleBundle((TagRuleBundle) objectFactory.create(tagRuleBundleName));
            }
        }

        // Custom ContentProcessor
        String contentProcessorName = xml.child("content-processor").attribute("class");
        if (contentProcessorName != null) {
            builder.setCustomContentProcessor(
                    (ContentProcessor) objectFactory.create(contentProcessorName));
        }

        // Decorator mappings
        for (Xml mapping : xml.children("mapping")) {
            String path = mapping.child("path").text(mapping.attribute("path", "/*"));

            List<String> decorators = new ArrayList<String>();
            if (mapping.attribute("decorator") != null) {
                decorators.add(mapping.attribute("decorator"));
            }
            for (Xml decorator : mapping.children("decorator")) {
                if (decorator.text() != null) {
                    decorators.add(decorator.text());
                }
            }

            builder.addDecoratorPaths(path, decorators);
        }
    }

}
