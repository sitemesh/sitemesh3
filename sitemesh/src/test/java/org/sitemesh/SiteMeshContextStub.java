package org.sitemesh;

import org.sitemesh.content.Content;

import java.io.IOException;

/**
 * Stub {@link SiteMeshContext} implementation, for use in tests.
 *
 * @author Joe Walnes
 */
public class SiteMeshContextStub implements SiteMeshContext {

    private String requestPath;
    private Content contentToMerge;

    @Override
    public String getRequestPath() {
        return requestPath;
    }

    public SiteMeshContextStub withRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    @Override
    public Content decorate(String decoratorName, Content content) throws IOException {
        throw new UnsupportedOperationException("Not supported by SiteMeshContextStub");
    }

    @Override
    public Content getContentToMerge() {
        return contentToMerge;
    }

    public void setContentToMerge(Content contentToMerge) {
        this.contentToMerge = contentToMerge;
    }
}
