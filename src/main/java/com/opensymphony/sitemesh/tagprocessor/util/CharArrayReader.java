package com.opensymphony.sitemesh.tagprocessor.util;

import java.io.Reader;
import java.io.IOException;

/**
 * This class implements a character buffer that can be used as a
 * character-input stream.
 * <p/>
 * Modified from the JDK source in that it gets rid of the
 * ensureOpen() method, so we get unexpected behaviour if the
 * reader is closed.
 * <p/>
 * The second modification is that since this class is used
 * internally by FastPageParser in a single thread, we don't
 * need any locking or synchronization. Using this class
 * instead of the standard CharArrayReader improves
 * FastPageParser performance by 15-20%.
 *
 * @author Hani Suleiman
 */
public class CharArrayReader extends Reader {
    /**
     * The character buffer.
     */
    protected char buf[];

    /**
     * The current buffer position.
     */
    protected int pos;

    /**
     * The position of mark in buffer.
     */
    protected int markedPos = 0;

    /**
     * The index of the end of this buffer.  There is not valid
     * data at or beyond this index.
     */
    protected int count;

    /**
     * Create an CharArrayReader from the specified array of chars.
     *
     * @param buf Input buffer (not copied)
     */
    public CharArrayReader(char buf[]) {
        this.buf = buf;
        this.pos = 0;
        this.count = buf.length;
    }

    /**
     * Create an CharArrayReader from the specified array of chars.
     *
     * @param buf    Input buffer (not copied)
     * @param offset Offset of the first char to read
     * @param length Number of chars to read
     */
    public CharArrayReader(char buf[], int offset, int length) {
        if ((offset < 0) || (offset > buf.length) || (length < 0) || ((offset + length) < 0)) {
            throw new IllegalArgumentException();
        }
        this.buf = buf;
        this.pos = offset;
        this.count = Math.min(offset + length, buf.length);
        this.markedPos = offset;
    }

    /**
     * Read a single character.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if (pos >= count)
            return -1;
        else
            return buf[pos++];
    }

    /**
     * Read characters into a portion of an array.
     *
     * @param b   Destination buffer
     * @param off Offset at which to start storing characters
     * @param len Maximum number of characters to read
     * @return The actual number of characters read, or -1 if
     *         the end of the stream has been reached
     * @throws IOException If an I/O error occurs
     */
    @Override
    public int read(char b[], int off, int len) throws IOException {
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (pos >= count) {
            return -1;
        }
        if (pos + len > count) {
            len = count - pos;
        }
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    }

    /**
     * Skip characters.
     *
     * @param n The number of characters to skip
     * @throws IOException If an I/O error occurs
     * @return The number of characters actually skipped
     */
    @Override
    public long skip(long n) throws IOException {
        if (pos + n > count) {
            n = count - pos;
        }
        if (n < 0) {
            return 0;
        }
        pos += n;
        return n;
    }

    /**
     * Tell whether this stream is ready to be read.  Character-array readers
     * are always ready to be read.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public boolean ready() throws IOException {
        return (count - pos) > 0;
    }

    /**
     * Tell whether this stream supports the mark() operation, which it does.
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Mark the present position in the stream.  Subsequent calls to reset()
     * will reposition the stream to this point.
     *
     * @param readAheadLimit Limit on the number of characters that may be
     *                       read while still preserving the mark.  Because
     *                       the stream's input comes from a character array,
     *                       there is no actual limit; hence this argument is
     *                       ignored.
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void mark(int readAheadLimit) throws IOException {
        markedPos = pos;
    }

    /**
     * Reset the stream to the most recent mark, or to the beginning if it has
     * never been marked.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void reset() throws IOException {
        pos = markedPos;
    }

    /**
     * Close the stream.
     */
    @Override
    public void close() {
        buf = null;
    }
}

