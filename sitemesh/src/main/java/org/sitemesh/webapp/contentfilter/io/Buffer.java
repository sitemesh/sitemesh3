package org.sitemesh.webapp.contentfilter.io;

import javax.servlet.ServletOutputStream;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.CharBuffer;

/**
 * A shared buffer, that can provide an interface as either a {@link PrintWriter}
 * (through {@link #getWriter()}) or {@link ServletOutputStream} (through {@link #getOutputStream()}.
 * <p/>
 * The buffered text can be accessed through {@link #toCharBuffer()}.
 *
 * @author Joe Walnes
 */
public class Buffer {

    private final String encoding;
    private static final CharBuffer EMPTY_BUFFER = CharBuffer.allocate(0);

    private final ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder();
    private PrintWriter exposedWriter;
    private ServletOutputStream exposedStream;

    private boolean usingWriter = false;
    private boolean usingOutputStream = false;

    public Buffer(String encoding) {
        this.encoding = encoding;
    }

    public PrintWriter getWriter() {
        if (usingOutputStream) {
            throw new IllegalStateException("response.getWriter() called after response.getOutputStream()");
        }
        if (exposedWriter == null) {
            exposedWriter = new PrintWriter(new OutputStreamWriter(byteBufferBuilder)) {

                @Override
                public void write(char buf[], int off, int len) {
                    super.write(buf, off, len);
                    super.flush();
                }

                @Override
                public void write(String s, int off, int len) {
                    super.write(s, off, len);
                    super.flush();
                }

                @Override
                public void write(int c) {
                    super.write(c);
                    super.flush();
                }
            };
        }
        usingWriter = true;
        return exposedWriter;
    }

    public ServletOutputStream getOutputStream() {
        if (usingWriter) {
            throw new IllegalStateException("response.getOutputStream() called after response.getWriter()");
        }
        if (exposedStream == null) {
            exposedStream = new ServletOutputStream() {

                @Override
                public void write(int b) {
                    byteBufferBuilder.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    byteBufferBuilder.write(b, off, len);
                }
            };
        }
        usingOutputStream = true;
        return exposedStream;
    }

    public boolean isUsingStream() {
        return usingOutputStream;
    }

    public CharBuffer toCharBuffer() throws IOException {
        if (byteBufferBuilder.size() > 0) {
            // TODO: Avoid allocating intermediate ByteBuffers.
            return TextEncoder.encode(byteBufferBuilder.toByteBuffer(), encoding);
        } else {
            return EMPTY_BUFFER;
        }
    }

    public void resetBuffer() {
        byteBufferBuilder.reset();
    }

    public void reset() {
        usingOutputStream = false;
        usingWriter = false;
        resetBuffer();
    }

    @Override
    public String toString() {
        try {
            return toCharBuffer().toString();
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}