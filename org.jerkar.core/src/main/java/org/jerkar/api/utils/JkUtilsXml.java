package org.jerkar.api.utils;

import java.io.File;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
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
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
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
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            doc = dBuilder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();
            return doc;
        } catch (final Exception e) {
            throw new RuntimeException("Error while parsing xml \n" + xml, e);
        }
    }

    /**
     * Returns the direct getChild node of the specified element having specified
     * name.
     */
    public static Element directChild(Element parent, String childName) {
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
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            final DOMSource source = new DOMSource(doc);
            final StreamResult console = new StreamResult(outputStream);
            transformer.transform(source, console);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }
}
