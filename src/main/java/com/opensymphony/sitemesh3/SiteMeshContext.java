package com.opensymphony.sitemesh3;

import com.opensymphony.sitemesh3.content.ContentProperty;

import java.io.IOException;

public interface SiteMeshContext {

    String getRequestPath();

    ContentProperty decorate(String decoratorName, ContentProperty content) throws IOException;

    /**
     * The ContentProperty of the document being merged in to the decorator. This is only
     * set within the scope of the {@link #decorate(String, ContentProperty)} method - the
     * rest of the time, this will return null.
     */
    ContentProperty getContentToMerge();

}
