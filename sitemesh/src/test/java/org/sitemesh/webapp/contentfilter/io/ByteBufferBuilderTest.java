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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Pins the public behaviour of {@link ByteBufferBuilder}. Small block sizes
 * make every block-boundary case (a write ending exactly on a boundary,
 * spanning several blocks, starting on a full block) cheap to enumerate.
 */
public class ByteBufferBuilderTest extends TestCase {

    public void testNewBuilderIsEmpty() {
        ByteBufferBuilder builder = new ByteBufferBuilder();

        assertEquals(0, builder.size());
        ByteBuffer result = builder.toByteBuffer();
        assertEquals(0, result.position());
        assertEquals(0, result.limit());
        assertEquals(0, result.capacity());
    }

    public void testEveryWriteShapeAgainstEveryBlockBoundary() {
        for (int blockSize = 1; blockSize <= 5; blockSize++) {
            for (int before = 0; before <= 11; before++) {
                for (int length = 0; length <= 13; length++) {
                    ByteBufferBuilder builder = new ByteBufferBuilder(blockSize);
                    ByteArrayOutputStream expected = new ByteArrayOutputStream();
                    for (int i = 0; i < before; i++) {
                        builder.write('a' + i);
                        expected.write('a' + i);
                    }
                    byte[] data = new byte[length + 4];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = (byte) ('A' + i);
                    }
                    builder.write(data, 2, length);
                    expected.write(data, 2, length);
                    builder.write('!');
                    expected.write('!');

                    String label = "blockSize=" + blockSize + " before=" + before + " length=" + length;
                    assertEquals(label, expected.size(), builder.size());
                    assertEquals(label, Arrays.toString(expected.toByteArray()), Arrays.toString(bytesOf(builder)));
                }
            }
        }
    }

    public void testManyWritesAtTheDefaultBlockSize() {
        ByteBufferBuilder builder = new ByteBufferBuilder();
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        byte[] data = new byte[20000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 31);
        }
        int[] lengths = {1, 8191, 8192, 8193, 4096, 4096, 0, 128, 20000, 3};
        int offset = 0;
        for (int length : lengths) {
            int from = offset % (data.length - length + 1);
            builder.write(data, from, length);
            expected.write(data, from, length);
            offset += 7919;
        }

        assertEquals(expected.size(), builder.size());
        assertTrue(Arrays.equals(expected.toByteArray(), bytesOf(builder)));
    }

    public void testToByteBufferReturnsAFreshReadyToReadCopyEachTime() {
        ByteBufferBuilder builder = new ByteBufferBuilder(4);
        builder.write(new byte[]{1, 2, 3, 4, 5, 6}, 0, 6);

        ByteBuffer first = builder.toByteBuffer();
        ByteBuffer second = builder.toByteBuffer();

        assertEquals(0, first.position());
        assertEquals(6, first.limit());
        assertEquals(6, first.capacity());
        assertTrue(first.hasArray());
        assertNotSame(first.array(), second.array());
        first.put(0, (byte) 99);
        assertEquals(1, second.get(0));
        assertEquals(1, builder.toByteBuffer().get(0));
    }

    public void testToStringDescribesTheGatheredByteBuffer() {
        ByteBufferBuilder builder = new ByteBufferBuilder(4);
        builder.write(new byte[10], 0, 10);

        assertEquals("java.nio.HeapByteBuffer[pos=0 lim=10 cap=10]", builder.toString());
    }

    public void testNullArrayThrowsNullPointerException() {
        try {
            new ByteBufferBuilder().write(null, 0, 0);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    public void testInvalidRangesThrowIndexOutOfBoundsException() {
        byte[] data = new byte[10];
        int[][] ranges = {{-1, 2}, {0, -1}, {0, 11}, {5, 6}, {11, 0}};
        for (int blockSize : new int[]{1, 3, 8192}) {
            ByteBufferBuilder builder = new ByteBufferBuilder(blockSize);
            builder.write(new byte[]{7, 7}, 0, 2);
            for (int[] range : ranges) {
                try {
                    builder.write(data, range[0], range[1]);
                    fail("Expected IndexOutOfBoundsException for " + Arrays.toString(range));
                } catch (IndexOutOfBoundsException expected) {
                    assertEquals(IndexOutOfBoundsException.class, expected.getClass());
                }
            }
            assertEquals(2, builder.size());
        }
    }

    public void testEndOverflowingIntWritesTheExistingBytesThenFails() {
        // offset + length overflows past the range check; historically the bytes up
        // to the end of the array were written one at a time before the failure.
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBufferBuilder builder = new ByteBufferBuilder(4);
        try {
            builder.write(data, 5, Integer.MAX_VALUE);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // expected
        }
        assertEquals(5, builder.size());
        assertTrue(Arrays.equals(new byte[]{5, 6, 7, 8, 9}, bytesOf(builder)));
    }

    public void testZeroBlockSizeAcceptsEmptyWritesAndFailsOnAnyByte() {
        ByteBufferBuilder builder = new ByteBufferBuilder(0);
        builder.write(new byte[3], 1, 0);
        assertEquals(0, builder.size());

        try {
            builder.write(new byte[]{1}, 0, 1);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // expected
        }
        // The failed store has already advanced the index (buffer[index++] = ...),
        // leaving a size the builder cannot back with bytes.
        assertEquals(1, builder.size());
        try {
            builder.toByteBuffer();
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    public void testNegativeBlockSizeIsRejectedAtConstruction() {
        try {
            new ByteBufferBuilder(-1);
            fail("Expected NegativeArraySizeException");
        } catch (NegativeArraySizeException expected) {
            // expected
        }
    }

    private static byte[] bytesOf(ByteBufferBuilder builder) {
        ByteBuffer buffer = builder.toByteBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
