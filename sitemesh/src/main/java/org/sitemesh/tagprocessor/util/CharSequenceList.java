package org.sitemesh.tagprocessor.util;

import org.sitemesh.tagprocessor.CharSequenceBuffer;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An {@link java.lang.Appendable} buffer of character data,similar to {@link java.lang.StringBuilder} and
 * {@link java.lang.StringBuffer}), except rather than copying the contents to an array of characters,
 * it keeps a list of references to the original {@link java.lang.CharSequence}s.
 *
 * <p>This offers a few advantages:</p>
 * <ul>
 * <li>If the CharSequences are already in memory elsewhere, it avoids the need for having
 * multiple copies, thereby saving memory.</li>
 * <li>Avoiding copying the actual char data also offers a performance boost when copying large
 * CharSequences.</li>
 * <li>The actual CharSequence being referred to does not need to know it's data until the entire
 * CharSequenceList is rendered. This offers the ability to do async data loading.</li>
 * </ul>
 *
 * <p>With these advantages, come some disadvantages:</p>
 * <ul>
 * <li>Appending an individual char requires wrapping it in an object and appending to a list. Avoid appending
 * lots of small CharSequences - it's better to use fewer larger chunks.</li>
 * <li>Random access reading through the CharSequence interface is not yet supported. Use {@link #writeTo(Appendable)}
 * instead.</li>
 * </ul>
 *
 * @author Joe Walnes
 */
public class CharSequenceList implements CharSequenceBuffer {

    private final LinkedList<CharSequence> list = new LinkedList<CharSequence>();

    public Appendable append(CharSequence csq) {
        list.add(csq);
        return this;
    }

    public Appendable append(CharSequence csq, int start, int end) {
        return append(csq.subSequence(start, end));
    }

    /**
     * Warning: Each time this method is called, a new String of length 1 is constructed
     * and added to a LinkedList - this is not optimal. If building up strings, it is
     * more efficient to build these up externally in a StringBuilder, and then pass that
     * to {@link #append(CharSequence)}.
     */
    public Appendable append(char c) {
        return append(String.valueOf(c));
    }

    public int length() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public char charAt(int index) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public CharSequence subSequence(int start, int end) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Iterator<CharSequence> iterator() {
        return Collections.unmodifiableList(list).iterator();
    }

    public String toString() {
        try {
            StringBuilder out = new StringBuilder();
            writeTo(out);
            return out.toString();
        } catch (IOException e) {
            throw new Error("Internal error. Please report to SiteMesh team.");
        }
    }

    public void writeTo(Appendable out) throws IOException {
        for (CharSequence charSequence : list) {
            if (charSequence instanceof CharSequenceBuffer) {
                // Optimization.
                ((CharSequenceBuffer) charSequence).writeTo(out);
            } else {
                out.append(charSequence);
            }
        }
    }

}
