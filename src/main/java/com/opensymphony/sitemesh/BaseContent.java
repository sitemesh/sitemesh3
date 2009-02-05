package com.opensymphony.sitemesh;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class BaseContent implements Content {

    private final Property original;
    private final Map<String, Property> properties = new HashMap<String, Property>();

    public BaseContent(String original) throws IOException {
        this.original = new StringProperty(original);
        processContent(original);
    }

    protected abstract void processContent(String original) throws IOException;

    public void addProperty(String name, Property property) {
        properties.put(name, property);
    }

    public void addProperty(String name, String value) {
        addProperty(name, new StringProperty(value));
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

    public static class StringProperty implements Content.Property {

        private final String value;

        public StringProperty(String value) {
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
            return value;
        }

        @Override
        public String valueNeverNull() {
            return value == null ? "" : value;
        }

        @Override
        public void writeTo(Appendable out) throws IOException {
            out.append(value);
        }

        @Override
        public String toString() {
            return value();
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
