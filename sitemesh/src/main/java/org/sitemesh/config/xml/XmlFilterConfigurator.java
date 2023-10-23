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

import org.sitemesh.DecoratorSelector;
import org.sitemesh.builder.BaseSiteMeshFilterBuilder;
import org.sitemesh.config.ObjectFactory;
import org.w3c.dom.Element;

import java.util.List;
import java.util.ArrayList;

public class XmlFilterConfigurator extends XmlConfigurator {

    private final Xml xml;

    public XmlFilterConfigurator(ObjectFactory objectFactory, Element siteMeshElement) {
        super(objectFactory, siteMeshElement);
        this.xml = new Xml(siteMeshElement);
    }

    @SuppressWarnings({
            "rawtypes", "unchecked"
    }) public void configureFilter(BaseSiteMeshFilterBuilder builder) {

        // Common configuration
        configureCommon(builder);

        // Filter specific configuration...
        
        String customDecoratorSelector = xml.child("decorator-selector").text();
        if (customDecoratorSelector != null) {
            builder.setCustomDecoratorSelector((DecoratorSelector) getObjectFactory().create(customDecoratorSelector));
        }

        String decoratorPrefix = xml.child("decorator-prefix").text();
        if (decoratorPrefix != null) {
            builder.setDecoratorPrefix(decoratorPrefix);
        }

        // Error pages inclusion
        String includeErrorPagesString = xml.child("include-error-pages").text("false");
        if ("true".equals(includeErrorPagesString) || "1".equals(includeErrorPagesString)) {
            builder.setIncludeErrorPages(true);
        }
        
        // Excludes
        for (Xml mapping : xml.children("mapping")) {
            String path = mapping.child("path").text(mapping.attribute("path", "/*"));
            if (isTrue(mapping.attribute("exclude")) || !mapping.children("exclude").isEmpty()) {
                builder.addExcludedPath(path);
            }
        }

        // Mime-types
        List<Xml> mimeTypeTags = xml.children("mime-type");
        if (!mimeTypeTags.isEmpty()) {
            List<String> mimeTypes = new ArrayList<String>();
            for (Xml mimeTypeTag : mimeTypeTags) {
                mimeTypes.add(mimeTypeTag.text());
            }
            builder.setMimeTypes(mimeTypes);
        }
    }

    private boolean isTrue(String string) {
        String lower = string == null ? "" : string.trim().toLowerCase();
        return lower.equals("true") || lower.equals("1") || lower.equals("yes");
    }

}