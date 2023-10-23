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

import org.sitemesh.builder.BaseSiteMeshFilterBuilder;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.webapp.contentfilter.Selector;

import java.util.Map;

/**
 * @author Joe Walnes
 */
public class PropertiesFilterConfiguratorTest extends PropertiesConfiguratorTest {

    // This test extends PropertiesConfiguratorTest, so it also runs each of those
    // tests on the PropertiesFilterConfigurator (just to be sure).

    private PropertiesFilterConfigurator propertiesConfigurator;
    private BaseSiteMeshFilterBuilder builder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        propertiesConfigurator = createConfigurator(new ObjectFactory.Default(), properties);
        builder = new SiteMeshFilterBuilder();
    }

    @Override
    protected PropertiesFilterConfigurator createConfigurator(ObjectFactory objectFactory, Map<String, String> properties) {
        return new PropertiesFilterConfigurator(objectFactory, properties);
    }

    public void testSetsMimeTypes() {
        properties.put(PropertiesFilterConfigurator.MIME_TYPES_PARAM, "text/foo, application/x-stuff  \n foo/bar");
        propertiesConfigurator.configureCommon(builder);

        Selector selector = builder.getSelector();
        // TODO: Re-enable tests when Selector has been simplified.
        // assertArrayEquals(config.getMimeTypes(), "text/foo", "application/x-stuff", "foo/bar");
    }

    public void testSetsExcludePaths() {
        properties.put(PropertiesFilterConfigurator.EXCLUDE_PARAM, "/bad/*, *.BAD");
        propertiesConfigurator.configureCommon(builder);

        Selector selector = builder.getSelector();
        // TODO: Re-enable tests when Selector has been simplified.
        // assertTrue(config.shouldExclude("/bad/foo"));
        // assertTrue(config.shouldExclude("so.bad"));
        // assertFalse(config.shouldExclude("/good/foo"));
        // assertFalse(config.shouldExclude("so.good"));
    }

}
