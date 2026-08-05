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

package org.sitemesh.content.memory;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Verifies the CharBuffer fast path in {@link InMemoryContentChunk#writeValueTo}.
 *
 * The fast path: when the stored value is a {@link CharBuffer} with a backing
 * array and the target {@link Appendable} is a {@link Writer}, the chunk
 * should route to {@link Writer#write(char[], int, int)} directly rather than
 * falling through to {@link Appendable#append(CharSequence)} (which on a
 * {@link java.io.PrintWriter}/{@link Writer} allocates a full String copy).
 */
public class InMemoryContentChunkCharBufferTest extends TestCase {

    /** Counts which underlying write/append methods were invoked. */
    private static class CountingWriter extends Writer {
        final StringWriter delegate = new StringWriter();
        int charArrayWrites;
        int stringWrites;
        int appendCharSequenceCalls;

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            charArrayWrites++;
            delegate.write(cbuf, off, len);
        }

        @Override
        public void write(String str) throws IOException {
            stringWrites++;
            delegate.write(str);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            stringWrites++;
            delegate.write(str, off, len);
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            appendCharSequenceCalls++;
            return super.append(csq);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        String result() {
            return delegate.toString();
        }
    }

    private InMemoryContentChunk newChunk() {
        // InMemoryContentChunk takes a Content; pass null - the owner isn't
        // touched by writeValueTo.
        return new InMemoryContentChunk(null);
    }

    public void testCharBufferWrappingCharArrayUsesFastPath() throws IOException {
        String text = "hello fast path";
        CharBuffer buf = CharBuffer.wrap(text.toCharArray());

        InMemoryContentChunk chunk = newChunk();
        chunk.setValue(buf);

        CountingWriter out = new CountingWriter();
        chunk.writeValueTo(out);

        assertEquals(text, out.result());
        assertEquals("expected Writer.write(char[], int, int) fast path",
                1, out.charArrayWrites);
        assertEquals("must not allocate a String via write(String)",
                0, out.stringWrites);
        assertEquals("must not fall through to append(CharSequence)",
                0, out.appendCharSequenceCalls);
    }

    public void testCharBufferWrappingCharArrayWithRangeRespectsOffsetAndLength() throws IOException {
        char[] data = "xxhello worldyy".toCharArray();
        // offset=2, length=11 -> "hello world"
        CharBuffer buf = CharBuffer.wrap(data, 2, 11);

        InMemoryContentChunk chunk = newChunk();
        chunk.setValue(buf);

        CountingWriter out = new CountingWriter();
        chunk.writeValueTo(out);

        assertEquals("hello world", out.result());
        assertEquals(1, out.charArrayWrites);
        assertEquals(0, out.stringWrites);
        assertEquals(0, out.appendCharSequenceCalls);
    }

    public void testStringBuilderUsesAppendPath() throws IOException {
        String text = "plain string builder";
        StringBuilder sb = new StringBuilder(text);

        InMemoryContentChunk chunk = newChunk();
        chunk.setValue(sb);

        CountingWriter out = new CountingWriter();
        chunk.writeValueTo(out);

        assertEquals(text, out.result());
        assertEquals("StringBuilder must not hit the CharBuffer fast path",
                0, out.charArrayWrites);
        assertEquals("append(CharSequence) must be used for StringBuilder",
                1, out.appendCharSequenceCalls);
    }

    public void testDirectCharBufferFallsThroughToAppendPath() throws IOException {
        // A direct ByteBuffer view yields a CharBuffer with hasArray() == false.
        CharBuffer direct = ByteBuffer.allocateDirect(32).asCharBuffer();
        String text = "direct";
        direct.put(text);
        direct.flip();

        assertFalse("precondition: direct CharBuffer must not be array-backed",
                direct.hasArray());

        InMemoryContentChunk chunk = newChunk();
        chunk.setValue(direct);

        CountingWriter out = new CountingWriter();
        chunk.writeValueTo(out);

        assertEquals(text, out.result());
        assertEquals("hasArray()==false must skip the fast path",
                0, out.charArrayWrites);
        assertEquals("must fall through to append(CharSequence)",
                1, out.appendCharSequenceCalls);
    }

    public void testOutputMatchesBaselineForAllCases() throws IOException {
        String text = "round-trip content";

        // 1. CharBuffer.wrap(char[])
        {
            InMemoryContentChunk chunk = newChunk();
            chunk.setValue(CharBuffer.wrap(text.toCharArray()));
            StringWriter sw = new StringWriter();
            chunk.writeValueTo(sw);
            assertEquals(text, sw.toString());
        }

        // 2. StringBuilder
        {
            InMemoryContentChunk chunk = newChunk();
            chunk.setValue(new StringBuilder(text));
            StringWriter sw = new StringWriter();
            chunk.writeValueTo(sw);
            assertEquals(text, sw.toString());
        }

        // 3. Direct (non-array-backed) CharBuffer
        {
            CharBuffer direct = ByteBuffer.allocateDirect(text.length() * 2).asCharBuffer();
            direct.put(text);
            direct.flip();
            InMemoryContentChunk chunk = newChunk();
            chunk.setValue(direct);
            StringWriter sw = new StringWriter();
            chunk.writeValueTo(sw);
            assertEquals(text, sw.toString());
        }
    }
}
