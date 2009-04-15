package com.opensymphony.sitemesh3.content.memory;

import com.opensymphony.sitemesh3.content.Content;
import com.opensymphony.sitemesh3.content.ContentChunk;
import com.opensymphony.sitemesh3.content.ContentProperty;

public class InMemoryContent implements Content {

    private final InMemoryContentProperty rootProperty = new InMemoryContentProperty(this);

    @Override
    public ContentProperty getExtractedProperties() {
        return rootProperty;
    }

    @Override
    public ContentChunk getData() {
        return rootProperty;
    }

}
