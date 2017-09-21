package org.jerkar.tool.builtins.idea;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsThrowable;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by angibaudj on 14-03-17.
 */
class ModulesXmlGenerator {

    private static final String ENCODING = "UTF-8";

    private static final String T1 = "  ";

    private static final String T2 = T1 + T1;

    private static final String T3 = T2 + T1;

    private static final String T4 = T3 + T1;

    private final Iterable<File> imlFiles;

    private final File projectDir;

    private final File outputFile;

    public ModulesXmlGenerator(File projectDir, Iterable<File> imlFiles) {
        this.imlFiles = imlFiles;
        this.projectDir = projectDir;
        this.outputFile = new File(projectDir,".idea/modules.xml");
    }

    public void generate() {
        try {
            _generate();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private void _generate() throws IOException, XMLStreamException, FactoryConfigurationError {

        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fos, ENCODING);
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("project");
        writer.writeCharacters("\n" + T1);
        writer.writeStartElement("component");
        writer.writeAttribute("name", "ProjectModuleManager");
        writer.writeCharacters("\n" + T2);
        writer.writeStartElement("modules");
        writer.writeCharacters("\n");
        for (File iml : imlFiles) {
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
        outputFile.delete();
        JkUtilsFile.writeStringAtTop(outputFile, fos.toString(ENCODING));
    }

    private String path(File iml) {
        String relPath = JkUtilsFile.getRelativePath(this.projectDir, iml);
        return "$PROJECT_DIR$/" + relPath;
    }



}
