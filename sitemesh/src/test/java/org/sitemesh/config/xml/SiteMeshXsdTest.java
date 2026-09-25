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
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;

/**
 * Checks that sitemesh3.xsd accepts every element and attribute the XML configurators read.
 * Keep the samples here in step with {@link XmlConfigurator}, {@link XmlFilterConfigurator}
 * and {@link XmlOfflineConfigurator}.
 */
public class SiteMeshXsdTest extends TestCase {

    private static final String OPEN = "<sitemesh xmlns='http://sitemesh.org/xml/config'>";
    private static final String CLOSE = "</sitemesh>";

    private Schema schema;

    @Override
    protected void setUp() throws Exception {
        schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(getClass().getResource("/org/sitemesh/sitemesh3.xsd"));
    }

    public void testAcceptsEveryFilterSettingInAnyOrder() throws Exception {
        assertValid(OPEN +
                "  <mapping path='/*' decorator='default.html'/>" +
                "  <mime-type>text/html</mime-type>" +
                "  <decorator-prefix>/decorators/</decorator-prefix>" +
                "  <mapping path='/admin/*' decorator='admin.html'/>" +
                "  <mime-type>application/xhtml+xml</mime-type>" +
                "  <decorator-selector>org.sitemesh.config.PathBasedDecoratorSelector</decorator-selector>" +
                "  <include-error-pages>true</include-error-pages>" +
                "  <dispatch-mode>include</dispatch-mode>" +
                "  <content-processor>" +
                "    <tag-rule-bundle class='com.example.MyBundle'/>" +
                "    <tag-rule-bundle class='com.example.OtherBundle'/>" +
                "  </content-processor>" +
                "  <mapping path='/assets/*' exclude='true'/>" +
                CLOSE);
    }

    public void testAcceptsMappingChildElements() throws Exception {
        assertValid(OPEN +
                "  <mapping>" +
                "    <path>/articles</path>" +
                "    <path>/articles/*</path>" +
                "    <decorator>article.html</decorator>" +
                "    <decorator>common.html</decorator>" +
                "  </mapping>" +
                "  <mapping>" +
                "    <path>/javadoc</path>" +
                "    <exclude/>" +
                "    <path>/javadoc/*</path>" +
                "  </mapping>" +
                "  <mapping decorator='default.html'/>" +
                CLOSE);
    }

    public void testAcceptsCustomContentProcessor() throws Exception {
        assertValid(OPEN + "<content-processor class='com.example.MyContentProcessor'/>" + CLOSE);
    }

    public void testAcceptsOfflineDirectories() throws Exception {
        assertValid("<sitemesh xmlns='http://sitemesh.org/xml/config' source-dir='src' destination-dir='dest'>" +
                "  <mapping path='/*' decorator='/decorators/main.html'/>" +
                CLOSE);
    }

    public void testAcceptsEmptyConfig() throws Exception {
        assertValid(OPEN + CLOSE);
    }

    public void testRejectsUnknownElement() {
        assertInvalid(OPEN + "<decorators-prefix>/decorators/</decorators-prefix>" + CLOSE);
    }

    public void testRejectsUnknownDispatchMode() {
        assertInvalid(OPEN + "<dispatch-mode>redirect</dispatch-mode>" + CLOSE);
    }

    public void testRejectsExcludeElementWithContent() {
        // <exclude> excludes by its presence, so <exclude>false</exclude> would still exclude.
        assertInvalid(OPEN + "<mapping><path>/a/*</path><exclude>false</exclude></mapping>" + CLOSE);
    }

    private void assertValid(String xml) throws Exception {
        schema.newValidator().validate(new StreamSource(new StringReader(xml)));
    }

    private void assertInvalid(String xml) {
        try {
            assertValid(xml);
            fail("Expected schema validation to fail for: " + xml);
        } catch (SAXException expected) {
            // expected
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
