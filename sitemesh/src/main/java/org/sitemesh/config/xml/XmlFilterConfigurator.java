package org.sitemesh.config.xml;

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

    public void configureFilter(BaseSiteMeshFilterBuilder builder) {

        // Common configuration
        configureCommon(builder);

        // Filter specific configuration...

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

        // Custom selector
        // TODO
    }

    private boolean isTrue(String string) {
        String lower = string == null ? "" : string.trim().toLowerCase();
        return lower.equals("true") || lower.equals("1") || lower.equals("yes");
    }

}