/*
 *    Copyright 2009-2026 SiteMesh authors.
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

package org.sitemesh.config.xml;

import junit.framework.TestCase;
import org.sitemesh.builder.SiteMeshOfflineBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.offline.directory.Directory;
import org.sitemesh.offline.directory.InMemoryDirectory;
import org.xml.sax.InputSource;

import java.io.StringReader;

import static java.nio.CharBuffer.wrap;

public class XmlOfflineConfiguratorTest extends TestCase {

    private Directory sourceDir;
    private Directory destinationDir;

    @Override
    protected void setUp() throws Exception {
        sourceDir = new InMemoryDirectory();
        destinationDir = new InMemoryDirectory();
        sourceDir.save("/WEB-INF/decorators/main.html", wrap("Main: <sitemesh:write property='title'/>"));
        sourceDir.save("/WEB-INF/decorators/other.html", wrap("Other: <sitemesh:write property='title'/>"));
    }

    public void testAppliesDecoratorPrefix() throws Exception {
        sourceDir.save("/page.html", wrap("<title>Hello</title>"));

        process("<sitemesh>" +
                "  <decorator-prefix>/WEB-INF/decorators/</decorator-prefix>" +
                "  <mapping path='/*' decorator='main.html'/>" +
                "</sitemesh>", "/page.html");

        assertEquals("Main: Hello", destinationDir.load("/page.html").toString());
    }

    public void testAppliesDecoratorSelector() throws Exception {
        // The default MetaTagBasedDecoratorSelector would pick other.html from the meta tag.
        sourceDir.save("/page.html", wrap("<title>Hello</title><meta name='decorator' content='/WEB-INF/decorators/other.html'>"));

        process("<sitemesh>" +
                "  <decorator-selector>org.sitemesh.config.PathBasedDecoratorSelector</decorator-selector>" +
                "  <mapping path='/*' decorator='/WEB-INF/decorators/main.html'/>" +
                "</sitemesh>", "/page.html");

        assertEquals("Main: Hello", destinationDir.load("/page.html").toString());
    }

    private void process(String xml, String path) throws Exception {
        SiteMeshOfflineBuilder builder = new SiteMeshOfflineBuilder()
                .setSourceDirectory(sourceDir)
                .setDestinationDirectory(destinationDir);
        new XmlOfflineConfigurator(new ObjectFactory.Default(),
                Xml.getSecureDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement())
                .configureOffline(builder);
        builder.create().process(path);
    }
}
