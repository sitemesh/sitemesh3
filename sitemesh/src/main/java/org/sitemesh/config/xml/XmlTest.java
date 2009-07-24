package org.sitemesh.config.xml;

import junit.framework.TestCase;

import java.util.List;

/**
 * @author Joe Walnes
 */
public class XmlTest extends TestCase {

    public void testExposesTextContentsAndAttributes() {
        Xml xml = new Xml("<doc list='animals'><animal type='sheep'>Fluffy</animal></doc>");
        assertEquals("animals", xml.attribute("list"));
        assertEquals("Fluffy", xml.child("animal").text());
        assertEquals("sheep", xml.child("animal").attribute("type"));
    }

    public void testExposesChildrenByTagName() {
        Xml xml = new Xml("<doc> <a id='1'/> <a id='2'/> <b id='3'><a id='4'/></b> <a id='5'/> </doc>");

        // first child only
        assertEquals("1", xml.child("a").attribute("id"));

        List<Xml> aList = xml.children("a");
        assertEquals("1", aList.get(0).attribute("id"));
        assertEquals("2", aList.get(1).attribute("id"));
        assertEquals("5", aList.get(2).attribute("id"));
        assertEquals(3, aList.size());

        List<Xml> bList = xml.children("b");
        assertEquals("3", bList.get(0).attribute("id"));
        assertEquals(1, bList.size());

        List<Xml> subList = bList.get(0).children("a");
        assertEquals("4", subList.get(0).attribute("id"));
    }

    public void testExposesFirstChildByTagName() {
        Xml xml = new Xml("<doc> <a id='1'/> <a id='2'/> <b id='3'><a id='4'/></b> <a id='5'/> </doc>");

        assertEquals("1", xml.child("a").attribute("id"));
        assertEquals("3", xml.child("b").attribute("id"));
        assertEquals("4", xml.child("b").child("a").attribute("id"));
    }

    public void testReturnsEmptyNodeForMissingElement() {
        Xml xml = new Xml("<doc><animal type='sheep'>Fluffy</animal></doc>");

        assertEquals(1, xml.children("animal").size());
        Xml animal = xml.child("animal");
        assertTrue(animal.exists());
        assertEquals("sheep", animal.attribute("type"));
        assertEquals("Fluffy", animal.text());

        assertEquals(0, xml.children("vegetable").size());
        Xml vegetable = xml.child("vegetable");
        assertFalse(vegetable.exists());
        assertEquals(null, vegetable.attribute("type"));
        assertEquals(null, vegetable.text());

        assertEquals(0, xml.child("animal").child("foo").child("blah").children("invalid").size());
    }

    public void testReturnsDefaultValuesIfValueMissing() {
        Xml xml = new Xml("<doc><animal type='sheep'>Fluffy</animal></doc>");

        Xml animal = xml.child("animal");
        assertEquals("sheep", animal.attribute("type", "MISSING"));
        assertEquals("MISSING", animal.attribute("another", "MISSING"));
        assertEquals("Fluffy", animal.text("MISSING"));

        Xml vegetable = xml.child("vegetable");
        assertEquals("MISSING", vegetable.attribute("type", "MISSING"));
        assertEquals("MISSING", vegetable.text("MISSING"));
    }
}
