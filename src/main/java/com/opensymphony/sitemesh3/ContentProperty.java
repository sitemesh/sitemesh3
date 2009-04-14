package com.opensymphony.sitemesh3;

/**
 * @author Joe Walnes
 */
public interface ContentProperty extends TreeNode<ContentProperty>, ContentChunk {

    /**
     * Returns the original unmodified content.
     */
    ContentChunk getOriginal();

}
