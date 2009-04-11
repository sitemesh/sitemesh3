package com.opensymphony.sitemesh3;

import com.opensymphony.sitemesh3.tagprocessor.util.CharSequenceList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

/**
 * {@link Content} implementation that stores properties in memory in a hashtable.
 *
 * @author Joe Walnes
 */
public class InMemoryContent implements Content {

    private Property original = new CharSequenceProperty();
    private Property processed = new CharSequenceProperty();
    private final Map<String, Property> properties = new HashMap<String, Property>();

    @Override
    public Property getProperty(String name) {
        Property property = properties.get(name);
        if (property == null) {
            property = new CharSequenceProperty();
            properties.put(name, property);
        }
        return property;
    }

    @Override
    public Property getOriginal() {
        return original;
    }

    @Override
    public Property getProcessed() {
        return processed;
    }

    @Override
    public Iterator<Map.Entry<String, Property>> iterator() {
        List<Map.Entry<String, Property>> list = new ArrayList<Map.Entry<String, Property>>(properties.size());
        for (Map.Entry<String, Property> next : properties.entrySet()) {
            if (next.getValue().exists()) {
                list.add(next);
            }
        }
        return list.iterator();
    }

    private static class CharSequenceProperty implements Content.Property {

        private CharSequence value;

        @Override
        public boolean exists() {
            return value != null;
        }

        @Override
        public String value() {
            return value != null ? value.toString() : null;
        }

        @Override
        public String valueNeverNull() {
            return value != null ? value.toString() : "";
        }

        @Override
        public void writeTo(Appendable out) throws IOException {
            if (value == null) {
                return;
            }
            if (value instanceof CharSequenceList) {
                // Optimization.
                CharSequenceList charSequenceList = (CharSequenceList) value;
                charSequenceList.writeTo(out);
            } else {
                out.append(value);
            }
        }

        @Override
        public void update(CharSequence data) {
            value = data;
        }

        @Override
        public String toString() {
            return valueNeverNull();
        }
    }

}
