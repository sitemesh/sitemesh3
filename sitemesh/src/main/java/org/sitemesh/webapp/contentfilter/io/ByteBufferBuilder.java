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

import java.nio.ByteBuffer;
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
            // Create new buffer and store current in linked list
            if (buffers == null)
                buffers = new LinkedList<byte[]>();

            buffers.addLast(buffer);

            buffer = new byte[blockSize];
            size += index;
            index = 0;
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
        } else {
            if (index + length >= blockSize) {
                // Write byte by byte
                // TODO: optimize this to use arraycopy's instead
                for (int i = 0; i < length; i++) {
                    write(data[offset + i]);
                }
            } else {
                // copy in the subarray
                System.arraycopy(data, offset, buffer, index, length);
                index += length;
            }
        }
    }

    @Override
    public String toString() {
        return toByteBuffer().toString();
    }

}