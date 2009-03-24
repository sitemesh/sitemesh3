package com.opensymphony.sitemesh;

import java.io.IOException;
import java.io.Writer;

/**
 * Stub {@link Context} implementation, for use in tests.
 *
 * @author Joe Walnes
 */
public class ContextStub implements Context {

    private String requestPath;

    @Override
    public String getRequestPath() {
        return requestPath;
    }

    public ContextStub withRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    @Override
    public boolean applyDecorator(Content content, Writer out) throws IOException {
        throw new UnsupportedOperationException("Not supported by ContextStub");
    }

    @Override
    public boolean applyDecorator(String decoratorName, Content content, Writer out) throws IOException {
        throw new UnsupportedOperationException("Not supported by ContextStub");
    }
}
