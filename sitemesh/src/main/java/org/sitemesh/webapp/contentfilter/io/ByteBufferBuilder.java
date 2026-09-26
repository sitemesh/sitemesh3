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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.LinkedList;

/**
 * A speedy implementation of ByteArrayOutputStream. It's not synchronized, and it
 * does not copy buffers when it's expanded.
 *
 * @author Rickard Öberg
 * @author Scott Farquhar
 */
public class ByteBufferBuilder {
    private static final int DEFAULT_BLOCK_SIZE = 8192;

    /**
     * Internal buffer.
     */
    private byte[] buffer;

    private LinkedList<byte[]> buffers;

    private int index;
    private int size;
    private int blockSize;

    /**
     * Creates a builder with the default block size (8192 bytes).
     */
    public ByteBufferBuilder() {
        this(DEFAULT_BLOCK_SIZE);
    }

    /**
     * Creates a builder with the given block size.
     *
     * @param aSize size in bytes of each internal buffer block.
     */
    public ByteBufferBuilder(int aSize) {
        blockSize = aSize;
        buffer = new byte[blockSize];
    }

    /**
     * @return total number of bytes written so far.
     */
    public int size() {
        return size + index;
    }

    /**
     * Copy the written bytes into a single, freshly allocated {@link ByteBuffer}.
     *
     * @return buffer containing all bytes written so far, ready for reading.
     */
    public ByteBuffer toByteBuffer() {
        ByteBuffer result = ByteBuffer.allocate(size());

        // check if we have a list of buffers
        if (buffers != null) {
            for (byte[] current : buffers) {
                result.put(current);
            }
        }

        // write the internal buffer directly
        result.put(buffer, 0, index);

        result.flip();
        return result;
    }

    /**
     * Write a single byte to the buffer.
     *
     * @param datum the byte to write (lowest 8 bits are used).
     */
    public void write(int datum) {
        if (index == blockSize) {
            nextBlock();
        }

        // store the byte
        buffer[index++] = (byte) datum;
    }

    /**
     * Write a range of bytes to the buffer.
     *
     * @param data source byte array. Must not be null.
     * @param offset offset of the first byte to write.
     * @param length number of bytes to write.
     */
    public void write(byte[] data, int offset, int length) {
        if (data == null) {
            throw new NullPointerException();
        } else if ((offset < 0) || (offset + length > data.length)
                || (length < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (index + length < blockSize) {
            // copy in the subarray
            System.arraycopy(data, offset, buffer, index, length);
            index += length;
        } else if (offset + length < 0 || blockSize == 0) {
            // offset + length overflowed, slipping past the range check above,
            // or no block can hold a byte. Keep the original byte-at-a-time
            // copy for these broken calls, so they still append whatever bytes
            // exist before failing with an ArrayIndexOutOfBoundsException.
            for (int i = 0; i < length; i++) {
                write(data[offset + i]);
            }
        } else {
            // Fill the current block, then carry on in new ones, copying a
            // block's worth at a time. Like write(int), a new block is only
            // started once there is a byte to put in it.
            int room = blockSize - index;
            System.arraycopy(data, offset, buffer, index, room);
            index = blockSize;
            offset += room;
            length -= room;
            while (length > 0) {
                nextBlock();
                int count = Math.min(length, blockSize);
                System.arraycopy(data, offset, buffer, 0, count);
                index = count;
                offset += count;
                length -= count;
            }
        }
    }

    /**
     * Store the full current block and start a new, empty one.
     */
    private void nextBlock() {
        if (buffers == null)
            buffers = new LinkedList<byte[]>();

        buffers.addLast(buffer);

        buffer = new byte[blockSize];
        size += index;
        index = 0;
    }

    /**
     * Decode everything written so far into {@code out}, reading the blocks
     * where they are. Gives the same result as
     * {@code decoder.decode(toByteBuffer(), out, true)} without first copying
     * every block into one freshly allocated buffer.
     *
     * <p>A character whose bytes straddle two blocks is completed in a small
     * carry buffer, fed one byte at a time from the next block, so the
     * decoder sees the same bytes — and so makes the same replacements for
     * malformed or truncated input — as it would reading them contiguously.</p>
     *
     * @param decoder a freshly created (or reset) decoder.
     * @param out destination for the decoded characters.
     * @return the result of the first decoding step that did not underflow
     *         (overflow, or an error the decoder is set to report), otherwise
     *         the result of the final, end-of-input step.
     */
    CoderResult decodeTo(CharsetDecoder decoder, CharBuffer out) {
        if (buffers == null) {
            return decoder.decode(ByteBuffer.wrap(buffer, 0, index), out, true);
        }
        BlockDecoder blocks = new BlockDecoder(decoder, out);
        for (byte[] block : buffers) {
            CoderResult result = blocks.decode(ByteBuffer.wrap(block));
            if (!result.isUnderflow()) {
                return result;
            }
        }
        CoderResult result = blocks.decode(ByteBuffer.wrap(buffer, 0, index));
        if (!result.isUnderflow()) {
            return result;
        }
        return blocks.finish();
    }

    /**
     * Feeds a sequence of blocks to a decoder as one continuous input.
     */
    private static final class BlockDecoder {
        private final CharsetDecoder decoder;
        private final CharBuffer out;

        /** Bytes the decoder left unconsumed at the end of the previous block (write mode). */
        private ByteBuffer carry = ByteBuffer.allocate(16);

        BlockDecoder(CharsetDecoder decoder, CharBuffer out) {
            this.decoder = decoder;
            this.out = out;
        }

        CoderResult decode(ByteBuffer block) {
            // Complete the character left over from the previous block first,
            // one byte at a time until the decoder has consumed the carry.
            while (carry.position() > 0 && block.hasRemaining()) {
                ensureCarryRoom(1);
                carry.put(block.get());
                carry.flip();
                CoderResult result = decoder.decode(carry, out, false);
                carry.compact();
                if (!result.isUnderflow()) {
                    return result;
                }
            }
            CoderResult result = decoder.decode(block, out, false);
            if (result.isUnderflow() && block.hasRemaining()) {
                ensureCarryRoom(block.remaining());
                carry.put(block);
            }
            return result;
        }

        CoderResult finish() {
            carry.flip();
            return decoder.decode(carry, out, true);
        }

        private void ensureCarryRoom(int needed) {
            if (carry.remaining() < needed) {
                ByteBuffer larger = ByteBuffer.allocate(Math.max(carry.capacity() * 2, carry.position() + needed));
                carry.flip();
                larger.put(carry);
                carry = larger;
            }
        }
    }

    @Override
    public String toString() {
        return toByteBuffer().toString();
    }

}