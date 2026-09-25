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

package org.sitemesh.config.properties;

import org.sitemesh.builder.BaseSiteMeshOfflineBuilder;
import org.sitemesh.builder.SiteMeshOfflineBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.offline.directory.Directory;
import org.sitemesh.offline.directory.FileSystemDirectory;
import org.sitemesh.offline.directory.InMemoryDirectory;

import java.util.Map;
import java.io.File;

import static java.nio.CharBuffer.wrap;

public class PropertiesOfflineConfiguratorTest extends PropertiesConfiguratorTest {

    private PropertiesOfflineConfigurator propertiesConfigurator;
    private BaseSiteMeshOfflineBuilder builder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        propertiesConfigurator = createConfigurator(new ObjectFactory.Default(), properties);
        builder = new SiteMeshOfflineBuilder();
    }

    @Override
    protected PropertiesOfflineConfigurator createConfigurator(ObjectFactory objectFactory, Map<String, String> properties) {
        return new PropertiesOfflineConfigurator(objectFactory, properties);
    }

    public void testConfiguresSourceAndDestDir() throws Exception {
        properties.put(PropertiesOfflineConfigurator.SOURCE_DIR_PARAM[0], "some/src/dir");
        properties.put(PropertiesOfflineConfigurator.DEST_DIR_PARAM[0], "some/dest/dir");

        propertiesConfigurator.configureOffline(builder);

        assertEquals(new FileSystemDirectory(new File("some/src/dir")), builder.getSourceDirectory());
        assertEquals(new FileSystemDirectory(new File("some/dest/dir")), builder.getDestinationDirectory());
    }

    public void testAppliesDecoratorSelector() throws Exception {
        // The default MetaTagBasedDecoratorSelector would pick other.html from the meta tag.
        Directory sourceDir = new InMemoryDirectory();
        Directory destinationDir = new InMemoryDirectory();
        sourceDir.save("/main.html", wrap("Main: <sitemesh:write property='title'/>"));
        sourceDir.save("/other.html", wrap("Other: <sitemesh:write property='title'/>"));
        sourceDir.save("/page.html", wrap("<title>Hello</title><meta name='decorator' content='/other.html'>"));
        properties.put(PropertiesConfigurator.DECORATOR_SELECTOR, "org.sitemesh.config.PathBasedDecoratorSelector");
        properties.put(PropertiesConfigurator.DECORATOR_MAPPINGS_PARAM, "/*=/main.html");

        propertiesConfigurator.configureOffline(builder);
        builder.setSourceDirectory(sourceDir).setDestinationDirectory(destinationDir).create().process("/page.html");

        assertEquals("Main: Hello", destinationDir.load("/page.html").toString());
    }

    public void testUsesAlternateSourceAndDestDirParamNames() throws Exception {
        properties.put(PropertiesOfflineConfigurator.SOURCE_DIR_PARAM[1 /* alternate */], "some/src/dir");
        properties.put(PropertiesOfflineConfigurator.DEST_DIR_PARAM[3 /* alternate */], "some/dest/dir");

        propertiesConfigurator.configureOffline(builder);

        assertEquals(new FileSystemDirectory(new File("some/src/dir")), builder.getSourceDirectory());
        assertEquals(new FileSystemDirectory(new File("some/dest/dir")), builder.getDestinationDirectory());
    }
}
