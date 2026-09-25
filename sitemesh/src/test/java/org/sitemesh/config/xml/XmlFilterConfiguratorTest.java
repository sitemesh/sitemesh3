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

import jakarta.servlet.Filter;
import junit.framework.TestCase;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.webapp.WebEnvironment;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.StringReader;

public class XmlFilterConfiguratorTest extends TestCase {

    private static final String DECORATOR = "Decorated: <sitemesh:write property='title'/>";
    private static final String CONTENT = "<title>Hello world</title>";
    private static final String CONTENT_WITH_META =
            "<title>Hello world</title><meta name='decorator' content='my-decorator'>";

    public void testAppliesDecoratorToEveryPathInMapping() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", createFilter(
                        "<sitemesh>" +
                        "  <mapping>" +
                        "    <path>/a</path>" +
                        "    <path>/a/*</path>" +
                        "    <decorator>my-decorator</decorator>" +
                        "  </mapping>" +
                        "</sitemesh>"))
                .addStaticContent("/WEB-INF/decorators/my-decorator", "text/html", DECORATOR)
                .addStaticContent("/a", "text/html", CONTENT)
                .addStaticContent("/a/b", "text/html", CONTENT)
                .addStaticContent("/other", "text/html", CONTENT)
                .create();

        webEnvironment.doGet("/a");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
        webEnvironment.doGet("/a/b");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
        webEnvironment.doGet("/other");
        assertEquals(CONTENT, webEnvironment.getBody());
    }

    public void testExcludesEveryPathInMapping() throws Exception {
        // The meta tag would decorate the page if the exclusion were not applied,
        // so an undecorated body proves the path was excluded.
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", createFilter(
                        "<sitemesh>" +
                        "  <mapping path='/*' decorator='my-decorator'/>" +
                        "  <mapping exclude='true'>" +
                        "    <path>/a</path>" +
                        "    <path>/a/*</path>" +
                        "  </mapping>" +
                        "</sitemesh>"))
                .addStaticContent("/WEB-INF/decorators/my-decorator", "text/html", DECORATOR)
                .addStaticContent("/a", "text/html", CONTENT_WITH_META)
                .addStaticContent("/a/b", "text/html", CONTENT_WITH_META)
                .addStaticContent("/other", "text/html", CONTENT)
                .create();

        webEnvironment.doGet("/a");
        assertEquals(CONTENT_WITH_META, webEnvironment.getBody());
        webEnvironment.doGet("/a/b");
        assertEquals(CONTENT_WITH_META, webEnvironment.getBody());
        webEnvironment.doGet("/other");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
    }

    public void testExcludesPathAttribute() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", createFilter(
                        "<sitemesh>" +
                        "  <mapping path='/*' decorator='my-decorator'/>" +
                        "  <mapping path='/a/*' exclude='true'/>" +
                        "</sitemesh>"))
                .addStaticContent("/WEB-INF/decorators/my-decorator", "text/html", DECORATOR)
                .addStaticContent("/a/b", "text/html", CONTENT_WITH_META)
                .addStaticContent("/other", "text/html", CONTENT)
                .create();

        webEnvironment.doGet("/a/b");
        assertEquals(CONTENT_WITH_META, webEnvironment.getBody());
        webEnvironment.doGet("/other");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
    }

    public void testDecoratorSelectorWithoutPrefixTakesFullDecoratorPaths() throws Exception {
        // As it always has, a <decorator-selector> without <decorator-prefix> gets no prefix.
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", createFilter(
                        "<sitemesh>" +
                        "  <decorator-selector>org.sitemesh.config.MetaTagBasedDecoratorSelector</decorator-selector>" +
                        "  <mapping path='/*' decorator='/WEB-INF/decorators/my-decorator'/>" +
                        "</sitemesh>"))
                .addStaticContent("/WEB-INF/decorators/my-decorator", "text/html", DECORATOR)
                .addStaticContent("/other", "text/html", CONTENT)
                .create();

        webEnvironment.doGet("/other");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
    }

    public void testReadsPrettyPrintedValues() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", createFilter(
                        "<sitemesh>\n" +
                        "  <mime-type>\n    application/xhtml+xml\n  </mime-type>\n" +
                        "  <mapping>\n" +
                        "    <path>\n      /a/*\n    </path>\n" +
                        "    <decorator>\n      my-decorator\n    </decorator>\n" +
                        "  </mapping>\n" +
                        "  <mapping exclude='true'>\n" +
                        "    <path>\n      /a/excluded\n    </path>\n" +
                        "  </mapping>\n" +
                        "</sitemesh>"))
                .addStaticContent("/WEB-INF/decorators/my-decorator", "text/html", DECORATOR)
                .addStaticContent("/a/b", "application/xhtml+xml", CONTENT)
                .addStaticContent("/a/excluded", "application/xhtml+xml", CONTENT_WITH_META)
                .create();

        webEnvironment.doGet("/a/b");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
        webEnvironment.doGet("/a/excluded");
        assertEquals(CONTENT_WITH_META, webEnvironment.getBody());
    }

    public void testDecoratorAttributeAcceptsChain() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", createFilter(
                        "<sitemesh><mapping path='/*' decorator='inner, outer'/></sitemesh>"))
                .addStaticContent("/WEB-INF/decorators/inner", "text/html",
                        "<title>Inner <sitemesh:write property='title'/></title>")
                .addStaticContent("/WEB-INF/decorators/outer", "text/html", DECORATOR)
                .addStaticContent("/other", "text/html", CONTENT)
                .create();

        webEnvironment.doGet("/other");
        assertEquals("Decorated: Inner Hello world", webEnvironment.getBody());
    }

    public void testReadsConfigThatDeclaresSchemaNamespace() throws Exception {
        WebEnvironment webEnvironment = new WebEnvironment.Builder()
                .addFilter("/*", createFilter(
                        "<sitemesh xmlns='http://sitemesh.org/xml/config'" +
                        "          xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
                        "          xsi:schemaLocation='http://sitemesh.org/xml/config sitemesh3.xsd'>" +
                        "  <mapping path='/*' decorator='my-decorator'/>" +
                        "  <mapping path='/a/*' exclude='true'/>" +
                        "</sitemesh>"))
                .addStaticContent("/WEB-INF/decorators/my-decorator", "text/html", DECORATOR)
                .addStaticContent("/a/b", "text/html", CONTENT)
                .addStaticContent("/other", "text/html", CONTENT)
                .create();

        webEnvironment.doGet("/a/b");
        assertEquals(CONTENT, webEnvironment.getBody());
        webEnvironment.doGet("/other");
        assertEquals("Decorated: Hello world", webEnvironment.getBody());
    }

    private Filter createFilter(String xml) throws Exception {
        Element element = Xml.getSecureDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)))
                .getDocumentElement();
        SiteMeshFilterBuilder builder = new SiteMeshFilterBuilder();
        new XmlFilterConfigurator(new ObjectFactory.Default(), element).configureFilter(builder);
        return builder.create();
    }
}
