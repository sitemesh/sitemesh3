package com.opensymphony.sitemesh.tagprocessor.util;

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
public class CharArray implements Appendable {

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
    public int length() {
        return size;
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        append(csq, 0, csq.length());
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        // Optimizations: Messy, but considerably boosts performance.
        if (csq instanceof CharBuffer) {
            return append((CharBuffer) csq);
        }
        if (csq instanceof String) {
            return append((String) csq);
        }

        // Optimization can't be applied... do it the clean way.
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
    public CharArray append(CharArray chars) {
        return append(chars.buffer, 0, chars.size);
    }

    /**
     * Appends an existing CharBuffer on to this one.
     */
    public CharArray append(CharBuffer charBuffer) {
        int length = charBuffer.remaining();
        int requiredSize = length + size;
        if (requiredSize >= buffer.length)
            grow(requiredSize);
        charBuffer.get(buffer, size, length);
        size = requiredSize;
        return this;
    }

    /**
     * Appends the supplied characters to the end of the array.
     */
    public CharArray append(char[] chars) {
        return append(chars, 0, chars.length);
    }

    public CharArray append(char[] chars, int position, int length) {
        int requiredSize = length + size;
        if (requiredSize >= buffer.length)
            grow(requiredSize);
        System.arraycopy(chars, position, buffer, size, length);
        size = requiredSize;
        return this;
    }

    /**
     * Appends a single character to the end of the character array.
     */
    public CharArray append(char c) {
        if (buffer.length == size)
            grow(0);
        buffer[size++] = c;
        return this;
    }

    /**
     * Appends the supplied string to the end of this character array.
     * <p/>
     * Passing in a <tt>null</tt> string will result in a <tt>NullPointerException</tt>.
     */
    public CharArray append(String str) {
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

}

