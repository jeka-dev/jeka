package dev.jeka.core.tool.builtins.intellij;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class ModulesXmlGenerator {

    private static final String ENCODING = "UTF-8";

    private static final String T1 = "  ";

    private static final String T2 = T1 + T1;

    private static final String T3 = T2 + T1;

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

    public Path outputFile() {
        return outputFile;
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
        for (final Path iml : imlFiles) {
            final Path relPath = projectDir.relativize(iml);
            JkLog.info("Iml file detected : " + relPath);
            final String path = path(relPath);
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
        JkUtilsPath.deleteIfExists(outputFile);
        JkUtilsPath.createDirectories(outputFile.getParent());
        Files.write(outputFile, baos.toByteArray());
    }

    private String path(Path relPath) {
        return "$PROJECT_DIR$/" + relPath.toString().replace('\\', '/');
    }



}
