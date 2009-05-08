package org.sitemesh.content;

import org.sitemesh.tagprocessor.CharSequenceBuffer;

/**
 * @author Joe Walnes
 */
public interface Content {

    /**
     * The main data of the content - that is, the complete document. This may have been
     * rewritten by the {@link ContentProcessor}.
     */
    ContentChunk getData();

    /**
     * Get a tree of extracted properties, that were captured by the {@link ContentProcessor}.
     */
    ContentProperty getExtractedProperties();

    /**
     * Creates a buffer that will output it contents in {@link #getData()}, but NOT in the
     * extracted properties from {@link #getExtractedProperties()}.
     */
    CharSequenceBuffer createDataOnlyBuffer();

}
