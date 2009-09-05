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