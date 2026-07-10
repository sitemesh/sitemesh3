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

package org.sitemesh.config.xml;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.StringReader;
import java.io.IOException;

/**
 * Really simple wrapper of org.w3c.node.Element to make life a little easier.
 *
 * <p>For convenience, the child() and children() methods will never return null - instead
 * they return Xml instances that represent missing nodes (Null Object Pattern).
 * This allows for things like <code>xml.child("some").child("node-that-might-not").child("exist").text()</code>
 * without having to do null checks for each intermediate element.</p>
 *
 * @author Joe Walnes
 */
public class Xml {

    private final Element element;

    /**
     * Wrap an existing Element.
     *
     * @param element element to wrap (may be null to represent a missing node)
     */
    public Xml(Element element) {
        this.element = element;
    }

    /**
     * Represent a missing node.
     */
    public Xml() {
        this.element = null;
    }

    /**
     * Parse a chunk of XML and wrap the root node.
     *
     * @param xml XML document as a string
     */
    public Xml(String xml) {
        try {
            DocumentBuilder builder = getSecureDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            this.element = document.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalArgumentException("Could not parse XML:\n" + xml, e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create a DocumentBuilder hardened against XML External Entity (XXE) attacks.
     *
     * @return a namespace-unaware DocumentBuilder with doctype declarations disallowed
     * @throws ParserConfigurationException if the underlying parser does not support the security features
     */
    public static DocumentBuilder getSecureDocumentBuilder() throws ParserConfigurationException {
        // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf.newDocumentBuilder();
    }

    /**
     * Return the value of the named attribute. Returns null if this node does not exist, or the
     * attribute does not exist.
     *
     * @param name attribute name
     * @return attribute value, or null
     */
    public String attribute(String name) {
        return element == null || !element.hasAttribute(name) ? null : element.getAttribute(name);
    }

    /**
     * Return the value of the named attribute. Returns defaultValue if this node does not exist, or the
     * attribute does not exist.
     *
     * @param name attribute name
     * @param defaultValue value to return if the attribute is not present
     * @return attribute value, or defaultValue
     */
    public String attribute(String name, String defaultValue) {
        return valueIfNotNull(attribute(name), defaultValue);
    }

    /**
     * Return text content of this element. Returns null if this node does not exist, or it does
     * not contain text.
     *
     * @return text content, or null
     */
    public String text() {
        return element == null ? null : element.getTextContent();
    }
    
    /**
     * Return text content of this element. Returns defaultValue if this node does not exist, or it does
     * not contain text.
     *
     * @param defaultValue value to return if there is no text content
     * @return text content, or defaultValue
     */
    public String text(String defaultValue) {
        return valueIfNotNull(text(), defaultValue);
    }

    /**
     * Get the child elements with a given tag name. This only include direct descendants - it will NOT
     * recursively search down the tree.
     *
     * <p>If no elements are found with the given name, an empty list is returned.
     * This method will NEVER return null.</p>
     *
     * @param tagName tag name of the child elements to find
     * @return list of matching child elements (never null)
     */
    public List<Xml> children(String tagName) {
        if (element == null) {
            return Collections.emptyList();
        }
        NodeList nodeList = element.getChildNodes();
        List<Xml> result = new ArrayList<Xml>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element element) {
                if (element.getTagName().equals(tagName)) {
                    result.add(new Xml(element));
                }
            }
        }
        return result;
    }

    /**
     * Get the first child element with a given tag name. This only include direct descendants - it will NOT
     * recursively search down the tree.
     *
     * <p>If no elements are found with the given name, an Xml instance is returned that represents
     * a missing node. This method will NEVER return null.</p>
     *
     * @param tagName tag name of the child element to find
     * @return first matching child element, or an Xml representing a missing node (never null)
     */
    public Xml child(String tagName) {
        if (element == null) {
            return new Xml();
        }
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element element) {
                if (element.getTagName().equals(tagName)) {
                    return new Xml(element);
                }
            }
        }
        return new Xml();
    }

    /**
     * Whether this node actually exists in the tree.
     *
     * @return true if this node exists, false if it represents a missing node
     */
    public boolean exists() {
        return element != null;
    }

    private String valueIfNotNull(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
