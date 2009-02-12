package com.opensymphony.sitemesh;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * Stub {@link Context} implementation, for use in tests. All data written to it will
 * reside in memory and can be retrieved through {@link #getWrittenData()}.
 *
 * @author Joe Walnes
 */
public class ContextStub implements Context {

    private final StringWriter buffer = new StringWriter();
    private final PrintWriter writer = new PrintWriter(buffer);

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    public String getWrittenData() {
        writer.flush();
        return buffer.toString();
    }

    @Override
    public String toString() {
        return getWrittenData();
    }
}
