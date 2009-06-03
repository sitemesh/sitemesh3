package org.sitemesh;

import org.sitemesh.content.Content;

import java.io.IOException;

/**
 * Stub {@link SiteMeshContext} implementation, for use in tests.
 *
 * @author Joe Walnes
 */
public class SiteMeshContextStub implements SiteMeshContext {

    private String path;
    private Content contentToMerge;

    public String getPath() {
        return path;
    }

    public SiteMeshContextStub withPath(String path) {
        this.path = path;
        return this;
    }

    public Content decorate(String decoratorName, Content content) throws IOException {
        throw new UnsupportedOperationException("Not supported by SiteMeshContextStub");
    }

    public Content getContentToMerge() {
        return contentToMerge;
    }

    public void setContentToMerge(Content contentToMerge) {
        this.contentToMerge = contentToMerge;
    }
}
