package com.opensymphony.sitemesh.simple;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;

/**
 * @author Joe Walnes
 */
public class PropertiesParserTest extends TestCase {

    private Map<String, String> properties;

    private PropertiesParser propertiesParser;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        properties = new HashMap<String, String>();
        propertiesParser = new PropertiesParser() {
            @Override
            protected String getProperty(String key) {
                return properties.get(key);
            }
        };
    }

    public void testRetrievesStringOrDefaultValueIfNotFound() {
        properties.put("foo", "bar");
        assertEquals("bar", propertiesParser.getString("foo", "fooDefault"));
        assertEquals("otherDefault", propertiesParser.getString("other", "otherDefault"));
        assertNull(propertiesParser.getString("other", null));
    }

    public void testDiscardsLeadingAndTrailingWhitespace() {
        properties.put("foo", "  \n ba r ");
        properties.put("other", "   \n  ");
        assertEquals("ba r", propertiesParser.getString("foo", "fooDefault"));
        assertEquals("otherDefault", propertiesParser.getString("other", "otherDefault"));
    }

    public void testSplitsStringArrayOnCommasOrWhitespace() {
        properties.put("foo", "   aaaa   bbbb,cccc\ndddd \n ");
        assertEquals("aaaa|bbbb|cccc|dddd",
                joinSequence(propertiesParser.getStringArray("foo", "foo Default")));
        assertEquals("other|Default",
                joinSequence(propertiesParser.getStringArray("other", "other Default")));
        assertEquals(0, propertiesParser.getStringArray("other", null).length);
    }

    public void testSplitsKeyValuesIntoMap() {
        properties.put("foo", "   aa=Apples,zz=Ziggy\n   bb=Bananas ");
        assertEquals("(aa:Apples)|(zz:Ziggy)|(bb:Bananas)",
                joinDict(propertiesParser.getStringMap("foo", "a=Default")));
        assertEquals("(other:Default)|(foo:bar)",
                joinDict(propertiesParser.getStringMap("other", "other=Default,foo=bar")));
        assertEquals(0, propertiesParser.getStringMap("other", null).size());
    }

    private String joinSequence(String... sequence) {
        StringBuilder result = new StringBuilder();
        for (String s : sequence) {
            if (result.length() > 0) {
                result.append('|');
            }
            result.append(s);
        }
        return result.toString();
    }

    private String joinDict(Map<String, String> map) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (result.length() > 0) {
                result.append('|');
            }
            result.append('(').append(entry.getKey()).append(":").append(entry.getValue()).append(')');
        }
        return result.toString();
    }
}
