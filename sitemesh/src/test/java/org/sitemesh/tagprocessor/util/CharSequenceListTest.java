package org.sitemesh.tagprocessor.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 */
public class CharSequenceListTest extends TestCase {

    private CharSequenceList charSequenceList = new CharSequenceList();

    public void testBehavesLikeAnAppendable() throws IOException {
        Appendable appendable = charSequenceList;
        appendable.append("STRING");
        appendable.append(',');
        appendable.append("XXSTRING,XX", 2, 9);
        appendable.append(new StringBuffer().append("String").append("Buffer,"));
        appendable.append(new StringBuilder().append("String").append("Builder,"));
        appendable.append(CharBuffer.wrap("CharBuffer,"));

        assertEquals("STRING,STRING,StringBuffer,StringBuilder,CharBuffer,", charSequenceList.toString());
    }

    public void testReferencesCharSequencesThatMayChangeLater() throws IOException {
        Appendable appendable = charSequenceList;

        StringBuilder referenced = new StringBuilder();
        appendable.append("<start>");
        appendable.append(referenced);
        appendable.append("<end>");

        assertEquals("<start><end>", charSequenceList.toString());

        referenced.append("Some new content");

        assertEquals("<start>Some new content<end>", charSequenceList.toString());
    }

    public void testAllowsIterationOfItems() throws IOException {
        Appendable appendable = charSequenceList;
        appendable.append("a");
        appendable.append("bb");
        appendable.append("c");

        StringBuilder out = new StringBuilder();
        for (CharSequence charSequence : charSequenceList) {
            out.append(charSequence).append(',');
        }
        assertEquals("a,bb,c,", out.toString());
    }

    public void testCanContainInstancesOfCharSequenceList() {
        CharSequenceList child = new CharSequenceList();
        child.append("hi");

        CharSequenceList grandChild = new CharSequenceList();
        child.append(grandChild);
        grandChild.append("bye");

        charSequenceList.append(child);

        assertEquals("hibye", charSequenceList.toString());
    }

}
