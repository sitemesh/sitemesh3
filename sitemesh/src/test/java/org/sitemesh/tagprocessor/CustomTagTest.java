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
