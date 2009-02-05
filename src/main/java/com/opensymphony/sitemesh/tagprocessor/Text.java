package com.opensymphony.sitemesh.tagprocessor;

import com.opensymphony.sitemesh.tagprocessor.util.CharArray;

/**
 * Text returned by {@link com.opensymphony.sitemesh.tagprocessor.TagTokenizer}.
 *
 * @author Joe Walnes
 */
public interface Text {

    /**
     * Get the complete contents of the text block, preserving original formatting.
     *
     * This has a slight overhead in that it needs to construct a String. For improved performance, use writeTo() instead.
     *
     * @see #writeTo(CharArray)
     */
    String getContents();

    /**
     * Write out the complete contents of the text block, preserving original formatting.
     */
    void writeTo(CharArray out);

    /**
     * @see #getContents()
     */
    String toString();

}
