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
     */
    void writeTo(Appendable out) throws IOException;

    /**
     * Name of tag (ie. element name).
     */
    String getName();

    /**
     * Type of tag (e.g. open, close, etc).
     *
     * @see Tag.Type
     */
    Type getType();

    /**
     * Number of attributes in tag.
     */
    int getAttributeCount();

    /**
     * Determine which attribute has the specified name.
     */
    int getAttributeIndex(String name, boolean caseSensitive);

    /**
     * Get name of attribute.
     */
    String getAttributeName(int index);

    /**
     * Get value of an attribute. If this is an empty attribute (i.e. just a name, without a value), null is returned.
     */
    String getAttributeValue(int index);

    /**
     * Get value of an attribute. If this is an empty attribute (i.e. just a name, without a value), null is returned.
     */
    String getAttributeValue(String name, boolean caseSensitive);

    /**
     * Determine if an attribute is present.
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

