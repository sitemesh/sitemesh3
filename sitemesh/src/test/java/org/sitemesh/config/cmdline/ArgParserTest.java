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

package org.sitemesh.config.cmdline;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

public class ArgParserTest extends TestCase {

    public void testExtractsSingleDashedProperties() throws Exception {
        ArgParser args = new ArgParser("-apple", "Apple", "-banana", "Banana");
        assertEquals(map("apple", "Apple", "banana", "Banana"), args.getProperties());
        assertEquals(list(), args.getRemaining());
    }

    public void testExtractsDoubleDashedProperties() throws Exception {
        ArgParser args = new ArgParser("--apple", "Apple", "--banana", "Banana");
        assertEquals(map("apple", "Apple", "banana", "Banana"), args.getProperties());
        assertEquals(list(), args.getRemaining());
    }

    public void testExtractsEqualsDelimitedProperties() throws Exception {
        ArgParser args = new ArgParser("-apple=Apple", "--cabbage=", "--banana=Banana");
        assertEquals(map("apple", "Apple", "banana", "Banana", "cabbage", ""), args.getProperties());
        assertEquals(list(), args.getRemaining());
    }

    public void testExtractsRemainingArgs() throws Exception {
        ArgParser args = new ArgParser("-apple", "Apple", "--banana=Banana", "--cabbage=", "some", "left", "over args");
        assertEquals(map("apple", "Apple", "banana", "Banana", "cabbage", ""), args.getProperties());
        assertEquals(list("some", "left", "over args"), args.getRemaining());
    }

    public void testThrowsExceptionOnHangingProperties() throws Exception {
        try {
            new ArgParser("-apple", "Apple", "--banana");
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("Parameter 'banana' does not have value associated.", expected.getMessage());
        }
    }

    public void testThrowsExceptionIfPropertyAppearsAfterRemaining() throws Exception {
        try {
            new ArgParser("-apple", "Apple", "-banana", "Banana", "some", "left", "-arg", "argvalue", "over");
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
            assertEquals("Parameters have to appear first.", expected.getMessage());
        }
    }

    private Map<String, String> map(String... pairs) {
        assertTrue("Even number of items expected", pairs.length % 2 == 0);
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(pairs[i], pairs[i + 1]);
        }
        return result;
    }

    private List<String> list(String... items) {
        return Arrays.asList(items);
    }
}
