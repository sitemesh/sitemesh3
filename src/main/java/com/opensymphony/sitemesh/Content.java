package com.opensymphony.sitemesh;

import java.io.IOException;
import java.util.Map;

/**
 * @author Joe Walnes
 * @since SiteMesh 3
 */
public interface Content extends Iterable<Map.Entry<String,Content.Property>> {

    interface Property {
        boolean exists();
        String value();
        String valueNeverNull();
        void writeTo(Appendable out) throws IOException;
    }

    Property getOriginal();
    void setOriginal(CharSequence original);
    void setOriginal(Property original);

    Property getProcessed();
    void setProcessed(CharSequence original);
    void setProcessed(Property original);

    Property getProperty(String name);
    void addProperty(String name, Property property);
    void addProperty(String name, CharSequence value);

}
