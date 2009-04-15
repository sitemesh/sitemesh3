package com.opensymphony.sitemesh3.content;

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

}
