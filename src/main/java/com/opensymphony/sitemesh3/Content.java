package com.opensymphony.sitemesh3;

import java.io.IOException;
import java.util.Map;

/**
 * @author Joe Walnes
 */
public interface Content extends Iterable<Map.Entry<String,Content.Property>> {

    interface Property {
        boolean exists();
        String value();
        String valueNeverNull();
        void writeTo(Appendable out) throws IOException;
        void update(CharSequence data);
    }

    Property getOriginal();

    Property getProcessed();

    Property getProperty(String name);

}
