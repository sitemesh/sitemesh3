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
