package com.opensymphony.sitemesh3;

import java.io.IOException;

/**
 * @author Joe Walnes
 */
public interface Content {

    ValueHolder getOriginal();

    ValueHolder getProcessed();

    Property getRoot();

    Property getProperty(String name);

    interface Property extends ValueHolder, TreeNode<Property> {
    }

    interface ValueHolder {

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

    }

    interface TreeNode<T extends TreeNode> {

        /**
         * Get the name of this node, in relation to the parent.
         */
        String getName();

        /**
         * Get the full path of this node, from the root node.
         */
        T[] getFullPath();

        /**
         * Returns the parent node. If this is the root node, null will be returned.
         */
        T getParent();

        /**
         * Returns whether this node has any child nodes.
         */
        boolean hasChildren();

        /**
         * Returns whether the child node exists.
         */
        boolean hasChild(String name);

        /**
         * Get child by name, in relation to this node.
         *
         * <p>If the child does not already exist, a new one shall be automatically created.
         * If this is not the preferred behavior, you should use {@link #hasChild(String)} first.</p>
         */
        T getChild(String name);

        /**
         * Get immediate children of this node.
         *
         * <p>This does not include grandchildren (use {@link #getDescendants()} instead).</p>
         */
        Iterable<T> getChildren();

        /**
         * Get all descendants of this node.
         *
         * <p>This includes grandchildren, great-grandchildren, etc (use {@link #getChildren()} if you just
         * want the immediate children).</p>
         */
        Iterable<T> getDescendants();

    }

}
