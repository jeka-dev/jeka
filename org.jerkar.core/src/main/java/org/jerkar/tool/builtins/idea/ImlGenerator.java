package org.jerkar.tool.builtins.idea;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.depmanagement.JkAttachedArtifacts;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkModuleDepFile;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkResolveResult;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.tool.JkConstants;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
final class ImlGenerator {

    private static final String ENCODING = "UTF-8";

    private static final String T1 = "  ";

    private static final String T2 = T1 + T1;

    private static final String T3 = T2 + T1;

    private static final String T4 = T3 + T1;

    private static final String T5 = T4 + T1;

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    private final File projectDir;

    /** default to projectDir/.classpath */
    public final File outputFile;

    /** attach javadoc to the lib dependencies */
    public boolean includeJavadoc;

    /** Used to generate JRE container */
    public String sourceJavaVersion;

    /** Can be empty but not null */
    public JkFileTreeSet sources = JkFileTreeSet.empty();

    /** Can be empty but not null */
    public JkFileTreeSet resources = JkFileTreeSet.empty();

    /** Can be empty but not null */
    public JkFileTreeSet testSources = JkFileTreeSet.empty();

    /** Can be empty but not null */
    public JkFileTreeSet testResources = JkFileTreeSet.empty();

    /** Dependency resolver to fetch module dependencies */
    public JkDependencyResolver dependencyResolver;

    /** Dependency resolver to fetch module dependencies for build classes */
    public JkDependencyResolver buildDefDependencyResolver;

    /** Can be empty but not null */
    public Iterable<File> projectDependencies = JkUtilsIterable.listOf();

    boolean forceJdkVersion;

    /**
     * Constructs a {@link ImlGenerator} from the project base directory
     */
    public ImlGenerator(File projectDir) {
        super();
        this.projectDir = projectDir;

        this.outputFile = new File(projectDir, projectDir.getName() + ".iml");
    }

    /** Generate the .classpath file */
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

        writeHead(writer);
        writeContent(writer);
        writeJdk(writer);
        writeSourceFolder(writer);
        writeModules(writer);
        writeLibs(writer, JkJavaBuild.COMPILE);
        writeLibs(writer, JkJavaBuild.PROVIDED);
        writeLibs(writer, JkJavaBuild.RUNTIME);
        writeTestLibs(writer);
        writeLocalLibs(writer, JkJavaBuild.COMPILE);
        writeLocalLibs(writer, JkJavaBuild.PROVIDED);
        writeLocalLibs(writer, JkJavaBuild.RUNTIME);
        writeLocalLibs(writer, JkJavaBuild.TEST);

        // Write end document
        writer.writeCharacters(T1);
        writer.writeEndElement();
        writer.writeCharacters("\n");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
        outputFile.delete();
        JkUtilsFile.writeStringAtTop(outputFile, fos.toString(ENCODING));
    }

    private static void writeHead(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("module");
        writer.writeAttribute("type", "JAVA_MODULE");
        writer.writeAttribute("version", "4");
        writer.writeCharacters("\n" + T1);
        writer.writeStartElement("component");
        writer.writeAttribute("name", "NewModuleRootManager");
        writer.writeAttribute("inherit-compiler-output", "true");
        writer.writeCharacters("\n");
        writer.writeCharacters(T2);
        writer.writeEmptyElement("exclude-output");
        writer.writeCharacters("\n");
    }

    private void writeContent(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeStartElement("content");
        writer.writeAttribute("url", "file://$MODULE_DIR$");
        writer.writeCharacters("\n");
        for (final JkFileTree fileTree : this.sources.fileTrees()) {
            if (fileTree.exists()) {
                writer.writeCharacters(T3);
                writer.writeEmptyElement("sourceFolder");
                final String path = JkUtilsFile.getRelativePath(projectDir, fileTree.root()).replace('\\', '/');
                writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
                writer.writeAttribute("isTestSource", "false");
                writer.writeCharacters("\n");
            }
        }
        for (final JkFileTree fileTree : this.testSources.fileTrees()) {
            if (fileTree.exists()) {
                writer.writeCharacters(T3);
                writer.writeEmptyElement("sourceFolder");
                final String path = JkUtilsFile.getRelativePath(projectDir, fileTree.root()).replace('\\', '/');
                writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
                writer.writeAttribute("isTestSource", "true");
                writer.writeCharacters("\n");
            }
        }
        for (final JkFileTree fileTree : this.resources.fileTrees()) {
            if (fileTree.exists()) {
                writer.writeCharacters(T3);
                writer.writeEmptyElement("sourceFolder");
                final String path = JkUtilsFile.getRelativePath(projectDir, fileTree.root()).replace('\\', '/');
                writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
                writer.writeAttribute("type", "java-resource");
                writer.writeCharacters("\n");
            }
        }
        for (final JkFileTree fileTree : this.testResources.fileTrees()) {
            if (fileTree.exists()) {
                writer.writeCharacters(T3);
                writer.writeEmptyElement("sourceFolder");
                final String path = JkUtilsFile.getRelativePath(projectDir, fileTree.root()).replace('\\', '/');
                writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
                writer.writeAttribute("type", "java-test-resource");
                writer.writeCharacters("\n");
            }
        }
        writer.writeCharacters(T3);
        writer.writeEmptyElement("sourceFolder");
        writer.writeAttribute("url", "file://$MODULE_DIR$/" + JkConstants.BUILD_DEF_DIR);
        writer.writeAttribute("isTestSource", "true");
        writer.writeCharacters("\n");
        writer.writeCharacters(T2);
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private void writeJdk(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeEmptyElement("orderEntry");
        if (this.forceJdkVersion) {
            writer.writeAttribute("type", "jdk");
            final String jdkVersion = jdkVesion(this.sourceJavaVersion);
            writer.writeAttribute("jdkName", jdkVersion);
            writer.writeAttribute("jdkType", "JavaSDK");
        } else {
            writer.writeAttribute("type", "inheritedJdk");
        }
        writer.writeCharacters("\n");
    }

    private void writeSourceFolder(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeEmptyElement("orderEntry");
        writer.writeAttribute("type", "sourceFolder");
        writer.writeAttribute("forTests", "false");
        writer.writeCharacters("\n");
    }

    private void writeModules(XMLStreamWriter writer) throws XMLStreamException {
        for (final File depProjectDir : projectDependencies) {
            writer.writeCharacters(T2);
            writer.writeEmptyElement("orderEntry");
            writer.writeAttribute("type", "module");
            writer.writeAttribute("module-name", depProjectDir.getName());
            writer.writeCharacters("\n");
        }
    }

    private void writeLibs(XMLStreamWriter writer, JkScope scope) throws XMLStreamException {
        final List<LibPath> libPaths = libPaths(dependencyResolver, scope);
        final String scopeName = scope == JkJavaBuild.COMPILE ? null : scope.name();
        writeLibs(writer, libPaths, scopeName);
    }

    private void writeTestLibs(XMLStreamWriter writer) throws XMLStreamException {
        final List<LibPath> libPaths = libPaths(dependencyResolver, JkJavaBuild.TEST);
        libPaths.addAll(libPaths(buildDefDependencyResolver));
        writeLibs(writer, libPaths, "TEST");
    }

    private void writeLocalLibs(XMLStreamWriter writer, JkScope scope) throws XMLStreamException {
        final List<File> binFiles = new LinkedList<File>();
        binFiles.addAll(dependencyResolver.dependenciesToResolve().fileSystemDependencies(scope).entries());
        if (scope.equals(JkJavaBuild.TEST)) {
            binFiles.addAll(buildDefDependencyResolver.dependenciesToResolve().localFileDependencies().entries());
        }
        final List<LibPath> libPaths = new LinkedList<ImlGenerator.LibPath>();
        for (final File file : binFiles) {
            final LibPath libPath = new LibPath();
            libPath.bin = file;
            final String name = JkUtilsString.substringBeforeLast(file.getName(), ".jar");
            File source = new File(file.getParentFile(), name + "-sources.jar");
            if (!source.exists()) {
                source = new File(file.getParentFile(), "../../libs-sources/" + name + "-sources.jar");
            }
            if (!source.exists()) {
                source = new File(file.getParentFile(), "libs-sources/" + name + "-sources.jar");
            }
            File javadoc = new File(file.getParentFile(), name + "-javadoc.jar");
            if (!javadoc.exists()) {
                javadoc = new File(file.getParentFile(), "../../libs-javadoc/" + name + "-javadoc.jar");
            }
            if (!javadoc.exists()) {
                javadoc = new File(file.getParentFile(), "libs-javadoc/" + name + "-javadoc.jar");
            }
            if (source.exists()) {
                libPath.source = source;
            }
            if (javadoc.exists()) {
                libPath.javadoc = javadoc;
            }
            libPaths.add(libPath);
        }
        writeLibs(writer, libPaths, scope.name());
    }

    private void writeLibs(XMLStreamWriter writer, List<LibPath> libPaths, String scopeName) throws XMLStreamException {
        for (final LibPath libPath : libPaths) {
            writer.writeCharacters(T2);
            writer.writeStartElement("orderEntry");
            writer.writeAttribute("type", "module-library");
            if (scopeName != null) {
                writer.writeAttribute("scope", scopeName.toUpperCase());
            }
            writer.writeCharacters("\n");
            writer.writeCharacters(T3);
            writer.writeStartElement("library");
            writer.writeCharacters("\n");
            writeLib(writer, "CLASSES", libPath.bin);
            writer.writeCharacters("\n");
            writeLib(writer, "JAVADOC", libPath.javadoc);
            writer.writeCharacters("\n");
            writeLib(writer, "SOURCES", libPath.source);
            writer.writeCharacters("\n" + T3);
            writer.writeEndElement();
            writer.writeCharacters("\n" + T2);
            writer.writeEndElement();
            writer.writeCharacters("\n");
        }
    }

    private void writeLib(XMLStreamWriter writer, String type, File file) throws XMLStreamException {
        writer.writeCharacters(T4);
        if (file != null) {
            writer.writeStartElement(type);
            writer.writeCharacters("\n");
            writer.writeCharacters(T5);
            writer.writeEmptyElement("root");
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
                final String relPath = JkUtilsFile.getRelativePath(projectDir, file);
                return "jar://$MODULE_DIR$/" + replacePathWithVar(relPath) + "!/";
            }
            final String path = JkUtilsFile.canonicalPath(file).replace('\\', '/');
            return "jar://" + replacePathWithVar(replacePathWithVar(path)) + "!/";
        }
        if (JkUtilsFile.isAncestor(projectDir, file)) {
            final String relPath = JkUtilsFile.getRelativePath(projectDir, file);
            return "file://$MODULE_DIR$/" + replacePathWithVar(relPath);
        }
        final String path = JkUtilsFile.canonicalPath(file).replace('\\', '/');
        return "file://" + replacePathWithVar(replacePathWithVar(path));

    }

    private static List<LibPath> libPaths(JkDependencyResolver dependencyResolver, JkScope... scopes) {
        if (!dependencyResolver.dependenciesToResolve().containsModules()) {
            return new LinkedList<ImlGenerator.LibPath>();
        }
        final JkResolveResult resolveResult = dependencyResolver.resolve(scopes);
        final JkAttachedArtifacts attachedArtifacts = dependencyResolver.getAttachedArtifacts(
                resolveResult.involvedModules(), JkJavaBuild.SOURCES, JkJavaBuild.JAVADOC);
        final List<LibPath> result = new LinkedList<LibPath>();
        for (final JkVersionedModule versionedModule : resolveResult.involvedModules()) {
            final LibPath libPath = new LibPath();
            final JkModuleId moduleId = versionedModule.moduleId();
            libPath.bin = resolveResult.filesOf(moduleId).get(0);
            final Set<JkModuleDepFile> attachedSources = attachedArtifacts.getArtifacts(moduleId, JkJavaBuild.SOURCES);
            if (!attachedSources.isEmpty()) {
                libPath.source = attachedSources.iterator().next().localFile();
            }
            final Set<JkModuleDepFile> attachedJavadoc = attachedArtifacts.getArtifacts(moduleId, JkJavaBuild.JAVADOC);
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

    private static String replacePathWithVar(String path) {
        final String repo = JkLocator.jerkarRepositoryCache().getAbsolutePath().replace('\\', '/');
        final String home = JkLocator.jerkarHome().getAbsolutePath().replace('\\', '/');
        final String result = path.replace(repo, "$JERKAR_REPO$");
        if (!result.equals(path)) {
            return result;
        }
        return path.replace(home, "$JERKAR_HOME$");
    }

}
