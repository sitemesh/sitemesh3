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

package org.sitemesh.tagprocessor;

import junit.framework.TestCase;

/**
 * @author Joe Walnes
 */
public class CustomTagTest extends TestCase {

    public void testWritesOutUserDefinedTag() {
        assertEquals("<hello/>", new CustomTag("hello", Tag.Type.EMPTY).toString());
        assertEquals("<hello>", new CustomTag("hello", Tag.Type.OPEN).toString());
        assertEquals("</hello>", new CustomTag("hello", Tag.Type.CLOSE).toString());
    }

    public void testWritesAttributes() {
        CustomTag tag = new CustomTag("hello", Tag.Type.EMPTY);
        tag.addAttribute("color", "green");
        tag.addAttribute("stuff", null);
        assertEquals("<hello color=\"green\" stuff/>", tag.toString());
    }

    public void testAllowsAttributesToBeManipulated() {
        CustomTag tag = new CustomTag("hello", Tag.Type.OPEN);
        assertEquals("<hello>", tag.toString());

        tag.addAttribute("a", "aaa");
        tag.addAttribute("b", "bbb");
        assertEquals("<hello a=\"aaa\" b=\"bbb\">", tag.toString());

        tag.removeAttribute("b", false);
        assertEquals("<hello a=\"aaa\">", tag.toString());

        tag.setAttributeValue("a", false, "zzz");
        assertEquals("<hello a=\"zzz\">", tag.toString());
      
        tag.addAttribute("c", "ccc");
        int index = tag.getAttributeIndex("c", true);
        assertEquals(1, index);
        assertEquals("ccc", tag.getAttributeValue(index));
        assertEquals("c", tag.getAttributeName(index));
    }
}
