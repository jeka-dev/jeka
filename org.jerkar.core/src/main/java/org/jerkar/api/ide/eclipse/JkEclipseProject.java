package org.jerkar.api.ide.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
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

import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Memory model of Eclipse .project file.
 */
public final class JkEclipseProject {

    public static JkEclipseProject of(File dotProjectFile) {
        final Document document = getDotProjectAsDom(dotProjectFile);
        return from(document);
    }

    public static JkEclipseProject ofJavaNature(String name) {
        return new JkEclipseProject(name, JkUtilsIterable.setOf("org.eclipse.jdt.core.javanature"));
    }

    public static JkEclipseProject ofSimpleNature(String name) {
        return new JkEclipseProject(name, new HashSet<>());
    }


    public static JkEclipseProject findProjectNamed(File parent, String projectName) {
        for (final File file : parent.listFiles()) {
            final File dotProject = new File(file, ".project");
            if (!(dotProject.exists())) {
                continue;
            }
            final JkEclipseProject project = JkEclipseProject.of(dotProject);
            if (projectName.equals(project.name)) {
                return project;
            }
        }
        return null;
    }

    @Deprecated
    public static Map<String, File> findProjects(File parent) {
        final Map<String, File> map = new HashMap<>();
        for (final File file : parent.listFiles()) {
            final File dotProject = new File(file, ".project");
            if (!(dotProject.exists())) {
                continue;
            }
            final JkEclipseProject project = JkEclipseProject.of(dotProject);
            map.put(project.name, file);
        }
        return map;
    }

    public static Map<String, Path> findProjectPath(Path parent) {
        final Map<String, Path> map = new HashMap<>();
        for (final Path file : JkUtilsPath.listDirectChildren(parent)) {
            final Path dotProject = file.resolve(".project");
            if (!Files.exists(dotProject)) {
                continue;
            }
            final JkEclipseProject project = JkEclipseProject.of(dotProject.toFile());
            map.put(project.name, file);
        }
        return map;
    }

    final String name;

    final Set<String> natures;

    boolean hasJavaNature() {
        return natures.contains("org.eclipse.jdt.core.javanature");
    }

    @Deprecated
    public void writeTo(File dotProjectFile) {
        try {
            writeToFile(dotProjectFile.toPath());
        } catch (final FileNotFoundException | FactoryConfigurationError | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeTo(Path dotProjectFile) {
        try {
            writeToFile(dotProjectFile);
        } catch (final FileNotFoundException | FactoryConfigurationError | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeToFile(Path dotProjectFile) throws XMLStreamException,
    FactoryConfigurationError, FileNotFoundException {
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

    private JkEclipseProject(String name, Set<String> natures) {
        this.name = name;
        this.natures = Collections.unmodifiableSet(natures);
    }

    private static JkEclipseProject from(Document document) {
        final NodeList nodeList = document.getElementsByTagName("name");
        final Node node = nodeList.item(0);
        final String name = node.getTextContent();
        final Set<String> natures = new HashSet<>();
        final NodeList natureNodes = document.getElementsByTagName("nature");
        for (int i = 0; i < natureNodes.getLength(); i++) {
            natures.add(natureNodes.item(i).getTextContent());
        }
        return new JkEclipseProject(name, natures);
    }

    private static Document getDotProjectAsDom(File dotProjectFile) {
        if (!dotProjectFile.exists()) {
            throw new IllegalStateException(dotProjectFile.getAbsolutePath() + " file not found.");
        }
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            doc = dBuilder.parse(dotProjectFile);
            doc.getDocumentElement().normalize();
            return doc;
        } catch (final Exception e) {
            throw new RuntimeException("Error while parsing .classpath file : ", e);
        }
    }

}
