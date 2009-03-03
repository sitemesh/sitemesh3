package com.opensymphony.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * Text returned by {@link com.opensymphony.sitemesh.tagprocessor.TagTokenizer}.
 *
 * @author Joe Walnes
 */
public interface Text {

    /**
     * Write out the complete contents of the text block, preserving original formatting.
     */
    void writeTo(Appendable out) throws IOException;

    /**
     * Get the complete contents of the text block, preserving original formatting.
     *
     * This has a slight overhead in that it needs to construct a String. For improved performance, use writeTo() instead.
     *
     * @see #writeTo(Appendable)
     */
    String toString();

}
