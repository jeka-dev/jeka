package org.jerkar.api.utils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

    /**
     * Returns the direct child node of the specified element having specified name.
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
     * Returns specified element direct child node elements.
     */
    public static List<Element> directChildren(Element parent, String childName) {
        final List<Element> result = new LinkedList<Element>();
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
     * Returns the text of the specified direct child of the specified element.
     */
    public static String directChildText(Element parent, String childName) {
        final Element child = directChild(parent, childName);
        if (child == null) {
            return null;
        }
        return child.getTextContent();
    }

}
