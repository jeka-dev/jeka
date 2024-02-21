package dev.jeka.core.tool.builtins.tooling.ide;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class IntelliJProject {

    private static final String ENCODING = "UTF-8";

    private static final String T1 = "  ";

    private static final String T2 = T1 + T1;

    private static final String T3 = T2 + T1;

    private final Path rootDir;

    private IntelliJProject(Path rootDir) {
        this.rootDir = rootDir;
    }

    public static IntelliJProject find(Path from) {
        from = from.toAbsolutePath();
        if (isProjectRooDir(from)) {
            JkLog.verbose("Intellij Parent found at %s", from);
            return new IntelliJProject(from);
        }
        if (from.getParent() == null) {
            throw new IllegalStateException("No Intellij project can be found from "
                    + from.normalize().toAbsolutePath());
        }
        return find(from.getParent());
    }

    public IntelliJProject deleteWorkspaceXml() {
        JkUtilsPath.deleteIfExists(rootDir.resolve(".idea/workspace.xml"));
        return this;
    }

    public Path getModulesXmlPath() {
        return rootDir.resolve(".idea/modules.xml");
    }

    /**
     * Regenerates the modules.xml file based on the .iml files found in the project  root directory and its subdirectories.
     */
    public void regenerateModulesXml() {
        List<Path> imlFiles = findImlFiles();
        generateModulesXml(imlFiles);
    }

    /**
     * Finds all .iml files in the specified root directory and its subdirectories.
     */
    public List<Path> findImlFiles() {
        return JkPathTree.of(rootDir).andMatching(true, "**.iml").getFiles();
    }

    /**
     * Generates an XML file containing module information based on the provided .iml file paths.
     *
     * @param imlPaths A collection of .iml file paths, or a single path.
     */
    public IntelliJProject generateModulesXml(Iterable<Path> imlPaths) {
        try {
            List<Path> imlFiles =JkUtilsPath.disambiguate(imlPaths);
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
                final Path relPath = rootDir.toAbsolutePath().normalize()
                        .relativize(iml.toAbsolutePath().normalize());
                JkLog.info("Iml file detected : " + relPath);
                final String path = "$PROJECT_DIR$/" + relPath.toString().replace('\\', '/');
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
            Path outputFile = getModulesXmlPath();
            JkUtilsPath.deleteIfExists(outputFile);
            JkUtilsPath.createDirectories(outputFile.getParent());
            Files.write(outputFile, baos.toByteArray());
            return this;
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private static boolean isProjectRooDir(Path candidate) {
        if (Files.exists(candidate.resolve(".idea/workspace.xml"))) {
            return true;
        }
        if (Files.exists(candidate.resolve(".idea/modules.xml"))) {
            return true;
        }
        return Files.exists(candidate.resolve(".idea/vcs.xml"));
    }
}
