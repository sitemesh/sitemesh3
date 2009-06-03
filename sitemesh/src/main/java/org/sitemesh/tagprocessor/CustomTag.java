package org.sitemesh.tagprocessor;

import java.util.Arrays;
import java.io.IOException;
import java.nio.CharBuffer;

/**
 * A CustomTag provides a mechanism to manipulate the contents of a Tag. The standard Tag implementations
 * are immutable, however CustomTag allows a copy to be taken of an immutable Tag that can then be manipulated.
 *
 * @author Joe Walnes
 * @see Tag
 */
public class CustomTag implements Tag {

    private String[] attributes = new String[10]; // name1, value1, name2, value2...
    private int attributeCount = 0;
    private String name;
    private Tag.Type type;

    /**
     * Create new tag.
     */
    public CustomTag(String name, Tag.Type type) {
        setName(name);
        setType(type);
    }

    /**
     * Create a CustomTag based on an existing Tag - this takes a copy of the Tag.
     */
    public CustomTag(Tag tag) {
        setName(tag.getName());
        setType(tag.getType());
        if (tag instanceof TagTokenizer.ReusableToken) {
            TagTokenizer.ReusableToken orig = (TagTokenizer.ReusableToken) tag;
            attributeCount = orig.attributeCount;
            attributes = new String[attributeCount];
            System.arraycopy(orig.attributes, 0, attributes, 0, attributeCount);
        } else if (tag instanceof CustomTag) {
            CustomTag orig = (CustomTag) tag;
            attributeCount = orig.attributeCount;
            attributes = new String[attributeCount];
            System.arraycopy(orig.attributes, 0, attributes, 0, attributeCount);
        } else {
            int c = tag.getAttributeCount();
            attributes = new String[c * 2];
            for (int i = 0; i < c; i++) {
                attributes[attributeCount++] = tag.getAttributeName(i);
                attributes[attributeCount++] = tag.getAttributeValue(i);
            }
        }
    }

    public void writeTo(Appendable out) throws IOException {
        // Calculate length of written tag up front, so we can allocate a
        // buffer of the correct size.
        int length = 0;
        length += type == Tag.Type.CLOSE ? 2 : 1;
        length += name.length();
        for (int i = 0; i < this.attributeCount; i += 2) {
            length += 1 + attributes[i].length();
            if (attributes[i + 1] != null) {
                length += 3 + attributes[i + 1].length();
            }
        }
        length += type == Tag.Type.EMPTY ? 2 : 1;

        // Allocate buffer for tag.
        CharBuffer tag = CharBuffer.allocate(length);

        tag.put('<');
        if (type == Tag.Type.CLOSE) {
            tag.put('/');
        }
        tag.put(name);

        for (int i = 0; i < this.attributeCount; i += 2) {
            tag.put(' ').put(attributes[i]);
            if (attributes[i + 1] != null) {
                tag.put('=').put('"').put(attributes[i + 1]).put('"');
            }
        }

        if (type == Tag.Type.EMPTY) {
            tag.put('/');
        }
        tag.put('>');

        assert tag.remaining() == 0 : "Incorrect buffer length for tag. Please report this to the SiteMesh team.";

        // Output the buffer.
        tag.flip();
        out.append(tag);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomTag)) return false;
        CustomTag customTag = (CustomTag) o;

        return type == customTag.type
                && !(attributes != null ? !Arrays.equals(attributes, customTag.attributes) : customTag.attributes != null)
                && !(name != null ? !name.equals(customTag.name) : customTag.name != null);
    }

    public int hashCode() {
        int result = (attributes != null ? attributes.hashCode() : 0);
        result = 29 * result + (name != null ? name.hashCode() : 0);
        result = 29 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        try {
            StringBuilder out = new StringBuilder(64);
            writeTo(out);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- Standard methods to implement Tag interface ------

    public int getAttributeCount() {
        return attributeCount / 2;
    }

    public int getAttributeIndex(String name, boolean caseSensitive) {
        if (attributes == null) {
            return -1;
        }
        final int len = attributeCount;
        for (int i = 0; i < len; i += 2) {
            final String current = attributes[i];
            if (caseSensitive ? name.equals(current) : name.equalsIgnoreCase(current)) {
                return i / 2;
            }
        }
        return -1;
    }

    public String getAttributeName(int index) {
        return attributes[index * 2];
    }

    public String getAttributeValue(int index) {
        return attributes[index * 2 + 1];
    }

    public String getAttributeValue(String name, boolean caseSensitive) {
        int attributeIndex = getAttributeIndex(name, caseSensitive);
        if (attributeIndex == -1) {
            return null;
        } else {
            return attributes[attributeIndex * 2 + 1];
        }
    }

    public boolean hasAttribute(String name, boolean caseSensitive) {
        return getAttributeIndex(name, caseSensitive) > -1;
    }

    public String getName() {
        return name;
    }

    /**
     * Type of tag.
     */
    public Tag.Type getType() {
        return type;
    }

    // ----------- Additional methods for changing a tag -----------

    /**
     * Change the name of the attribute.
     */
    public void setName(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("CustomTag requires a name");
        } else {
            this.name = name;
        }
    }

    /**
     * Change the type of the tag.
     */
    public void setType(Tag.Type type) {
        if (type == Tag.Type.OPEN || type == Tag.Type.CLOSE || type == Tag.Type.EMPTY) {
            this.type = type;
        } else {
            throw new IllegalArgumentException("CustomTag must be of type Tag.OPEN, Tag.CLOSE or Tag.EMPTY - was " + type);
        }
    }

    private void growAttributes() {
        int newSize = attributes.length == 0 ? 4 : attributes.length * 2;
        String[] newAttributes = new String[newSize];
        System.arraycopy(attributes, 0, newAttributes, 0, attributes.length);
        attributes = newAttributes;
    }

    /**
     * Add a new attribute. This does not check for the existence of an attribute with the same name,
     * thus allowing duplicate attributes.
     *
     * @param name  Name of attribute to change.
     * @param value New value of attribute or null for an HTML style empty attribute.
     * @return Index of new attribute.
     */
    public int addAttribute(String name, String value) {
        if (attributeCount == attributes.length) {
            growAttributes();
        }
        attributes[attributeCount++] = name;
        attributes[attributeCount++] = value;
        return (attributeCount / 2) - 1;
    }

    /**
     * Change the value of an attribute, or add an attribute if it does not already exist.
     *
     * @param name          Name of attribute to change.
     * @param caseSensitive Whether the name should be treated as case sensitive when searching for an existing value.
     * @param value         New value of attribute or null for an HTML style empty attribute.
     */
    public void setAttributeValue(String name, boolean caseSensitive, String value) {
        int attributeIndex = getAttributeIndex(name, caseSensitive);
        if (attributeIndex == -1) {
            addAttribute(name, value);
        } else {
            attributes[attributeIndex * 2 + 1] = value;
        }
    }

    /**
     * Change the name of an existing attribute.
     */
    public void setAttributeName(int attributeIndex, String name) {
        attributes[attributeIndex * 2] = name;
    }

    /**
     * Change the value of an existing attribute. The value may be null for an HTML style empty attribute.
     */
    public void setAttributeValue(int attributeIndex, String value) {
        attributes[(attributeIndex * 2) + 1] = value;
    }

    /**
     * Remove an attribute.
     */
    public void removeAttribute(int attributeIndex) {
        if (attributeIndex > attributeCount / 2) {
            throw new ArrayIndexOutOfBoundsException("Cannot remove attribute at index " + attributeIndex + ", max index is " + attributeCount / 2);
        }
        //shift everything down one and null the last two
        String[] newAttributes = new String[attributes.length - 2];
        System.arraycopy(attributes, 0, newAttributes, 0, attributeIndex * 2);
        int next = (attributeIndex * 2) + 2;
        System.arraycopy(attributes, next, newAttributes, attributeIndex * 2, attributes.length - next);
        attributeCount = attributeCount - 2;
        attributes = newAttributes;
    }

    /**
     * Change the value of an attribute, or add an attribute if it does not already exist.
     *
     * @param name          Name of attribute to remove.
     * @param caseSensitive Whether the name should be treated as case sensitive.
     */
    public void removeAttribute(String name, boolean caseSensitive) {
        int attributeIndex = getAttributeIndex(name, caseSensitive);
        if (attributeIndex == -1) {
            throw new IllegalArgumentException("Attribute " + name + " not found");
        } else {
            removeAttribute(attributeIndex);
        }
    }


}

