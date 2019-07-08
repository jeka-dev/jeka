package dev.jeka.core.api.tooling.eclipse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsThrowable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Memory model of Eclipse .project file.
 */
public final class JkEclipseProjectGenerator {

    private static JkEclipseProjectGenerator of(Path dotProjectFile) {
        final Document document = getDotProjectAsDom(dotProjectFile);
        return from(document);
    }

    public static JkEclipseProjectGenerator ofJavaNature(String name) {
        return new JkEclipseProjectGenerator(name, JkUtilsIterable.setOf("org.eclipse.jdt.core.javanature"));
    }

    public static JkEclipseProjectGenerator ofSimpleNature(String name) {
        return new JkEclipseProjectGenerator(name, new HashSet<>());
    }

    static Map<String, Path> findProjectPath(Path parent) {
        final Map<String, Path> map = new HashMap<>();
        for (final Path file : JkUtilsPath.listDirectChildren(parent)) {
            final Path dotProject = file.resolve(".project");
            if (!Files.exists(dotProject)) {
                continue;
            }
            final JkEclipseProjectGenerator project = JkEclipseProjectGenerator.of(dotProject);
            map.put(project.name, file);
        }
        return map;
    }

    final String name;

    final Set<String> natures;

    boolean hasJavaNature() {
        return natures.contains("org.eclipse.jdt.core.javanature");
    }

    public void writeTo(Path dotProjectFile) {
        try {
            writeToFile(dotProjectFile);
        } catch (FactoryConfigurationError | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeToFile(Path dotProjectFile) throws XMLStreamException, FactoryConfigurationError {
        try(OutputStream fos = Files.newOutputStream(dotProjectFile)) {
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fos,
                    "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement("projectDescription");
            writer.writeCharacters("\n\t");
            writer.writeStartElement("name");
            writer.writeCharacters(name);
            writer.writeEndElement();
            writer.writeCharacters("\n\t");
            writer.writeEmptyElement("comment");
            writer.writeCharacters("\n\t");
            writer.writeEmptyElement("projects");
            writer.writeCharacters("\n\t");
            writer.writeStartElement("buildSpec");
            writer.writeCharacters("\n\t\t");
            writer.writeStartElement("buildCommand");
            writer.writeCharacters("\n\t\t\t");
            writer.writeStartElement("name");
            writer.writeCharacters("org.eclipse.jdt.core.javabuilder");
            writer.writeEndElement();
            writer.writeCharacters("\n\t\t\t");
            writer.writeEmptyElement("arguments");
            writer.writeCharacters("\n\t\t");
            writer.writeEndElement();
            writer.writeCharacters("\n\t");
            writer.writeEndElement();
            writer.writeCharacters("\n\t");
            writer.writeStartElement("natures");
            for (final String nature : natures) {
                writer.writeCharacters("\n\t\t");
                writer.writeStartElement("nature");
                writer.writeCharacters(nature);
                writer.writeEndElement();
            }
            writer.writeCharacters("\n\t");
            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndElement();

            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private JkEclipseProjectGenerator(String name, Set<String> natures) {
        this.name = name;
        this.natures = Collections.unmodifiableSet(natures);
    }

    private static JkEclipseProjectGenerator from(Document document) {
        final NodeList nodeList = document.getElementsByTagName("name");
        final Node node = nodeList.item(0);
        final String name = node.getTextContent();
        final Set<String> natures = new HashSet<>();
        final NodeList natureNodes = document.getElementsByTagName("nature");
        for (int i = 0; i < natureNodes.getLength(); i++) {
            natures.add(natureNodes.item(i).getTextContent());
        }
        return new JkEclipseProjectGenerator(name, natures);
    }

    private static Document getDotProjectAsDom(Path dotProjectFile) {
        if (!Files.exists(dotProjectFile)) {
            throw new IllegalStateException(dotProjectFile + " file not found.");
        }
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            doc = dBuilder.parse(dotProjectFile.toFile());
            doc.getDocumentElement().normalize();
            return doc;
        } catch (final Exception e) {
            throw new RuntimeException("Error while parsing .classpath file : ", e);
        }
    }

}
