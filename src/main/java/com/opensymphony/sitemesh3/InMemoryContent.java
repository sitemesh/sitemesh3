package com.opensymphony.sitemesh3;

import com.opensymphony.sitemesh3.tagprocessor.util.CharSequenceList;

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

    private Property original = EMPTY_PROPERTY;
    private Property processed = EMPTY_PROPERTY;
    private final Map<String, Property> properties = new HashMap<String, Property>();

    public InMemoryContent() throws IOException {
        setOriginal(EMPTY_PROPERTY);
    }

    protected Property toProperty(CharSequence charSequence) {
        if (charSequence == null) {
            return EMPTY_PROPERTY;
        } else if (charSequence instanceof CharSequenceList) {
            // Optimization.
            return new CharSequenceListProperty((CharSequenceList) charSequence);
        } else {
            return new CharSequenceProperty(charSequence);
        }
    }

    @Override
    public void setOriginal(CharSequence original) {
        setOriginal(toProperty(original));
    }

    @Override
    public void setOriginal(Property original) {
        this.original = original;
    }

    @Override
    public void setProcessed(CharSequence processed) {
        setProcessed(toProperty(processed));
    }

    @Override
    public void setProcessed(Property processed) {
        this.processed = processed;
    }

    @Override
    public void addProperty(String name, Property property) {
        properties.put(name, property);
    }

    @Override
    public void addProperty(String name, CharSequence value) {
        addProperty(name, toProperty(value));
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
    public Property getProcessed() {
        return processed;
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

    /**
     * Implementation of {@link Content.Property} that is optimized for
     * {@link com.opensymphony.sitemesh3.tagprocessor.util.CharSequenceList}.
     */
    private static class CharSequenceListProperty implements Content.Property {
        private final CharSequenceList value;

        public CharSequenceListProperty(CharSequenceList value) {
            this.value = value;
        }

        @Override
        public boolean exists() {
            return true;
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
            value.writeTo(out);
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
