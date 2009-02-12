package com.opensymphony.sitemesh;

import java.io.IOException;

/**
 * Stub {@link Content} implementation, for use in tests. Can be set up by calling
 * {@link #addProperty(String, String)}.
 *
 * @author Joe Walnes
 */
public class ContentStub extends BaseContent {

    public ContentStub(String original) throws IOException {
        super(original);
    }

    public ContentStub() throws IOException {
        super("ORIGINAL CONTENT");
    }

    @Override
    protected void processContent(String original) throws IOException {
        // No op.
    }
}
