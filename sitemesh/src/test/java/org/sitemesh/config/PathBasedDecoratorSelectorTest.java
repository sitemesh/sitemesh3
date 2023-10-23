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

package org.sitemesh.config;

import junit.framework.TestCase;
import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContextStub;
import org.sitemesh.content.Content;
import org.sitemesh.content.memory.InMemoryContent;

import java.io.IOException;

/**
 * @author Joe Walnes
 */
public class PathBasedDecoratorSelectorTest extends TestCase {

    public void testSelectsDecoratorBasedOnContentRequestPath() throws IOException {
        Content content = new InMemoryContent();
        DecoratorSelector selector = new PathBasedDecoratorSelector()
                .put("/*", "/decorators/default.jsp")
                .put("/admin/*", "/decorators/admin.jsp")
                .put("/thingy", "/decorators/thingy.jsp")
                .put("/multiple", "/1.jsp", "/2.jsp", "/3.jsp");

        assertEquals(
                join("/decorators/admin.jsp"),
                join(selector.selectDecoratorPaths(content,
                        new SiteMeshContextStub().withPath("/admin/foo"))));
        assertEquals(
                join("/decorators/thingy.jsp"),
                join(selector.selectDecoratorPaths(content,
                        new SiteMeshContextStub().withPath("/thingy"))));
        assertEquals(
                join("/decorators/default.jsp"),
                join(selector.selectDecoratorPaths(content,
                        new SiteMeshContextStub().withPath("/thingy-not"))));
        assertEquals(
                join("/1.jsp", "/2.jsp", "/3.jsp"),
                join(selector.selectDecoratorPaths(content,
                        new SiteMeshContextStub().withPath("/multiple"))));
    }

    public void testReturnsEmptyArrayForUnMappedPaths() throws IOException {
        Content content = new InMemoryContent();
        DecoratorSelector selector = new PathBasedDecoratorSelector()
                .put("/a/*", "A", "B");

        assertEquals(
                join("A", "B"),
                join(selector.selectDecoratorPaths(content,
                        new SiteMeshContextStub().withPath("/a/foo"))));
        assertEquals(0, selector.selectDecoratorPaths(content,
                new SiteMeshContextStub().withPath("/b/foo")).length);
    }

    private static String join(String... strings) {
        StringBuilder result = new StringBuilder();
        for (String string : strings) {
            if (result.length() > 0) {
                result.append('|');
            }
            result.append(string);
        }
        return result.toString();
    }
}
