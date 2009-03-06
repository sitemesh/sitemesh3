package com.opensymphony.sitemesh;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * {@link Content} implementation that stores properties in memory in a hashtable.
 *
 * @author Joe Walnes
 */
public class InMemoryContent implements Content {

    private final Property original;
    private final Map<String, Property> properties = new HashMap<String, Property>();

    public InMemoryContent(Property original) throws IOException {
        this.original = original;
    }

    public InMemoryContent(CharSequence original) throws IOException {
        this(original == null ? EMPTY_PROPERTY : new CharSequenceProperty(original));
    }

    public InMemoryContent() throws IOException {
        this(EMPTY_PROPERTY);
    }

    public void addProperty(String name, Property property) {
        properties.put(name, property);
    }

    public void addProperty(String name, CharSequence value) {
        addProperty(name, value == null ? EMPTY_PROPERTY : new CharSequenceProperty(value));
    }

    @Override
    public Property getProperty(String name) {
        Property result = properties.get(name);
        return result == null ? EMPTY_PROPERTY : result;
    }

    @Override
    public Property getOriginal() {
        return original;
    }

    @Override
    public Iterator<Map.Entry<String, Property>> iterator() {
        return properties.entrySet().iterator();
    }

    private static class CharSequenceProperty implements Content.Property {

        private final CharSequence value;

        public CharSequenceProperty(CharSequence value) {
            this.value = value;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public String value() {
            return value.toString();
        }

        @Override
        public String valueNeverNull() {
            return value.toString();
        }

        @Override
        public void writeTo(Appendable out) throws IOException {
            out.append(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static final Property EMPTY_PROPERTY = new Property() {
        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public String value() {
            return null;
        }

        @Override
        public String valueNeverNull() {
            return "";
        }

        @Override
        public void writeTo(Appendable out) throws IOException {
        }

        @Override
        public String toString() {
            return "";
        }
    };
}
