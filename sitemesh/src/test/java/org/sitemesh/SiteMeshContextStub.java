package org.sitemesh;

import java.io.IOException;

import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;

/**
 * Stub {@link SiteMeshContext} implementation, for use in tests.
 *
 * @author Joe Walnes
 */
public class SiteMeshContextStub implements SiteMeshContext {

    private String path;
    private Content contentToMerge;
    private ContentProcessor contentProcessor;

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
    
    public ContentProcessor getContentProcessor() {
        return contentProcessor;
    }
    
    public void setContentProcessor(ContentProcessor contentProcessor) {
        this.contentProcessor = contentProcessor;
    }
}
