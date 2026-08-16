/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.webapp.contentfilter.io;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * A shared buffer, that can provide an interface as either a {@link PrintWriter}
 * (through {@link #getWriter()}) or {@link ServletOutputStream} (through {@link #getOutputStream()}.
 *
 * <p>The buffered text can be accessed through {@link #toCharBuffer()}.</p>
 *
 * @author Joe Walnes
 */
public class Buffer {

    /**
     * Initial character capacity used when the caller gives no size hint.
     * Large enough that a small page does not repeatedly grow, small enough
     * to be irrelevant for a response that turns out to be tiny.
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 1024;

    private final String encoding;
    private final int initialCapacity;
    private static final CharBuffer EMPTY_BUFFER = CharBuffer.allocate(0);

    private ExposedCharArrayWriter bufferedWriter;
    private ByteBufferBuilder byteBufferBuilder;
    private PrintWriter exposedWriter;
    private ServletOutputStream exposedStream;

    /**
     * A {@link CharArrayWriter} that can hand out its backing array directly,
     * so {@link #toCharBuffer()} can return a view over the accumulated
     * characters instead of copying them.
     */
    private static final class ExposedCharArrayWriter extends CharArrayWriter {

        ExposedCharArrayWriter(int initialSize) {
            super(initialSize);
        }

        CharBuffer toCharBufferView() {
            return CharBuffer.wrap(this.buf, 0, this.count);
        }
    }

    /**
     * Equivalent to {@link #Buffer(String, int)} with
     * {@link #DEFAULT_INITIAL_CAPACITY}.
     *
     * @param encoding character encoding used to decode bytes written to the
     *                 {@link ServletOutputStream} into text.
     */
    public Buffer(String encoding) {
        this(encoding, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * @param encoding character encoding used to decode bytes written to the
     *                 {@link ServletOutputStream} into text.
     * @param initialCapacity initial character capacity of the underlying
     *                        buffer. Sizing this close to the eventual
     *                        response length avoids the repeated
     *                        grow-and-copy cycles that dominate the cost of
     *                        buffering a large page. Values below 1 are
     *                        replaced with {@link #DEFAULT_INITIAL_CAPACITY}.
     */
    public Buffer(String encoding, int initialCapacity) {
        this.encoding = encoding;
        this.initialCapacity = initialCapacity > 0 ? initialCapacity : DEFAULT_INITIAL_CAPACITY;
    }

    /**
     * Expose the buffer as a {@link PrintWriter}. Must not be called after {@link #getOutputStream()}.
     *
     * @return writer that appends to this buffer.
     */
    public PrintWriter getWriter() {
        if (bufferedWriter == null) {
            if (byteBufferBuilder != null) {
                throw new IllegalStateException("response.getWriter() called after response.getOutputStream()");
            }
            bufferedWriter = new ExposedCharArrayWriter(initialCapacity);
            exposedWriter = new PrintWriter(bufferedWriter);
        }
        return exposedWriter;
    }

    /**
     * Expose the buffer as a {@link ServletOutputStream}. Must not be called after {@link #getWriter()}.
     *
     * @return stream that appends to this buffer.
     */
    public ServletOutputStream getOutputStream() {
        if (byteBufferBuilder == null) {
            if (bufferedWriter != null) {
                throw new IllegalStateException("response.getOutputStream() called after response.getWriter()");
            }
            byteBufferBuilder = new ByteBufferBuilder();
            exposedStream = new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) { }

                @Override
                public void write(int b) {
                    byteBufferBuilder.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    byteBufferBuilder.write(b, off, len);
                }

                @Override
                public void write(ByteBuffer buffer) throws IOException {
                    if (buffer.hasArray()) {
                        byteBufferBuilder.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
                        buffer.position(buffer.limit());
                    } else {
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        byteBufferBuilder.write(bytes, 0, bytes.length);
                    }
                }
            };
        }
        return exposedStream;
    }

    /**
     * @return true if content was written via {@link #getOutputStream()} rather than {@link #getWriter()}.
     */
    public boolean isUsingStream() {
        return byteBufferBuilder != null;
    }

    /**
     * Return the buffered content as text, decoding bytes with the buffer's encoding if
     * the {@link ServletOutputStream} was used.
     *
     * <p>When the content was written through {@link #getWriter()}, the
     * returned buffer is a <em>view</em> over the accumulated characters
     * rather than a copy — avoiding a full page-sized array allocation and
     * copy on every buffered response. Callers must therefore treat the
     * result as read-only and must not write to this {@code Buffer} again
     * while still using it; every caller in SiteMesh reads the buffer only
     * once the render that filled it has completed.</p>
     *
     * @return the buffered content (empty if nothing was written).
     * @throws IOException if the byte content cannot be decoded.
     */
    public CharBuffer toCharBuffer() throws IOException {
        if (bufferedWriter != null) {
            return bufferedWriter.toCharBufferView();
        } else if (byteBufferBuilder != null) {
            // TODO: Avoid allocating intermediate ByteBuffers.
            return TextEncoder.encode(byteBufferBuilder.toByteBuffer(), encoding);
        } else {
            return EMPTY_BUFFER;
        }
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