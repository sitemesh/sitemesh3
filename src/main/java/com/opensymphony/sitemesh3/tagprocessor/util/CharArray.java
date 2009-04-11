package com.opensymphony.sitemesh3.tagprocessor.util;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * A leaner, meaner version of StringBuilder.
 * <p>It provides basic functionality to handle dynamically-growing
 * char arrays as quickly as possible. This class is not threadsafe.</p>
 *
 * @author Chris Miller
 * @author Joe Walnes
 */
public class CharArray implements Appendable, CharSequence {

    private int size = 0;
    private char[] buffer;

    /**
     * Constructs a CharArray that is initialized to the specified size.
     * <p/>
     * Do not pass in a negative value because there is no bounds checking!
     */
    public CharArray(int size) {
        buffer = new char[size];
    }

    public CharArray() {
        this(1024);
    }

    /**
     * Returns a String represenation of the character array.
     */
    @Override
    public String toString() {
        return new String(buffer, 0, size);
    }

    /**
     * Returns the current length of the character array.
     */
    @Override
    public int length() {
        return size;
    }

    @Override
    public char charAt(int index) {
        return buffer[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end < 0 || end > size || start > end) {
            throw new IndexOutOfBoundsException("start=" + start + ",end=" + end + ",length=" + size);
        }
        return new SubSequence(start, end);
    }

    /**
     * Appends a single character to the end of the character array.
     */
    @Override
    public Appendable append(char c) {
        if (buffer.length == size)
            grow(0);
        buffer[size++] = c;
        return this;
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        // Optimizations: Messy, but considerably boosts performance.
        if (csq instanceof CharBuffer) {
            return append((CharBuffer) csq);
        }
        if (csq instanceof String) {
            return append((String) csq);
        }
        if (csq instanceof CharArray) {
            return append((CharArray) csq);
        }

        // Optimization can't be applied... do it the clean way.
        for (int i = 0; i < csq.length(); i++) {
            append(csq.charAt(i));
        }

        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        for (int i = start; i < end; i++) {
            append(csq.charAt(i));
        }

        return this;
    }

    /**
     * Appends an existing CharArray on to this one.
     * <p/>
     * Passing in a <tt>null</tt> CharArray will result in a <tt>NullPointerException</tt>.
     */
    private Appendable append(CharArray chars) {
        return append(chars.buffer, 0, chars.size);
    }

    /**
     * Appends an existing CharBuffer on to this one.
     */
    private Appendable append(CharBuffer charBuffer) {
        int length = charBuffer.remaining();
        int requiredSize = length + size;
        if (requiredSize >= buffer.length)
            grow(requiredSize);
        charBuffer.get(buffer, size, length);
        size = requiredSize;
        return this;
    }

    private Appendable append(char[] chars, int position, int length) {
        int requiredSize = length + size;
        if (requiredSize >= buffer.length)
            grow(requiredSize);
        System.arraycopy(chars, position, buffer, size, length);
        size = requiredSize;
        return this;
    }

    /**
     * Appends the supplied string to the end of this character array.
     * <p/>
     * Passing in a <tt>null</tt> string will result in a <tt>NullPointerException</tt>.
     */
    private CharArray append(String str) {
        int requiredSize = str.length() + size;
        if (requiredSize >= buffer.length)
            grow(requiredSize);

        str.getChars(0, str.length(), buffer, size);

        size = requiredSize;
        return this;
    }

    /**
     * Grows the internal array by either ~100% or minSize (whichever is larger),
     * up to a maximum size of Integer.MAX_VALUE.
     */
    private void grow(int minSize) {
        int newCapacity = (buffer.length + 1) * 2;
        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        } else if (minSize > newCapacity) {
            newCapacity = minSize;
        }
        char newBuffer[] = new char[newCapacity];
        System.arraycopy(buffer, 0, newBuffer, 0, size);
        buffer = newBuffer;
    }

    /**
     * Clear the contents.
     */
    public final void clear() {
        size = 0;
    }

    private class SubSequence implements CharSequence {
        private int start;
        private int end;

        public SubSequence(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public int length() {
            return end - start;
        }

        @Override
        public char charAt(int index) {
            return buffer[index + start];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (start < 0 || end < 0 || end > this.end - size || start > end) {
                throw new IndexOutOfBoundsException("start=" + start + ",end=" + end + ",length=" + size);
            }
            return new SubSequence(this.start + start, this.start + end);
        }
    }

}

