package org.jerkar.tool.builtins.idea;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.depmanagement.*;
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

    private final File projectDir;

    /** default to projectDir/.classpath */
    final File outputFile;

    /** attach javadoc to the lib dependencies */
    boolean includeJavadoc;

    /** Used to generate JRE container */
    String sourceJavaVersion;

    /** Can be empty but not null */
    JkFileTreeSet sources = JkFileTreeSet.empty();

    /** Can be empty but not null */
    private JkFileTreeSet resources = JkFileTreeSet.empty();

    File outputClassFolder;

    File outputTestClassFolder;

    /** Can be empty but not null */
    JkFileTreeSet testSources = JkFileTreeSet.empty();

    /** Can be empty but not null */
    private JkFileTreeSet testResources = JkFileTreeSet.empty();

    /** Dependency resolver to fetch module dependencies */
    public JkDependencyResolver dependencyResolver;

    public JkDependencies dependencies;

    /** Dependency resolver to fetch module dependencies for build classes */
    JkDependencyResolver buildDefDependencyResolver;

    JkDependencies buildDependencies;

    /** Can be empty but not null */
    Iterable<File> projectDependencies = JkUtilsIterable.listOf();

    boolean forceJdkVersion;

    /* When true, path will be mentioned with $JERKAR_HOME$ and $JERKAR_REPO$ instead of explicit absolute path. */
    boolean useVarPath;

    private final Set<String> paths = new HashSet<>();

    private final ByteArrayOutputStream fos = new ByteArrayOutputStream();

    private final XMLStreamWriter writer = createWriter(fos);

    /**
     * Constructs a {@link ImlGenerator} to the project base directory
     */
    ImlGenerator(File projectDir) {
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
        writeHead();
        writeOutput();
        writeJdk();
        writeContent();
        writeOrderEntrySourceFolder();
        Set<File> allPaths = new HashSet<>();
        Set<File> allModules = new HashSet<>();
        if (this.dependencyResolver != null) {
            writeDependencies(dependencies, this.dependencyResolver, allPaths, allModules, false);
        }
        if (this.buildDefDependencyResolver != null) {
            writeDependencies(this.buildDependencies, this.buildDefDependencyResolver, allPaths, allModules, true);
        }
        writeBuildProjectDependencies(allModules);

        writeFoot();
        outputFile.delete();
        JkUtilsFile.writeStringAtTop(outputFile, fos.toString(ENCODING));
    }

    private void writeHead() throws XMLStreamException {
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("module");
        writer.writeAttribute("type", "JAVA_MODULE");
        writer.writeAttribute("version", "4");
        writer.writeCharacters("\n" + T1);
        writer.writeStartElement("component");
        writer.writeAttribute("name", "NewModuleRootManager");
        writer.writeAttribute("inherit-compiler-output", "false");
        writer.writeCharacters("\n");
    }

    private void writeFoot() throws XMLStreamException {
        writer.writeCharacters(T1);
        writer.writeEndElement();
        writer.writeCharacters("\n");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    private void writeOutput() throws XMLStreamException {

        // TODO should get location to #outputClassFolder and #outputTestClassFolder
        writer.writeCharacters(T2);
        writer.writeEmptyElement("output");
        writer.writeAttribute("url", "file://$MODULE_DIR$/build/output/classes");
        writer.writeCharacters("\n");
        writer.writeCharacters(T2);
        writer.writeEmptyElement("output-test");
        writer.writeAttribute("url", "file://$MODULE_DIR$/build/output/test-classes");
        writer.writeCharacters("\n");
        writer.writeCharacters(T2);
        writer.writeEmptyElement("exclude-output");
        writer.writeCharacters("\n");
    }

    private void writeContent() throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeStartElement("content");
        writer.writeAttribute("url", "file://$MODULE_DIR$");
        writer.writeCharacters("\n");

        // Write build sources
        writer.writeCharacters(T3);
        writer.writeEmptyElement("sourceFolder");
        writer.writeAttribute("url", "file://$MODULE_DIR$/" + JkConstants.BUILD_DEF_DIR);
        writer.writeAttribute("isTestSource", "true");
        writer.writeCharacters("\n");
        writer.writeCharacters(T2);

        // Write test sources
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

        // write test resources
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

        // Write production sources
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

        // Write production test resources
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

        writer.writeCharacters(T3);
        writer.writeEmptyElement("excludeFolder");
        final String path = JkConstants.BUILD_OUTPUT_PATH;
        writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
        writer.writeAttribute("isTestSource", "false");
        writer.writeCharacters("\n");
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private void writeBuildProjectDependencies(Set<File> allModules) throws XMLStreamException {
        for (File rootFolder : this.projectDependencies) {
            if (!allModules.contains(rootFolder)) {
                writeOrderEntryForModule(rootFolder.getName(), "COMPILE");
                allModules.add(rootFolder);
            }
        }
    }

    private void writeDependencies(JkDependencies dependencies, JkDependencyResolver resolver, Set<File> allPaths, Set<File> allModules,
                                   boolean forceTest) throws XMLStreamException {

        final JkResolveResult resolveResult = resolver.resolve(dependencies);
        final JkDependencyNode tree = resolveResult.dependencyTree();
        for (final JkDependencyNode node : tree.flatten()) {

            // Maven dependency
            if (node.isModuleNode()) {
                String ideScope = forceTest ? "TEST" : ideScope(node.moduleInfo().resolvedScopes());
                final List<LibPath> paths = toLibPath(node.moduleInfo(), resolver.repositories(), ideScope);
                for (LibPath libPath : paths) {
                    if (!allPaths.contains(libPath.bin)) {
                        writeOrderEntryForLib(libPath);
                        allPaths.add(libPath.bin);
                    }
                }

                // File dependencies (file system + computed)
            } else {
                String ideScope = forceTest ? "TEST" : ideScope(node.nodeInfo().declaredScopes());
                JkDependencyNode.FileNodeInfo fileNodeInfo = (JkDependencyNode.FileNodeInfo) node.nodeInfo();
                if (fileNodeInfo.isComputed()) {
                    final File projectDir = getProjectFolderOf(fileNodeInfo.files(), this.projectDependencies);
                    if (projectDir != null && !allModules.contains(projectDir)) {
                        writeOrderEntryForModule(projectDir.getName(), ideScope);
                        allModules.add(projectDir);
                    }
                } else {
                    writeFileEntries(fileNodeInfo.files(), paths, ideScope);
                }
            }
        }
    }

    private void writeFileEntries(Iterable<File> files, Set<String> paths, String ideScope) throws XMLStreamException {
        for (File file : files) {
            LibPath libPath = new LibPath();
            libPath.bin = file;
            libPath.scope = ideScope;
            libPath.source = lookForSources(file);
            libPath.javadoc = lookForJavadoc(file);
            writeOrderEntryForLib(libPath);
            paths.add(file.getPath());
        }
    }

    private List<LibPath> toLibPath(JkDependencyNode.ModuleNodeInfo moduleInfo, JkRepos repos,
                                    String scope) {
        final List<LibPath> result = new LinkedList<>();
        final JkModuleId moduleId = moduleInfo.moduleId();
        final JkVersion version = moduleInfo.resolvedVersion();
        final JkVersionedModule versionedModule = JkVersionedModule.of(moduleId, version);
        final List<File> files = moduleInfo.files();
        for (File file : files) {
            LibPath libPath = new LibPath();
            libPath.bin = file;
            libPath.scope = scope;
            libPath.source = repos.get(JkModuleDependency.of(versionedModule).classifier("sources"));
            libPath.javadoc = repos.get(JkModuleDependency.of(versionedModule).classifier("javadoc"));
            result.add(libPath);
        }
        return result;
    }

    private static Set<String> toStringScopes(Set<JkScope> scopes) {
        Set<String> result = new HashSet<>();
        for (JkScope scope : scopes) {
            result.add(scope.name());
        }
        return result;
    }

    private static String ideScope(Set<JkScope> scopesArg) {
        Set<String> scopes = toStringScopes(scopesArg);
        if (scopes.contains(JkJavaBuild.COMPILE.name())) {
            return "COMPILE";
        }
        if (scopes.contains(JkJavaBuild.PROVIDED.name())) {
            return "PROVIDED";
        }
        if (scopes.contains(JkJavaBuild.RUNTIME.name())) {
            return "RUNTIME";
        }
        if (scopes.contains(JkJavaBuild.TEST.name())) {
            return "TEST";
        }
        return "COMPILE";
    }

    private void writeJdk() throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeEmptyElement("orderEntry");
        if (this.forceJdkVersion  && this.sourceJavaVersion != null) {
            writer.writeAttribute("type", "jdk");
            final String jdkVersion = jdkVesion(this.sourceJavaVersion);
            writer.writeAttribute("jdkName", jdkVersion);
            writer.writeAttribute("jdkType", "JavaSDK");
        } else {
            writer.writeAttribute("type", "inheritedJdk");
        }
        writer.writeCharacters("\n");
    }

    private void writeOrderEntrySourceFolder() throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeEmptyElement("orderEntry");
        writer.writeAttribute("type", "sourceFolder");
        writer.writeAttribute("forTests", "false");
        writer.writeCharacters("\n");
    }

    private void writeOrderEntryForLib(LibPath libPath) throws XMLStreamException {
            writer.writeCharacters(T2);
            writer.writeStartElement("orderEntry");
            writer.writeAttribute("type", "module-library");
            if (libPath.scope != null) {
                writer.writeAttribute("scope", libPath.scope);
            }
            writer.writeAttribute("exported", "");
            writer.writeCharacters("\n");
            writer.writeCharacters(T3);
            writer.writeStartElement("library");
            writer.writeCharacters("\n");
            writeLibType("CLASSES", libPath.bin);
            writer.writeCharacters("\n");
            writeLibType("JAVADOC", libPath.javadoc);
            writer.writeCharacters("\n");
            writeLibType("SOURCES", libPath.source);
            writer.writeCharacters("\n" + T3);
            writer.writeEndElement();
            writer.writeCharacters("\n" + T2);
            writer.writeEndElement();
            writer.writeCharacters("\n");
    }

    private void writeOrderEntryForModule(String ideaModuleName, String scope) throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeEmptyElement("orderEntry");
        writer.writeAttribute("type", "module");
        if (scope != null) {
            writer.writeAttribute("scope", scope);
        }
        writer.writeAttribute("module-name", ideaModuleName);
        writer.writeAttribute("exported", "");
        writer.writeCharacters("\n");
    }

    private void writeLibType(String type, File file) throws XMLStreamException {
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

    private String ideaPath(File projectDir, File file) {
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


    private static String jdkVesion(String compilerVersion) {
        if ("4".equals(compilerVersion)) {
            return "1.4";
        }
        if ("5".equals(compilerVersion)) {
            return "1.5";
        }
        if ("6".equals(compilerVersion)) {
            return "1.6";
        }
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
        String scope;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LibPath libPath = (LibPath) o;

            return bin.equals(libPath.bin);
        }

        @Override
        public int hashCode() {
            return bin.hashCode();
        }
    }

    private String replacePathWithVar(String path) {
        if (!useVarPath) {
            return path;
        }
        final String repo = JkLocator.jerkarRepositoryCache().getAbsolutePath().replace('\\', '/');
        final String home = JkLocator.jerkarHome().getAbsolutePath().replace('\\', '/');
        final String result = path.replace(repo, "$JERKAR_REPO$");
        if (!result.equals(path)) {
            return result;
        }
        return path.replace(home, "$JERKAR_HOME$");
    }

    private static XMLStreamWriter createWriter(ByteArrayOutputStream fos) {
        try {
            return XMLOutputFactory.newInstance().createXMLStreamWriter(fos, ENCODING);
        } catch (XMLStreamException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /*
     * If the specified folder is the output folder of an eclipse project than it returns the asScopedDependency of this project,
     * else otherwise.
     */
    private static File getProjectFolderOf(Iterable<File> files, Iterable<File> projectDependencies) {
        if (!files.iterator().hasNext()) {
            return null;
        }
        File folder = files.iterator().next().getParentFile();
        while (folder != null) {
            if (JkFileTree.of(folder).include("*.iml").fileCount(false) == 1) {
                return folder;
            }
            if (JkUtilsIterable.listOf(projectDependencies).contains(folder)) {
                return folder;
            }
            folder = folder.getParentFile();
        }
        return null;
    }

    private File lookForSources(File binary) {
        String name = binary.getName();
        String nameWithoutExt = JkUtilsString.substringBeforeLast(name, ".");
        String ext = JkUtilsString.substringAfterLast(name, ".");
        String sourceName = nameWithoutExt + "-sources." + ext;
        List<File> folders = JkUtilsIterable.listOf(
                binary.getParentFile(),
                new File(binary.getParentFile().getParentFile().getParentFile(), "libs-sources"),
                new File(binary.getParentFile().getParentFile(), "libs-sources"),
                new File(binary.getParentFile(), "libs-sources"));
        List<String> names = JkUtilsIterable.listOf(sourceName, nameWithoutExt + "-sources.zip");
        return lookFileHere(folders, names);
    }

    private File lookForJavadoc(File binary) {
        String name = binary.getName();
        String nameWithoutExt = JkUtilsString.substringBeforeLast(name, ".");
        String ext = JkUtilsString.substringAfterLast(name, ".");
        String sourceName = nameWithoutExt + "-javadoc." + ext;
        List<File> folders = JkUtilsIterable.listOf(
                binary.getParentFile(),
                new File(binary.getParentFile().getParentFile().getParentFile(), "libs-javadoc"),
                new File(binary.getParentFile().getParentFile(), "libs-javadoc"),
                new File(binary.getParentFile(), "libs-javadoc"));
        List<String> names = JkUtilsIterable.listOf(sourceName, nameWithoutExt + "-javadoc.zip");
        return lookFileHere(folders, names);
    }

    private File lookFileHere(Iterable<File> folders, Iterable<String> names) {
        for (File folder : folders) {
            for (String name : names) {
                File candidate = new File(folder, name);
                if (candidate.exists()) {
                    return candidate;
                }
            }
        }
        return null;
    }

}
