package com.opensymphony.sitemesh3;

import java.io.IOException;

public interface SiteMeshContext {

    String getRequestPath();

    Content decorate(String decoratorName, Content content) throws IOException;

    /**
     * The Content of the document being merged in to the decorator. This is only
     * set within the scope of the {@link #decorate(String, Content)} method - the
     * rest of the time, this will return null.
     */
    Content getContentToMerge();

}
