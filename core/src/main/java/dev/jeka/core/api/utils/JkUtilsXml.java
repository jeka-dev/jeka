/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Utilities methods to ease XML api of the JDK
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsXml {

    private JkUtilsXml() {
        // Can't instantiate
    }

    /**
     * Creates an empty document.
     */
    public static Document createDocument() {
        final DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder icBuilder;
        try {
            icBuilder = icFactory.newDocumentBuilder();
            return icBuilder.newDocument();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Creates a document from the specified file.
     */
    public static Document documentFrom(File documentFile) {
        if (!documentFile.exists()) {
            throw new IllegalStateException(documentFile.getAbsolutePath() + " file not found.");
        }
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();  //NOSONAR
            Document doc;
            doc = dBuilder.parse(documentFile);
            doc.getDocumentElement().normalize();
            return doc;
        } catch (final Exception e) {
            throw new RuntimeException("Error while parsing file " + documentFile.getPath(), e);
        }
    }

    public static Document documentFrom(Path documentFile) {
        if (!Files.exists(documentFile)) {
            throw new IllegalStateException(documentFile.toAbsolutePath().normalize() + " file not found.");
        }
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();  //NOSONAR
            Document doc;
            doc = dBuilder.parse(documentFile.toFile());
            doc.getDocumentElement().normalize();
            return doc;
        } catch (final Exception e) {
            throw new RuntimeException("Error while parsing file " + documentFile, e);
        }
    }

    /**
     * Creates a document from the specified xml string.
     */
    public static Document documentFrom(String xml) {
        return documentFrom(new StringReader(xml));
    }

    /**
     * Creates a document from the specified xml string.
     */
    public static Document documentFrom(Reader reader) {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();  //NOSONAR
            Document doc;
            doc = dBuilder.parse(new InputSource(reader));
            doc.getDocumentElement().normalize();
            return doc;
        } catch (final Exception e) {
            throw new RuntimeException("Error while parsing xml", e);
        }
    }

    /**
     * Returns the direct getChild node of the specified element having specified
     * name.
     */
    public static Element directChild(Node parent, String childName) {
        final NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (node instanceof Element) {
                final Element element = (Element) node;
                if (element.getTagName().equals(childName)) {
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * Returns specified element direct getChild node elements.
     */
    public static List<Element> directChildren(Element parent, String childName) {
        final List<Element> result = new LinkedList<>();
        final NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (node instanceof Element) {
                final Element element = (Element) node;
                if (element.getTagName().equals(childName)) {
                    result.add(element);
                }
            }
        }
        return result;
    }

    /**
     * Returns the text of the specified direct getChild of the specified element.
     */
    public static String directChildText(Element parent, String childName) {
        final Element child = directChild(parent, childName);
        if (child == null) {
            return null;
        }
        return child.getTextContent();
    }

    /**
     * Prints the specified document in the specified output getOutputStream.
     * The output is indented.
     */
    public static void output(Document doc, OutputStream outputStream) {
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();  //NOSONAR
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            final DOMSource source = new DOMSource(doc);
            final StreamResult console = new StreamResult(outputStream);
            transformer.transform(source, console);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Finds and returns the first direct child element within the provided NodeList
     * that matches the specified tag name.
     *
     * @param nodeList the NodeList to iterate through for searching direct child elements
     * @param tagName the name of the tag to search for among the elements within the NodeList
     * @return the first Element with the matching tag name if found, otherwise null
     */
    public static Element findDirectChild(NodeList nodeList, String tagName) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (node instanceof Element) {
                final Element element = (Element) node;
                if (element.getTagName().equals(tagName)) {
                    return element;
                }
            }
        }
        return null;
    }
}
