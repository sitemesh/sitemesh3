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

import org.sitemesh.builder.BaseSiteMeshOfflineBuilder;
import org.sitemesh.config.ObjectFactory;
import org.w3c.dom.Element;

public class XmlOfflineConfigurator extends XmlConfigurator {

    // TODO: Tests

    private final Xml xml;

    public XmlOfflineConfigurator(ObjectFactory objectFactory, Element siteMeshElement) {
        super(objectFactory, siteMeshElement);
        this.xml = new Xml(siteMeshElement);
    }

    public void configureOffline(BaseSiteMeshOfflineBuilder builder) {

        // Common configuration
        configureCommon(builder);

        // Offline specific configuration...
        String sourceDir = xml.attribute("source-dir");
        if (sourceDir != null) {
            builder.setSourceDirectory(sourceDir);
        }
        String destDir = xml.attribute("destination-dir");
        if (destDir != null) {
            builder.setDestinationDirectory(destDir);
        }
        
    }

}