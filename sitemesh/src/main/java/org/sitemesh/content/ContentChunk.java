package org.sitemesh.content;

import java.io.IOException;

/**
 * A mutable reference to a chunk of content.
 *
 * @author Joe Walnes
 */
public interface ContentChunk {

    /**
     * Returns whether this property has a value set.
     */
    boolean hasValue();

    /**
     * Returns the value of this property as a String. If not set, will return null.
     */
    String getValue();

    /**
     * Returns the value of this property as a String. If not set, will return "".
     */
    String getNonNullValue();

    /**
     * Write the value of this property to {@code out}. This is typically more efficient
     * than calling {@link #getValue()} for large properties as it does not require copying
     * into an intermediate String instance. If no value is set, nothing will be written.
     */
    void writeValueTo(Appendable out) throws IOException;

    /**
     * Sets the value. May be null.
     */
    void setValue(CharSequence value);

    /**
     * Returns the Content that this chunk belongs to.
     */
    Content getOwningContent();

}
