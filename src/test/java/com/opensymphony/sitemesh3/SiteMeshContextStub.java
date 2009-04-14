package com.opensymphony.sitemesh3;

import com.opensymphony.sitemesh3.content.ContentProperty;

import java.io.IOException;

/**
 * Stub {@link SiteMeshContext} implementation, for use in tests.
 *
 * @author Joe Walnes
 */
public class SiteMeshContextStub implements SiteMeshContext {

    private String requestPath;
    private ContentProperty contentToMerge;

    @Override
    public String getRequestPath() {
        return requestPath;
    }

    public SiteMeshContextStub withRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    @Override
    public ContentProperty decorate(String decoratorName, ContentProperty content) throws IOException {
        throw new UnsupportedOperationException("Not supported by SiteMeshContextStub");
    }

    @Override
    public ContentProperty getContentToMerge() {
        return contentToMerge;
    }

    public void setContentToMerge(ContentProperty contentToMerge) {
        this.contentToMerge = contentToMerge;
    }
}
