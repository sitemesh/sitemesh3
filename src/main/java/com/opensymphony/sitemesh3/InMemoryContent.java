package com.opensymphony.sitemesh3;

import com.opensymphony.sitemesh3.tagprocessor.util.CharSequenceList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;


/**
 * {@link Content} implementation that stores properties in memory in a hashtable.
 *
 * @author Joe Walnes
 */
public class InMemoryContent implements Content {

    private Property original = new CharSequenceProperty();
    private Property processed = new CharSequenceProperty();
    private Property root = new CharSequenceProperty();

    @Override
    public Property getProperty(String name) {
        return root.getChild(name);
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
    public Property getRoot() {
        return root;
    }

    private static class CharSequenceProperty implements Property, Iterable<Property> {

        private static final Iterator<Property> EMPTY_ITERATOR = Collections.<Property>emptySet().iterator();

        private CharSequence value;

        private final boolean isRoot;
        private final String name;
        private final CharSequenceProperty parent;
        private Map<String,Property> children; // Lazily instantiated.

        public CharSequenceProperty() {
            name = null;
            parent = null;
            isRoot = true;
        }

        private CharSequenceProperty(String name, CharSequenceProperty parent) {
            this.name = name;
            this.parent = parent;
            isRoot = false;
        }

        @Override
        public boolean hasValue() {
            return value != null;
        }

        @Override
        public String getValue() {
            return value != null ? value.toString() : null;
        }

        @Override
        public String getNonNullValue() {
            return value != null ? value.toString() : "";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Property[] getFullPath() {
            // Determine size of array.
            int size = 0;
            for (CharSequenceProperty node = this; !node.isRoot; node = node.parent) {
                size++;
            }

            Property[] result = new Property[size];

            // Build array.
            int index = 0;
            for (CharSequenceProperty node = this; !node.isRoot; node = node.parent, index++) {
                result[result.length - 1 - index] = node; // Start from end of array.
            }

            return result;
        }

        @Override
        public void writeValueTo(Appendable out) throws IOException {
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
        public void setValue(CharSequence value) {
            this.value = value;
        }

        @Override
        public boolean hasChildren() {
            return children != null && !children.isEmpty();
        }

        @Override
        public boolean hasChild(String childName) {
            return children != null && children.containsKey(childName);
        }

        @Override
        public Property getChild(String childName) {
            if (children == null) {
                children = new HashMap<String, Property>();
            }

            Property property = children.get(childName);
            if (property == null) {
                property = new CharSequenceProperty(childName, this);
                children.put(childName, property);
            }
            return property;
        }

        @Override
        public Property getParent() {
            return parent;
        }


        @Override
        public Iterable<Property> getChildren() {
            return this;
        }

        @Override
        public Iterator<Property> iterator() {
            return children == null ? EMPTY_ITERATOR : children.values().iterator();
        }

        @Override
        public Iterable<Property> getDescendants() {
            Property current = CharSequenceProperty.this;
            List<Property> descendants = new LinkedList<Property>();
            walk(current, descendants);
            return descendants;
        }

        private void walk(Property node, List<Property> result) {
            result.add(node);
            for (Property child : node.getChildren()) {
                walk(child, result);
            }
        }

        @Override
        public String toString() {
            return getNonNullValue();
        }
    }

}
