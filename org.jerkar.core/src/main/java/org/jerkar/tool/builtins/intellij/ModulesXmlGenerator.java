package org.jerkar.tool.builtins.intellij;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsThrowable;

class ModulesXmlGenerator {

    private static final String ENCODING = "UTF-8";

    private static final String T1 = "  ";

    private static final String T2 = T1 + T1;

    private static final String T3 = T2 + T1;

    private static final String T4 = T3 + T1;

    private final Iterable<Path> imlFiles;

    private final Path projectDir;

    private final Path outputFile;

    public ModulesXmlGenerator(Path projectDir, Iterable<Path> imlFiles) {
        this.imlFiles = imlFiles;
        this.projectDir = projectDir;
        this.outputFile = projectDir.resolve(".idea/modules.xml");
    }

    public void generate() {
        try {
            _generate();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private void _generate() throws IOException, XMLStreamException, FactoryConfigurationError {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(baos, ENCODING);
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("project");
        writer.writeCharacters("\n" + T1);
        writer.writeStartElement("component");
        writer.writeAttribute("name", "ProjectModuleManager");
        writer.writeCharacters("\n" + T2);
        writer.writeStartElement("modules");
        writer.writeCharacters("\n");
        for (Path iml : imlFiles) {
            String path = path(iml);
            writer.writeCharacters(T3);
            writer.writeEmptyElement("module");
            writer.writeAttribute("fileurl", "file://" + path);
            writer.writeAttribute("filepath", path);
            writer.writeCharacters("\n");
        }
        writer.writeCharacters(T2);
        writer.writeEndElement();
        writer.writeCharacters("\n" + T1);
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
        JkUtilsPath.deleteFile(outputFile);
        Files.write(outputFile, baos.toByteArray());
    }

    private String path(Path iml) {
        String relPath = iml.relativize(projectDir).toString();
        return "$PROJECT_DIR$/.idea/" + relPath;
    }



}
