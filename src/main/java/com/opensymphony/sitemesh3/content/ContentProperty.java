package com.opensymphony.sitemesh3.content;

/**
 * @author Joe Walnes
 */
public interface ContentProperty extends TreeNode<ContentProperty>, ContentChunk {

    /**
     * Returns the original unmodified content.
     */
    ContentChunk getOriginal();

}
