/*
 *    Copyright 2009-2023 SiteMesh authors.
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

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;

/**
 * A shared buffer, that can provide an interface as either a {@link PrintWriter}
 * (through {@link #getWriter()}) or {@link ServletOutputStream} (through {@link #getOutputStream()}.
 * <p></p>
 * The buffered text can be accessed through {@link #toCharBuffer()}.
 *
 * @author Joe Walnes
 */
public class Buffer {

    private final String encoding;
    private static final CharBuffer EMPTY_BUFFER = CharBuffer.allocate(0);

    private CharArrayWriter bufferedWriter;
    private ByteBufferBuilder byteBufferBuilder;
    private PrintWriter exposedWriter;
    private ServletOutputStream exposedStream;

    public Buffer(String encoding) {
        this.encoding = encoding;
    }

    public PrintWriter getWriter() {
        if (bufferedWriter == null) {
            if (byteBufferBuilder != null) {
                throw new IllegalStateException("response.getWriter() called after response.getOutputStream()");
            }
            bufferedWriter = new CharArrayWriter(128);
            exposedWriter = new PrintWriter(bufferedWriter);
        }
        return exposedWriter;
    }

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
            };
        }
        return exposedStream;
    }

    public boolean isUsingStream() {
        return byteBufferBuilder != null;
    }

    public CharBuffer toCharBuffer() throws IOException {
        if (bufferedWriter != null) {
            return CharBuffer.wrap(bufferedWriter.toCharArray());
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