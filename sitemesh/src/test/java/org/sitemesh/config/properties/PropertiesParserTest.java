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

package org.sitemesh.config.properties;

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
        propertiesParser = new PropertiesParser(properties);
    }

    public void testRetrievesStringOrNullIfNotFound() {
        properties.put("foo", "bar");
        assertEquals("bar", propertiesParser.getString("foo"));
        assertNull(propertiesParser.getString("other"));
    }

    public void testSearchesFallbackPropertiesUntilFindingAResult() {
        properties.put("foo", "bar");
        assertEquals("bar", propertiesParser.getString("flimp", "flump", "foo", "foot"));
        assertNull(propertiesParser.getString("other", "out", "oooh"));
    }

    public void testDiscardsLeadingAndTrailingWhitespace() {
        properties.put("foo", "  \n ba r ");
        properties.put("other", "   \n  ");
        assertEquals("ba r", propertiesParser.getString("foo"));
        assertNull(propertiesParser.getString("other"));
    }

    public void testSplitsStringArrayOnCommasOrWhitespace() {
        properties.put("foo", "   aaaa   bbbb,cccc\ndddd \n ");
        assertEquals("aaaa|bbbb|cccc|dddd",
                joinSequence(propertiesParser.getStringArray("foo")));
        assertEquals(0, propertiesParser.getStringArray("other").length);
    }

    public void testSplitsKeyValuesIntoMap() {
        properties.put("foo", "   aa=Apples,zz=Ziggy\n   bb=Bananas ");
        assertEquals("(aa:Apples)|(zz:Ziggy)|(bb:Bananas)",
                joinMultiMap(propertiesParser.getStringMultiMap("foo")));
        assertTrue(propertiesParser.getStringMultiMap("other").isEmpty());
    }

    public void testSplitsKeyValuesIntoMultiMap() {
        properties.put("foo", "   aa=Apples|Aardvark,zz=Ziggy|Zoo\n   bb=Bananas ");
        assertEquals("(aa:Apples|Aardvark)|(zz:Ziggy|Zoo)|(bb:Bananas)",
                joinMultiMap(propertiesParser.getStringMultiMap("foo")));
        assertTrue(propertiesParser.getStringMultiMap("other").isEmpty());
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

    private String joinMultiMap(Map<String, String[]> map) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            if (result.length() > 0) {
                result.append('|');
            }
            result.append('(').append(entry.getKey()).append(":").append(joinSequence(entry.getValue())).append(')');
        }
        return result.toString();
    }
}
