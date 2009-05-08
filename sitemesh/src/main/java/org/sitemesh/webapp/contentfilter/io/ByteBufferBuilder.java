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

    public ByteBufferBuilder() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public ByteBufferBuilder(int aSize) {
        blockSize = aSize;
        buffer = new byte[blockSize];
    }

    public int size() {
        return size + index;
    }

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