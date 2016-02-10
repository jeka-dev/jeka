package org.jerkar.tool.builtins.idea;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.tool.JkConstants;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
final class ImlGenerator {

    private static final String ENCODING = "UTF-8";

    private static final String T1 = "\t";

    private static final String T2 = "\t\t";

    private static final String T3 = "\t\t\t";

    private static final String T4 = "\t\t\t\t";

    private static final String T5 = "\t\t\t\t\t";

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    private final File projectDir;

    /** default to projectDir/.classpath */
    public File outputFile;

    /** attach javadoc to the lib dependencies */
    public boolean includeJavadoc = true;

    /** Used to generate JRE container */
    public String sourceJavaVersion;

    /** Can be empty but not null */
    public JkFileTreeSet sources = JkFileTreeSet.empty();

    /** Can be empty but not null */
    public JkFileTreeSet testSources = JkFileTreeSet.empty();

    /** Dependency resolver to fetch module dependencies */
    public JkDependencyResolver dependencyResolver;

    /** Dependency resolver to fetch module dependencies for build classes */
    public JkDependencyResolver buildDefDependencyResolver;

    /** Can be empty but not null */
    public Iterable<File> projectDependencies = JkUtilsIterable.listOf();

    /**
     * Constructs a {@link ImlGenerator} from the project base
     * directory
     */
    public ImlGenerator(File projectDir) {
        super();
        this.projectDir = projectDir;
        this.outputFile = new File(projectDir, ".classpath");
    }


    /** Generate the .classpath file */
    public void generate() {
        try {
            _generate();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }


    void _generate() throws IOException, XMLStreamException, FactoryConfigurationError {

        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fos,
                ENCODING);

        writeHead(writer);
        writeContent(writer);
        writeJdk(writer);
        writeSourceFolder(writer);
        writeTestLibs(writer);
        writeLibs(writer, JkJavaBuild.COMPILE);


        // Write end document
        writer.writeEndElement();
        writer.writeCharacters("\n");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
        JkUtilsFile.writeStringAtTop(outputFile, fos.toString(ENCODING));
    }

    private static void writeHead(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("module");
        writer.writeAttribute("type", "JAVA_MODULE");
        writer.writeAttribute("version", "4");
        writer.writeCharacters("\n");
        writer.writeStartElement("component");
        writer.writeAttribute("name","NewModuleRootManager");
        writer.writeAttribute("inherit-compiler-output", "true");
        writer.writeCharacters("\n");
        writer.writeEmptyElement("exclude-output");
        writer.writeCharacters("\n");
    }

    private void writeContent(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("content");
        writer.writeAttribute("url", "file://$MODULE_DIR$");
        writer.writeCharacters("\n");
        for(JkFileTree fileTree : this.sources.fileTrees()) {
            writer.writeStartElement("sourceFolder");
            String path = JkUtilsFile.getRelativePath(projectDir, fileTree.root());
            writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
            writer.writeAttribute("isTestSource", "false");
            writer.writeCharacters("\n");
        }
        for(JkFileTree fileTree : this.testSources.fileTrees()) {writer.writeStartElement("sourceFolder");
            String path = JkUtilsFile.getRelativePath(projectDir, fileTree.root());
            writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
            writer.writeAttribute("isTestSource", "true");
            writer.writeCharacters("\n");
        }
        writer.writeStartElement("sourceFolder");
        writer.writeAttribute("url", "file://$MODULE_DIR$/" + JkConstants.BUILD_DEF_DIR);
        writer.writeAttribute("isTestSource", "true");
        writer.writeCharacters("\n");
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private void writeJdk(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("orderEntry");
        writer.writeAttribute("type", "jdk");
        writer.writeAttribute("forTests", "false");
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private void writeSourceFolder(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("orderEntry");
        writer.writeAttribute("type", "sourceFolder");
        String jdkVersion = jdkVesion(this.sourceJavaVersion == null ? "1.7" : this.sourceJavaVersion);
        writer.writeAttribute("jdkName", jdkVersion);
        writer.writeAttribute("jdkType","JavaSDK");
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private void writeLibs(XMLStreamWriter writer, JkScope scope) throws XMLStreamException {
        List<LibPath> libPaths = libPaths(dependencyResolver, scope);
        String scopeName = scope == JkJavaBuild.COMPILE ? null : scope.name();
        writeLibs(writer, libPaths, scopeName);
    }

    private void writeTestLibs(XMLStreamWriter writer) throws XMLStreamException {
        List<LibPath> libPaths = libPaths(dependencyResolver, JkJavaBuild.TEST);
        libPaths.addAll(libPaths(buildDefDependencyResolver));
        writeLibs(writer, libPaths, "TEST");
    }

    private void writeLibs(XMLStreamWriter writer, List<LibPath> libPaths, String scopeName) throws XMLStreamException {
        for (LibPath libPath: libPaths) {
            writer.writeCharacters(T2);
            writer.writeStartElement("orderEntry");
            writer.writeAttribute("type", "module-library");
            if (scopeName != null) {
                writer.writeAttribute("scope", scopeName);
            }
            writer.writeCharacters("\n");
            writer.writeCharacters(T3);
            writer.writeStartElement("library");
            writer.writeCharacters("\n");
            writeLib(writer, "CLASSES", libPath.bin);
            writer.writeCharacters("\n");
            writeLib(writer, "JAVADOC", libPath.bin);
            writer.writeCharacters("\n");
            writeLib(writer, "SOURCES", libPath.bin);
            writer.writeCharacters("\n");
            writer.writeCharacters("\n" + T3);
            writer.writeEndElement();
            writer.writeCharacters("\n" + T2);
            writer.writeEndElement();
        }
    }

    private void writeLib(XMLStreamWriter writer, String type, File file) throws XMLStreamException {
        writer.writeCharacters(T4);
        if (file != null) {
            writer.writeStartElement(type);
            writer.writeCharacters("\n");
            writer.writeCharacters(T5);
            writer.writeStartElement("root");
            writer.writeAttribute("url", ideaPath(this.projectDir, file));
            writer.writeCharacters("\n" + T4);
            writer.writeEndElement();
        } else {
            writer.writeEmptyElement(type);
        }
    }

    private static String ideaPath(File projectDir, File file) {
        if (file.getName().toLowerCase().endsWith(".jar")) {
            if (JkUtilsFile.isAncestor(projectDir, file)) {
                String relPath = JkUtilsFile.getRelativePath(projectDir, file);
                return "jar://$MODULE_DIR$/" + relPath + "!/";
            }
            return "jar://" + JkUtilsFile.canonicalPath(file);
        }
        throw new IllegalStateException("File type of " + file + " not handled. Only handle jar files.");
    }

    private static List<LibPath> libPaths(JkDependencyResolver dependencyResolver, JkScope... scopes) {
        final JkResolveResult resolveResult = dependencyResolver.resolve(scopes);
        final JkAttachedArtifacts attachedArtifacts = dependencyResolver.getAttachedArtifacts(
                resolveResult.involvedModules(), JkJavaBuild.SOURCES, JkJavaBuild.JAVADOC);
        final List<LibPath> result = new LinkedList<LibPath>();
        for (JkVersionedModule versionedModule : resolveResult.involvedModules()) {
            LibPath libPath = new LibPath();
            final JkModuleId moduleId = versionedModule.moduleId();
            libPath.bin = resolveResult.filesOf(moduleId).get(0);
            final Set<JkModuleDepFile> attachedSources = attachedArtifacts.getArtifacts(moduleId,JkJavaBuild.SOURCES);
            if (!attachedSources.isEmpty()) {
                libPath.source = attachedSources.iterator().next().localFile();
            }
            final Set<JkModuleDepFile> attachedJavadoc = attachedArtifacts.getArtifacts(moduleId,JkJavaBuild.JAVADOC);
            if (!attachedJavadoc.isEmpty()) {
                libPath.javadoc = attachedJavadoc.iterator().next().localFile();
            }
            result.add(libPath);
        }
        return result;
    }


    private static String jdkVesion(String compilerVersion) {
        if ("7".equals(compilerVersion)) {
            return "1.7";
        }
        if ("8".equals(compilerVersion)) {
            return "1.8";
        }
        if ("9".equals(compilerVersion)) {
            return "1.9";
        }
        return compilerVersion;
    }

    private static class LibPath {
        File bin;
        File source;
        File javadoc;
    }

}
