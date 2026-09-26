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

package org.sitemesh.tagprocessor.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class CharSequencesTest extends TestCase {

    /** Heap buffers whose content starts at a non-zero array offset and position. */
    private static CharBuffer offsetBuffer() {
        CharBuffer cb = CharBuffer.wrap("xxHello, wörld ✓yy".toCharArray(), 1, 16).slice();
        cb.position(1);
        return cb;
    }

    private static CharSequence[] samples() {
        return new CharSequence[] {
                CharBuffer.wrap("plain"),
                offsetBuffer(),
                CharBuffer.wrap("read-only").asReadOnlyBuffer(),          // no accessible array
                ByteBuffer.allocateDirect(8).asCharBuffer().put("dir").flip(), // no backing array
                CharBuffer.allocate(0),
                "a String",
                new StringBuilder("a StringBuilder"),
                null,
        };
    }

    public void testWritesSameAsWriterAppend() throws IOException {
        for (CharSequence sample : samples()) {
            StringWriter expected = new StringWriter();
            expected.append(sample);
            StringWriter actual = new StringWriter();
            CharSequences.appendTo(actual, sample);
            assertEquals(String.valueOf(sample), expected.toString(), actual.toString());
        }
    }

    public void testWritesSameToOtherAppendables() throws IOException {
        for (CharSequence sample : samples()) {
            StringBuilder expected = new StringBuilder().append(sample);
            StringBuilder actual = new StringBuilder();
            CharSequences.appendTo(actual, sample);
            assertEquals(String.valueOf(sample), expected.toString(), actual.toString());
        }
    }

    public void testLeavesBufferPositionUnchanged() throws IOException {
        CharBuffer buffer = offsetBuffer();
        int position = buffer.position();
        CharSequences.appendTo(new StringWriter(), buffer);
        assertEquals(position, buffer.position());
    }

    public void testWritesArrayBackedBufferWithoutStringCopy() throws IOException {
        // Writer.append(CharSequence) goes through write(String); the fast path must not.
        final StringBuilder written = new StringBuilder();
        Writer writer = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
                written.append(cbuf, off, len);
            }

            @Override
            public void write(String str, int off, int len) {
                fail("expected chars to be written from the backing array, not via a String");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        CharSequences.appendTo(writer, offsetBuffer());
        assertEquals(offsetBuffer().toString(), written.toString());
    }
}
