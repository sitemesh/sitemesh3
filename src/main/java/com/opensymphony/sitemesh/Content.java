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

    Property getProperty(String name);

    Property getOriginal();

}
