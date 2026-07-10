/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * Tag returned by {@link TagTokenizer}. Allows easy access to element name and attributes.
 *
 * This interface supports read-only operations on the tag. To change a tag, create a new {@link CustomTag}.
 *
 * @author Joe Walnes
 */
public interface Tag {

    /**
     * Type of tag.
     */
    public static enum Type {

        /**
         * Opening tag: <code>&lt;blah&gt;</code>
         */
        OPEN,

        /**
         * Closing tag: <code>&lt;/blah&gt;</code>
         */
        CLOSE,

        /**
         * Empty tag: <code>&lt;blah/&gt;</code>
         */
        EMPTY,

        /**
         * Opening conditional comment: <code>&lt;!--[</code>
         */
        OPEN_CONDITIONAL_COMMENT,

        /**
         * Closing conditional comment: <code>&lt;![</code>
         */
        CLOSE_CONDITIONAL_COMMENT
    }

    /**
     * Write out the complete tag in its original form, preserving original formatting.
     *
     * @param out destination to write the tag to
     * @throws IOException if the destination cannot be written to
     */
    void writeTo(Appendable out) throws IOException;

    /**
     * Name of tag (ie. element name).
     *
     * @return the element name of the tag
     */
    String getName();

    /**
     * Type of tag (e.g. open, close, etc).
     *
     * @return the type of the tag
     * @see Tag.Type
     */
    Type getType();

    /**
     * Number of attributes in tag.
     *
     * @return number of attributes in the tag
     */
    int getAttributeCount();

    /**
     * Determine which attribute has the specified name.
     *
     * @param name          name of the attribute to look for
     * @param caseSensitive whether the name should be treated as case sensitive
     * @return index of the attribute, or -1 if not present
     */
    int getAttributeIndex(String name, boolean caseSensitive);

    /**
     * Get name of attribute.
     *
     * @param index index of the attribute
     * @return name of the attribute at the given index
     */
    String getAttributeName(int index);

    /**
     * Get value of an attribute. If this is an empty attribute (i.e. just a name, without a value), null is returned.
     *
     * @param index index of the attribute
     * @return value of the attribute, or null for an empty attribute
     */
    String getAttributeValue(int index);

    /**
     * Get value of an attribute. If this is an empty attribute (i.e. just a name, without a value), null is returned.
     *
     * @param name          name of the attribute to look for
     * @param caseSensitive whether the name should be treated as case sensitive
     * @return value of the attribute, or null if the attribute is empty or not present
     */
    String getAttributeValue(String name, boolean caseSensitive);

    /**
     * Determine if an attribute is present.
     *
     * @param name          name of the attribute to look for
     * @param caseSensitive whether the name should be treated as case sensitive
     * @return true if the tag has an attribute with the given name
     */
    boolean hasAttribute(String name, boolean caseSensitive);

    /**
     * Get the complete tag in its original form, preserving original formatting.
     *
     * <p>This has a slight overhead in that it needs to construct a String. For improved
     * performance, use writeTo() instead.</p>
     *
     * @see #writeTo(Appendable)
     */
    String toString();
}

