package com.opensymphony.sitemesh.tagprocessor.util;

import java.io.Writer;
import java.io.IOException;

/**
 * Unsynced version of the JDK's CharArrayWriter
 */
public class CharArrayWriter extends Writer {
    /**
     * The buffer where data is stored.
     */
    protected char buf[];

    /**
     * The number of chars in the buffer.
     */
    protected int count;

    /**
     * Creates a new CharArrayWriter.
     */
    public CharArrayWriter() {
        this(32);
    }

    /**
     * Creates a new CharArrayWriter with the specified initial size.
     *
     * @param initialSize an int specifying the initial buffer size.
     * @throws IllegalArgumentException if initialSize is negative
     */
    public CharArrayWriter(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException("Negative initial size: "
                    + initialSize);
        }
        buf = new char[initialSize];
    }

    /**
     * Writes a character to the buffer.
     */
    @Override
    public void write(int c) {
        int newcount = count + 1;
        if (newcount > buf.length) {
            char newbuf[] = new char[Math.max(buf.length << 1, newcount)];
            System.arraycopy(buf, 0, newbuf, 0, count);
            buf = newbuf;
        }
        buf[count] = (char) c;
        count = newcount;
    }

    /**
     * Writes characters to the buffer.
     *
     * @param c   the data to be written
     * @param off the start offset in the data
     * @param len the number of chars that are written
     */
    @Override
    public void write(char c[], int off, int len) {
        if ((off < 0) || (off > c.length) || (len < 0) ||
                ((off + len) > c.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        int newcount = count + len;
        if (newcount > buf.length) {
            char newbuf[] = new char[Math.max(buf.length << 1, newcount)];
            System.arraycopy(buf, 0, newbuf, 0, count);
            buf = newbuf;
        }
        System.arraycopy(c, off, buf, count, len);
        count = newcount;
    }

    /**
     * Write a portion of a string to the buffer.
     *
     * @param str String to be written from
     * @param off Offset from which to start reading characters
     * @param len Number of characters to be written
     */
    @Override
    public void write(String str, int off, int len) {
        int newcount = count + len;
        if (newcount > buf.length) {
            char newbuf[] = new char[Math.max(buf.length << 1, newcount)];
            System.arraycopy(buf, 0, newbuf, 0, count);
            buf = newbuf;
        }
        str.getChars(off, off + len, buf, count);
        count = newcount;
    }

    /**
     * Writes the contents of the buffer to another character stream.
     *
     * @param out the output stream to write to
     * @throws java.io.IOException If an I/O error occurs.
     */
    public void writeTo(Writer out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * Resets the buffer so that you can use it again without
     * throwing away the already allocated buffer.
     */
    public void reset() {
        count = 0;
    }

    /**
     * Returns a copy of the input data.
     *
     * @return an array of chars copied from the input data.
     */
    public char toCharArray()[] {
        char newbuf[] = new char[count];
        System.arraycopy(buf, 0, newbuf, 0, count);
        return newbuf;
    }

    /**
     * Returns the current size of the buffer.
     *
     * @return an int representing the current size of the buffer.
     */
    public int size() {
        return count;
    }

    /**
     * Converts input data to a string.
     *
     * @return the string.
     */
    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    /**
     * Flush the stream.
     */
    @Override
    public void flush() {
    }

    /**
     * Close the stream.  This method does not release the buffer, since its
     * contents might still be required.
     */
    @Override
    public void close() {
    }

}

