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
class Xml {

    private final Element element;

    /**
     * Wrap an existing Element.
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
     */
    public Xml(String xml) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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
     * Return the value of the named attribute. Returns null if this node does not exist, or the
     * attribute does not exist.
     */
    public String attribute(String name) {
        return element == null || !element.hasAttribute(name) ? null : element.getAttribute(name);
    }

    /**
     * Return the value of the named attribute. Returns defaultValue if this node does not exist, or the
     * attribute does not exist.
     */
    public String attribute(String name, String defaultValue) {
        return valueIfNotNull(attribute(name), defaultValue);
    }

    /**
     * Return text content of this element. Returns null if this node does not exist, or it does
     * not contain text.
     */
    public String text() {
        return element == null ? null : element.getTextContent();
    }
    
    /**
     * Return text content of this element. Returns defaultValue if this node does not exist, or it does
     * not contain text.
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
     */
    public List<Xml> children(String tagName) {
        if (element == null) {
            return Collections.emptyList();
        }
        NodeList nodeList = element.getChildNodes();
        List<Xml> result = new ArrayList<Xml>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getTagName().equals(tagName)) {
                    result.add(new Xml((Element) node));
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
     */
    public Xml child(String tagName) {
        if (element == null) {
            return new Xml();
        }
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getTagName().equals(tagName)) {
                    return new Xml((Element) node);
                }
            }
        }
        return new Xml();
    }

    /**
     * Whether this node actually exists in the tree.
     */
    public boolean exists() {
        return element != null;
    }

    private String valueIfNotNull(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
