package org.sitemesh.content.memory;

import org.sitemesh.content.ContentProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * {@link ContentProperty} implementation that stores properties in memory.
 *
 * @author Joe Walnes
 */
class InMemoryContentProperty extends InMemoryContentChunk implements ContentProperty, Iterable<ContentProperty> {

    private static final Iterator<ContentProperty> EMPTY_ITERATOR = Collections.<ContentProperty>emptySet().iterator();

    private final boolean isRoot;
    private final String name;
    private final InMemoryContentProperty parent;
    private Map<String, ContentProperty> children; // Lazily instantiated.

    public InMemoryContentProperty(InMemoryContent owner) {
        super(owner);
        name = null;
        parent = null;
        isRoot = true;
    }

    protected InMemoryContentProperty(String name, InMemoryContentProperty parent) {
        super(parent.getOwningContent());
        this.name = name;
        this.parent = parent;
        this.isRoot = false;
    }

    public String getName() {
        return name;
    }

    public ContentProperty[] getFullPath() {
        // Determine size of array.
        int size = 0;
        for (InMemoryContentProperty node = this; !node.isRoot; node = node.parent) {
            size++;
        }

        ContentProperty[] result = new ContentProperty[size];

        // Build array.
        int index = 0;
        for (InMemoryContentProperty node = this; !node.isRoot; node = node.parent, index++) {
            result[result.length - 1 - index] = node; // Start from end of array.
        }

        return result;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public boolean hasChild(String childName) {
        return children != null && children.containsKey(childName);
    }

    public ContentProperty getChild(String childName) {
        if (children == null) {
            children = new HashMap<String, ContentProperty>();
        }

        ContentProperty property = children.get(childName);
        if (property == null) {
            property = createChild(childName);
            children.put(childName, property);
        }
        return property;
    }

    protected InMemoryContentProperty createChild(String childName) {
        return new InMemoryContentProperty(childName, this);
    }

    public ContentProperty getParent() {
        return parent;
    }

    public Iterable<ContentProperty> getChildren() {
        return this;
    }

    public Iterator<ContentProperty> iterator() {
        return children == null ? EMPTY_ITERATOR : children.values().iterator();
    }

    public Iterable<ContentProperty> getDescendants() {
        return walk(this, new LinkedList<ContentProperty>());
    }

    private List<ContentProperty> walk(ContentProperty node, List<ContentProperty> result) {
        result.add(node);
        for (ContentProperty child : node.getChildren()) {
            walk(child, result);
        }
        return result;
    }

}
