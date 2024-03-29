/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
            List<Xml> paths = mapping.children("path");
            if (!paths.isEmpty()) {
                for (Xml path : paths) {
                    addDecoratorPaths(builder, mapping, path.text());
                }
            } else {
                addDecoratorPaths(builder, mapping, mapping.attribute("path", "/*"));
            }
        }
    }

    private void addDecoratorPaths(BaseSiteMeshBuilder builder, Xml mapping, String path) {
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
    
    protected ObjectFactory getObjectFactory() {
        return objectFactory;
    }

}
