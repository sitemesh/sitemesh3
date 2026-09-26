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

import junit.framework.TestCase;

import jakarta.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Random;

/**
 * Pins the behaviour of {@link Buffer}'s byte path: content written through
 * {@link Buffer#getOutputStream()} and decoded by {@link Buffer#toCharBuffer()}.
 *
 * <p>Every expectation here was captured from the original implementation,
 * which gathered all written bytes into one contiguous buffer and decoded it
 * with a single {@link TextEncoder#encode(ByteBuffer, String)} call. Where a
 * test compares against {@link #decodedContiguously(byte[], String)} it is
 * asserting exactly that equivalence, however the bytes were written and
 * wherever they fall relative to the buffer's internal block boundaries.</p>
 */
public class BufferStreamTest extends TestCase {

    /** Size of the blocks {@link Buffer} stores stream content in (ByteBufferBuilder's default). */
    private static final int BLOCK = 8192;

    private static final String TWO_BYTE = "é";          // C3 A9
    private static final String THREE_BYTE = "日";       // E6 97 A5
    private static final String FOUR_BYTE = "🚀"; // F0 9F 9A 80 (rocket)

    // ------------------------------------------------------------------
    // Empty buffers
    // ------------------------------------------------------------------

    public void testUnusedBufferIsEmptyAndNeverConsultsTheEncoding() throws IOException {
        Buffer buffer = new Buffer("no-such-charset");

        CharBuffer result = buffer.toCharBuffer();

        assertEquals(0, result.position());
        assertEquals(0, result.limit());
        assertEquals("", buffer.toString());
        assertFalse(buffer.isUsingStream());
    }

    public void testOpenedStreamWithNothingWrittenDecodesToAnEmptyArrayBackedBuffer() throws IOException {
        Buffer buffer = new Buffer("UTF-8");
        buffer.getOutputStream();

        CharBuffer result = buffer.toCharBuffer();

        assertTrue(buffer.isUsingStream());
        assertEquals(0, result.position());
        assertEquals(0, result.limit());
        assertEquals(0, result.capacity());
        assertTrue(result.hasArray());
        assertFalse(result.isReadOnly());
    }

    public void testOpenedStreamWithNothingWrittenStillRejectsAnUnsupportedEncoding() {
        Buffer buffer = new Buffer("no-such-charset");
        buffer.getOutputStream();

        try {
            buffer.toCharBuffer();
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Unsupported encoding no-such-charset", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Shape of the returned CharBuffer
    // ------------------------------------------------------------------

    public void testReturnsAFreshWritableHeapBufferSizedByMaxCharsPerByte() throws IOException {
        byte[] bytes = ("ab" + THREE_BYTE + FOUR_BYTE).getBytes(StandardCharsets.UTF_8); // 2 + 3 + 4 bytes

        CharBuffer result = write(bytes, "UTF-8");

        assertEquals(0, result.position());
        assertEquals(5, result.limit()); // a, b, 日, and the rocket's surrogate pair
        assertEquals(9, result.capacity()); // UTF-8 maxCharsPerByte (1.0) * 9 bytes
        assertTrue(result.hasArray());
        assertEquals(0, result.arrayOffset());
        assertFalse(result.isReadOnly());
        assertEquals("ab" + THREE_BYTE + FOUR_BYTE, result.toString());
    }

    public void testCapacitySpansAllBlocksForMultiBlockContent() throws IOException {
        byte[] bytes = repeat("x" + THREE_BYTE, 3 * BLOCK + 17); // not a multiple of the 4-byte unit

        CharBuffer result = write(bytes, "UTF-8");

        assertEquals(0, result.position());
        assertEquals(bytes.length, result.capacity());
        assertEquals(decodedContiguously(bytes, "UTF-8"), result.toString());
    }

    public void testToCharBufferTwiceReturnsIndependentBuffersWithEqualContent() throws IOException {
        Buffer buffer = new Buffer("UTF-8");
        buffer.getOutputStream().write("hello".getBytes(StandardCharsets.UTF_8));

        CharBuffer first = buffer.toCharBuffer();
        CharBuffer second = buffer.toCharBuffer();

        assertNotSame(first, second);
        assertNotSame(first.array(), second.array());
        assertEquals(first, second);

        // Consuming or overwriting one must not affect the other.
        first.get();
        first.put(0, 'J');
        assertEquals(0, second.position());
        assertEquals("hello", second.toString());
        assertEquals("hello", buffer.toCharBuffer().toString());
    }

    public void testToCharBufferReflectsWritesMadeAfterAnEarlierCall() throws IOException {
        Buffer buffer = new Buffer("UTF-8");
        ServletOutputStream out = buffer.getOutputStream();
        out.write("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("abc", buffer.toCharBuffer().toString());

        out.write(repeat("d", BLOCK));
        assertEquals("abc" + "d".repeat(BLOCK), buffer.toCharBuffer().toString());
    }

    // ------------------------------------------------------------------
    // The three write methods
    // ------------------------------------------------------------------

    public void testWriteIntAppendsTheLowEightBits() throws IOException {
        Buffer buffer = new Buffer("ISO-8859-1");
        ServletOutputStream out = buffer.getOutputStream();
        out.write('A');
        out.write(0x100 + 'B'); // high bits ignored
        out.write(-1);          // 0xFF

        assertEquals("ABÿ", buffer.toCharBuffer().toString());
    }

    public void testWriteByteArrayRangeAppendsOnlyThatRange() throws IOException {
        Buffer buffer = new Buffer("UTF-8");
        ServletOutputStream out = buffer.getOutputStream();
        out.write("0123456789".getBytes(StandardCharsets.UTF_8), 3, 4);
        out.write("xyz".getBytes(StandardCharsets.UTF_8));
        out.write(new byte[5], 2, 0);

        assertEquals("3456xyz", buffer.toCharBuffer().toString());
    }

    public void testWriteHeapByteBufferHonoursOffsetAndPositionAndConsumesIt() throws IOException {
        byte[] backing = "__0123456789__".getBytes(StandardCharsets.UTF_8);
        ByteBuffer slice = ByteBuffer.wrap(backing, 2, 10).slice(); // arrayOffset 2
        slice.position(3).limit(8);                                  // "34567"

        Buffer buffer = new Buffer("UTF-8");
        writeByteBuffer(buffer.getOutputStream(), slice);

        assertEquals(slice.limit(), slice.position());
        assertEquals("34567", buffer.toCharBuffer().toString());
    }

    public void testWriteDirectByteBufferAppendsItsRemainingBytesAndConsumesIt() throws IOException {
        ByteBuffer direct = ByteBuffer.allocateDirect(16);
        direct.put("..direct..".getBytes(StandardCharsets.UTF_8)).flip();
        direct.position(2).limit(8);

        Buffer buffer = new Buffer("UTF-8");
        writeByteBuffer(buffer.getOutputStream(), direct);

        assertEquals(8, direct.position());
        assertEquals(8, direct.limit());
        assertEquals("direct", buffer.toCharBuffer().toString());
    }

    public void testWriteReadOnlyHeapByteBufferAppendsItsRemainingBytesAndConsumesIt() throws IOException {
        ByteBuffer readOnly = ByteBuffer.wrap("read-only".getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
        assertFalse(readOnly.hasArray());

        Buffer buffer = new Buffer("UTF-8");
        writeByteBuffer(buffer.getOutputStream(), readOnly);

        assertFalse(readOnly.hasRemaining());
        assertEquals("read-only", buffer.toCharBuffer().toString());
    }

    public void testWriteEmptyByteBuffersAppendsNothing() throws IOException {
        Buffer buffer = new Buffer("UTF-8");
        ServletOutputStream out = buffer.getOutputStream();
        writeByteBuffer(out, ByteBuffer.allocate(0));
        writeByteBuffer(out, ByteBuffer.allocateDirect(0));
        writeByteBuffer(out, ByteBuffer.allocateDirect(4).position(4));

        assertEquals("", buffer.toCharBuffer().toString());
    }

    public void testLargeDirectByteBufferSpanningSeveralBlocks() throws IOException {
        byte[] bytes = repeat("<p>" + FOUR_BYTE + TWO_BYTE + THREE_BYTE + "</p>", 5 * BLOCK + 3);
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length + 10);
        direct.position(5);
        direct.put(bytes);
        direct.flip().position(5);

        Buffer buffer = new Buffer("UTF-8");
        ServletOutputStream out = buffer.getOutputStream();
        out.write('>');
        writeByteBuffer(out, direct);

        assertFalse(direct.hasRemaining());
        assertEquals(">" + decodedContiguously(bytes, "UTF-8"), buffer.toCharBuffer().toString());
    }

    public void testInterleavedWriteMethodsAppendInOrder() throws IOException {
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        Buffer buffer = new Buffer("UTF-8");
        ServletOutputStream out = buffer.getOutputStream();
        Random random = new Random(42);
        for (int i = 0; i < 2000; i++) {
            byte[] chunk = ("<" + i + TWO_BYTE + THREE_BYTE + FOUR_BYTE + ">").getBytes(StandardCharsets.UTF_8);
            expected.write(chunk, 0, chunk.length);
            switch (random.nextInt(4)) {
                case 0:
                    for (byte b : chunk) {
                        out.write(b);
                    }
                    break;
                case 1:
                    out.write(chunk, 0, chunk.length);
                    break;
                case 2:
                    writeByteBuffer(out, ByteBuffer.wrap(chunk));
                    break;
                default:
                    ByteBuffer direct = ByteBuffer.allocateDirect(chunk.length);
                    direct.put(chunk).flip();
                    writeByteBuffer(out, direct);
            }
        }

        assertEquals(new String(expected.toByteArray(), StandardCharsets.UTF_8), buffer.toCharBuffer().toString());
    }

    // ------------------------------------------------------------------
    // Argument checking on write
    // ------------------------------------------------------------------

    public void testWriteNullArrayThrowsNullPointerException() throws IOException {
        ServletOutputStream out = new Buffer("UTF-8").getOutputStream();
        try {
            out.write(null, 0, 0);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    public void testWriteNullByteBufferThrowsNullPointerException() throws IOException {
        ServletOutputStream out = new Buffer("UTF-8").getOutputStream();
        try {
            writeByteBuffer(out, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    public void testWriteInvalidRangesThrowsIndexOutOfBoundsAndWritesNothing() throws IOException {
        byte[] data = "0123456789".getBytes(StandardCharsets.UTF_8);
        int[][] ranges = {{-1, 2}, {0, -1}, {0, 11}, {5, 6}, {11, 0}};
        for (int blockOffset : new int[]{0, BLOCK - 3}) {
            Buffer buffer = new Buffer("UTF-8");
            ServletOutputStream out = buffer.getOutputStream();
            out.write(repeat("p", blockOffset));
            for (int[] range : ranges) {
                try {
                    out.write(data, range[0], range[1]);
                    fail("Expected IndexOutOfBoundsException for " + Arrays.toString(range));
                } catch (IndexOutOfBoundsException expected) {
                    assertEquals(IndexOutOfBoundsException.class, expected.getClass());
                }
            }
            assertEquals("p".repeat(blockOffset), buffer.toCharBuffer().toString());
        }
    }

    public void testWriteWhoseEndOverflowsIntFailsAfterWritingTheBytesThatExist() throws IOException {
        // offset + length overflows, slipping past the range check. Historically
        // the bytes up to the end of the array were appended one at a time before
        // the copy ran off the end of the array.
        Buffer buffer = new Buffer("UTF-8");
        ServletOutputStream out = buffer.getOutputStream();
        try {
            out.write("0123456789".getBytes(StandardCharsets.UTF_8), 5, Integer.MAX_VALUE);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // expected
        }
        assertEquals("56789", buffer.toCharBuffer().toString());
        out.write('!');
        assertEquals("56789!", buffer.toCharBuffer().toString());
    }

    public void testWriteWithAnOffsetThatOverflowsIntWritesNothing() throws IOException {
        // offset + length wraps negative and slips past the range check; the copy
        // then fails before appending anything, whichever branch it takes.
        for (int blockOffset : new int[]{0, BLOCK - 1}) {
            Buffer buffer = new Buffer("UTF-8");
            ServletOutputStream out = buffer.getOutputStream();
            out.write(repeat("p", blockOffset));
            try {
                out.write("0123456789".getBytes(StandardCharsets.UTF_8), Integer.MAX_VALUE, 1);
                fail("Expected ArrayIndexOutOfBoundsException");
            } catch (ArrayIndexOutOfBoundsException expected) {
                // expected
            }
            assertEquals("p".repeat(blockOffset), buffer.toCharBuffer().toString());
        }
    }

    public void testWriteWhoseEndOverflowsIntAfterEarlierContentWritesNothing() throws IOException {
        // Here index + length overflows too, so the historical code took its bulk
        // copy branch, which fails before copying anything.
        Buffer buffer = new Buffer("UTF-8");
        ServletOutputStream out = buffer.getOutputStream();
        out.write("abc".getBytes(StandardCharsets.UTF_8));
        try {
            out.write("0123456789".getBytes(StandardCharsets.UTF_8), 5, Integer.MAX_VALUE);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // expected
        }
        assertEquals("abc", buffer.toCharBuffer().toString());
    }

    public void testWriterAndStreamAreMutuallyExclusive() {
        Buffer streamFirst = new Buffer("UTF-8");
        streamFirst.getOutputStream();
        try {
            streamFirst.getWriter();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("response.getWriter() called after response.getOutputStream()", e.getMessage());
        }

        Buffer writerFirst = new Buffer("UTF-8");
        writerFirst.getWriter();
        try {
            writerFirst.getOutputStream();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("response.getOutputStream() called after response.getWriter()", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Multibyte UTF-8 split across writes and across internal blocks
    // ------------------------------------------------------------------

    public void testUtf8SequencesSplitAcrossSeparateWrites() throws IOException {
        String text = "a" + TWO_BYTE + "b" + THREE_BYTE + "c" + FOUR_BYTE + "d" + TWO_BYTE + THREE_BYTE + FOUR_BYTE;
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        for (int chunk = 1; chunk <= bytes.length; chunk++) {
            Buffer buffer = new Buffer("UTF-8");
            ServletOutputStream out = buffer.getOutputStream();
            for (int off = 0; off < bytes.length; off += chunk) {
                out.write(bytes, off, Math.min(chunk, bytes.length - off));
            }
            assertEquals("chunk=" + chunk, text, buffer.toCharBuffer().toString());
        }

        Buffer byteByByte = new Buffer("UTF-8");
        for (byte b : bytes) {
            byteByByte.getOutputStream().write(b);
        }
        assertEquals(text, byteByByte.toCharBuffer().toString());
    }

    public void testUtf8SequenceSplitExactlyAtTheInternalBlockBoundary() throws IOException {
        for (String character : new String[]{TWO_BYTE, THREE_BYTE, FOUR_BYTE}) {
            int width = character.getBytes(StandardCharsets.UTF_8).length;
            // bytesBefore = how many of the character's bytes land in the first block.
            for (int bytesBefore = 0; bytesBefore <= width; bytesBefore++) {
                String text = "a".repeat(BLOCK - bytesBefore) + character + "z".repeat(10);
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                String label = "width=" + width + " bytesBefore=" + bytesBefore;

                assertEquals(label + " single write", text, write(bytes, "UTF-8").toString());

                Buffer split = new Buffer("UTF-8");
                ServletOutputStream out = split.getOutputStream();
                out.write(bytes, 0, BLOCK);
                out.write(bytes, BLOCK, bytes.length - BLOCK);
                assertEquals(label + " split at boundary", text, split.toCharBuffer().toString());
            }
        }
    }

    public void testMultibyteContentSpanningManyBlocksInEveryWritePattern() throws IOException {
        String unit = "<li class=\"x\">" + TWO_BYTE + THREE_BYTE + FOUR_BYTE + "Ж</li>\n";
        String text = unit.repeat(3000); // ~96 KB, 12 blocks, units misaligned with blocks
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        for (int chunk : new int[]{1, 7, 128, 4096, BLOCK - 1, BLOCK, BLOCK + 1, bytes.length}) {
            Buffer buffer = new Buffer("UTF-8");
            ServletOutputStream out = buffer.getOutputStream();
            for (int off = 0; off < bytes.length; off += chunk) {
                out.write(bytes, off, Math.min(chunk, bytes.length - off));
            }
            CharBuffer result = buffer.toCharBuffer();
            assertEquals("chunk=" + chunk, text, result.toString());
            assertEquals(0, result.position());
            assertEquals(text.length(), result.limit());
            assertEquals(bytes.length, result.capacity());
        }
    }

    // ------------------------------------------------------------------
    // Malformed and truncated input: replaced, never an error
    // ------------------------------------------------------------------

    public void testMalformedUtf8IsReplacedAsTheDecoderAlwaysHas() throws IOException {
        assertDecodes("�", "UTF-8", 0x80);                               // lone continuation byte
        assertDecodes("�", "UTF-8", 0xFF);                               // never valid
        assertDecodes("��", "UTF-8", 0xC0, 0xAF);                   // overlong '/'
        assertDecodes("�", "UTF-8", 0xED, 0xA0, 0x80);                   // encoded surrogate
        assertDecodes("��A", "UTF-8", 0xE0, 0x80, 0x41);            // overlong prefix
        assertDecodes("�A", "UTF-8", 0xE3, 0x81, 0x41);                  // interrupted sequence
        assertDecodes("�A", "UTF-8", 0xF0, 0x9F, 0x9A, 0x41);
        assertDecodes("����", "UTF-8", 0xF4, 0x90, 0x80, 0x80); // above U+10FFFF
    }

    public void testTruncatedUtf8AtTheEndIsReplacedOnce() throws IOException {
        assertDecodes("a�", "UTF-8", 0x61, 0xC3);
        assertDecodes("�", "UTF-8", 0xE3, 0x81);
        assertDecodes("�", "UTF-8", 0xF0, 0x9F, 0x9A);
    }

    public void testMalformedAndTruncatedUtf8AtTheInternalBlockBoundary() throws IOException {
        int[][] sequences = {
                {0xE3, 0x81, 0x41}, {0xF0, 0x9F, 0x9A, 0x41}, {0xE0, 0x80, 0x41}, {0xC0, 0xAF},
                {0xED, 0xA0, 0x80}, {0x80, 0x80}, {0xC3}, {0xE3, 0x81}, {0xF0, 0x9F, 0x9A}
        };
        for (int[] sequence : sequences) {
            for (int bytesBefore = 0; bytesBefore <= sequence.length; bytesBefore++) {
                for (boolean atEnd : new boolean[]{false, true}) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bytes.write(repeat("a", BLOCK - bytesBefore), 0, BLOCK - bytesBefore);
                    for (int b : sequence) {
                        bytes.write(b);
                    }
                    if (!atEnd) {
                        bytes.write('z');
                    }
                    byte[] data = bytes.toByteArray();
                    String label = Arrays.toString(sequence) + " bytesBefore=" + bytesBefore + " atEnd=" + atEnd;
                    String expected = decodedContiguously(data, "UTF-8");
                    assertTrue(label, expected.contains("�"));
                    assertEquals(label, expected, write(data, "UTF-8").toString());
                    assertEquals(label, expected, writeInChunks(data, "UTF-8", 1).toString());
                }
            }
        }
    }

    public void testTruncatedUtf8AtTheEndOfMultiBlockContent() throws IOException {
        byte[] body = repeat("<p>" + THREE_BYTE + "</p>", 2 * BLOCK + 5);
        byte[] data = Arrays.copyOf(body, body.length + 2);
        data[body.length] = (byte) 0xE6;
        data[body.length + 1] = (byte) 0x97;

        String result = write(data, "UTF-8").toString();

        assertEquals(decodedContiguously(body, "UTF-8") + "�", result);
    }

    public void testRandomBytesAroundTheBlockBoundaryDecodeAsIfContiguous() throws IOException {
        // Fuzz: arbitrary (mostly invalid) byte runs straddling the boundary, in
        // charsets with multi-byte, stateful and BOM-sensitive decoders.
        String[] encodings = {"UTF-8", "UTF-16", "UTF-16LE", "Shift_JIS", "EUC-JP", "GB18030", "Big5",
                "ISO-2022-JP", "windows-1252", "US-ASCII"};
        byte[] interesting = {0x00, 0x1B, 0x24, 0x28, 0x41, 0x42, 0x4A, 0x7F, (byte) 0x80, (byte) 0x81,
                (byte) 0x8E, (byte) 0x9F, (byte) 0xA0, (byte) 0xA1, (byte) 0xBF, (byte) 0xC0, (byte) 0xC3,
                (byte) 0xD8, (byte) 0xDC, (byte) 0xE0, (byte) 0xE3, (byte) 0xED, (byte) 0xEF, (byte) 0xF0,
                (byte) 0xF4, (byte) 0xFE, (byte) 0xFF};
        Random random = new Random(20260925);
        for (String encoding : encodings) {
            if (!Charset.isSupported(encoding)) {
                continue;
            }
            for (int round = 0; round < 150; round++) {
                int prefix = BLOCK - 1 - random.nextInt(12);
                byte[] data = new byte[prefix + 24];
                Arrays.fill(data, 0, prefix, (byte) 'a');
                for (int i = prefix; i < data.length; i++) {
                    data[i] = random.nextBoolean()
                            ? interesting[random.nextInt(interesting.length)]
                            : (byte) random.nextInt(256);
                }
                String label = encoding + " round=" + round;
                String expected = decodedContiguously(data, encoding);
                assertEquals(label, expected, write(data, encoding).toString());
                assertEquals(label, expected, writeInChunks(data, encoding, 1 + random.nextInt(9)).toString());
            }
        }
    }

    // ------------------------------------------------------------------
    // Other charsets
    // ------------------------------------------------------------------

    public void testIso88591DecodesEveryByteValueIncludingAcrossBlocks() throws IOException {
        byte[] all = new byte[256];
        for (int i = 0; i < 256; i++) {
            all[i] = (byte) i;
        }
        StringBuilder expected = new StringBuilder();
        for (char c = 0; c < 256; c++) {
            expected.append(c);
        }
        assertEquals(expected.toString(), write(all, "ISO-8859-1").toString());

        byte[] many = new byte[256 * 70]; // > 2 blocks
        for (int i = 0; i < many.length; i++) {
            many[i] = (byte) i;
        }
        assertEquals(expected.toString().repeat(70), writeInChunks(many, "ISO-8859-1", 4096).toString());
    }

    public void testUnmappableBytesAreReplaced() throws IOException {
        assertDecodes("A�", "US-ASCII", 0x41, 0xE9);
        assertDecodes("A�", "windows-1252", 0x41, 0x81);
    }

    public void testUtf16SurrogatePairAndBomAcrossTheBlockBoundary() throws IOException {
        // BOM (2 bytes) + 4094 BMP chars puts the rocket's 4 bytes at 8190..8193,
        // splitting its surrogate pair across the boundary; also one char either side.
        for (int bmpChars = 4092; bmpChars <= 4094; bmpChars++) {
            String text = "x".repeat(bmpChars) + FOUR_BYTE + "y";
            byte[] bytes = text.getBytes(Charset.forName("UTF-16")); // big-endian with BOM
            assertEquals(text, write(bytes, "UTF-16").toString());
            assertEquals(text, writeInChunks(bytes, "UTF-16", 3).toString());
        }
    }

    public void testUtf16TrailingOddByteAndLoneSurrogateAreReplaced() throws IOException {
        assertDecodes("A�", "UTF-16BE", 0x00, 0x41, 0x00);
        assertDecodes("�", "UTF-16BE", 0xD8, 0x3D, 0x00, 0x41);
        assertDecodes("A", "UTF-16", 0xFE, 0xFF, 0x00, 0x41);
    }

    public void testDoubleByteAndStatefulCharsetsAcrossTheBlockBoundary() throws IOException {
        String japanese = "日本語のテキスト、カタカナ、ひらがな。";
        for (String encoding : new String[]{"Shift_JIS", "EUC-JP", "ISO-2022-JP", "GB18030"}) {
            if (!Charset.isSupported(encoding)) {
                continue;
            }
            for (int shift = 0; shift < 7; shift++) {
                String text = "a".repeat(BLOCK - 5 - shift) + japanese.repeat(3) + "end";
                byte[] bytes = text.getBytes(Charset.forName(encoding));
                assertEquals(encoding + " shift=" + shift, text, write(bytes, encoding).toString());
                assertEquals(encoding + " shift=" + shift, text, writeInChunks(bytes, encoding, 5).toString());
            }
        }
    }

    // ------------------------------------------------------------------
    // Encoding names
    // ------------------------------------------------------------------

    public void testNullEncodingDecodesWithTheFileEncoding() throws IOException {
        Charset fileEncoding = Charset.forName(System.getProperty("file.encoding"));
        String text = "plain ascii and more";
        if (fileEncoding.newEncoder().canEncode("héllo")) {
            text += " héllo";
        }
        byte[] bytes = text.getBytes(fileEncoding);

        assertEquals(text, write(bytes, null).toString());
    }

    public void testEncodingNamesAreResolvedCaseInsensitivelyAndByAlias() throws IOException {
        byte[] utf8 = ("x" + TWO_BYTE).getBytes(StandardCharsets.UTF_8);
        for (String name : new String[]{"UTF-8", "utf-8", "Utf-8", "UTF8", "utf8"}) {
            assertEquals(name, "x" + TWO_BYTE, write(utf8, name).toString());
        }
        byte[] latin1 = {0x78, (byte) 0xE9};
        for (String name : new String[]{"ISO-8859-1", "iso-8859-1", "latin1", "ISO8859_1"}) {
            assertEquals(name, "x" + TWO_BYTE, write(latin1, name).toString());
        }
    }

    public void testUnsupportedAndIllegalEncodingNamesThrowIOException() {
        assertRejected("no-such-charset", UnsupportedCharsetException.class);
        assertRejected("x-unknown-thing", UnsupportedCharsetException.class);
        assertRejected("bad name!", IllegalCharsetNameException.class);
        assertRejected("", IllegalCharsetNameException.class);
    }

    public void testToStringReportsTheDecodingFailureMessage() {
        Buffer buffer = new Buffer("no-such-charset");
        buffer.getOutputStream();

        assertEquals("Unsupported encoding no-such-charset", buffer.toString());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void assertRejected(String encoding, Class<? extends Exception> cause) {
        Buffer buffer = new Buffer(encoding);
        try {
            buffer.getOutputStream().write('A');
            buffer.toCharBuffer();
            fail("Expected IOException for '" + encoding + "'");
        } catch (IOException e) {
            assertEquals("Unsupported encoding " + encoding, e.getMessage());
            assertEquals(cause, e.getCause().getClass());
        }
    }

    private static void assertDecodes(String expected, String encoding, int... unsignedBytes) throws IOException {
        byte[] bytes = new byte[unsignedBytes.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) unsignedBytes[i];
        }
        CharBuffer result = write(bytes, encoding);
        assertEquals(expected, result.toString());
        assertEquals(0, result.position());
        assertEquals(expected.length(), result.limit());
        assertEquals(expected, writeInChunks(bytes, encoding, 1).toString());
    }

    /**
     * Calls the stream's Servlet 6.1 {@code write(ByteBuffer)} override. The
     * test classpath carries the Servlet 6.0 API (via Jetty ee10), whose
     * {@link ServletOutputStream} does not declare the method yet, so it is
     * invoked reflectively; the override itself is compiled against 6.1.
     */
    private static void writeByteBuffer(ServletOutputStream out, ByteBuffer buffer) throws IOException {
        Method write;
        try {
            write = out.getClass().getMethod("write", ByteBuffer.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Buffer's stream no longer overrides write(ByteBuffer)", e);
        }
        try {
            write.invoke(out, buffer);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new AssertionError(cause);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /** One {@code write(byte[], int, int)} call, then decode. */
    private static CharBuffer write(byte[] bytes, String encoding) throws IOException {
        Buffer buffer = new Buffer(encoding);
        buffer.getOutputStream().write(bytes, 0, bytes.length);
        return buffer.toCharBuffer();
    }

    private static CharBuffer writeInChunks(byte[] bytes, String encoding, int chunk) throws IOException {
        Buffer buffer = new Buffer(encoding);
        ServletOutputStream out = buffer.getOutputStream();
        for (int off = 0; off < bytes.length; off += chunk) {
            out.write(bytes, off, Math.min(chunk, bytes.length - off));
        }
        return buffer.toCharBuffer();
    }

    /** What the original implementation produced: one decode over all the bytes. */
    private static String decodedContiguously(byte[] bytes, String encoding) throws IOException {
        return TextEncoder.encode(ByteBuffer.wrap(bytes), encoding).toString();
    }

    /** {@code unit} repeated and cut to exactly {@code length} bytes of its UTF-8 encoding. */
    private static byte[] repeat(String unit, int length) {
        byte[] one = unit.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = one[i % one.length];
        }
        return result;
    }
}
