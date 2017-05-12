package org.jerkar.tool.builtins.idea;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
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

    /** Dependency resolver to fetch module dependencies for build classes */
    JkDependencyResolver buildDefDependencyResolver;

    /** Can be empty but not null */
    Iterable<File> projectDependencies = JkUtilsIterable.listOf();

    private boolean forceJdkVersion;

    private final Set<String> paths = new HashSet<String>();

    private final ByteArrayOutputStream fos = new ByteArrayOutputStream();

    private final XMLStreamWriter writer = createWriter(fos);

    /**
     * Constructs a {@link ImlGenerator} from the project base directory
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
        writeContent();
        writeOrderEntrySourceFolder();
        Set<File> allPaths = new HashSet<File>();
        Set<File> allModules = new HashSet<File>();
        if (this.dependencyResolver != null) {
            writeDependencies(this.dependencyResolver, allPaths, allModules, false);
        }
        if (this.buildDefDependencyResolver != null) {
            writeDependencies(this.buildDefDependencyResolver, allPaths, allModules, true);
        }
        writeBuildProjectDependencies(allModules);
        writeJdk();
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

        // TODO should get location from #outputClassFolder and #outputTestClassFolder
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

    private void writeDependencies(JkDependencyResolver resolver, Set<File> allPaths, Set<File> allModules,  boolean forceTest) throws XMLStreamException {

        // Get dependency resolution result from both regular dependencies and build dependencies
        final JkResolveResult resolveResult = resolver.resolve();

        // Write direct dependencies  (maven module + file system lib + computed deps)
        for (final JkScopedDependency scopedDependency : resolver.dependenciesToResolve()) {
            final JkDependency dependency = scopedDependency.dependency();
            String ideScope = forceTest ? "TEST" : ideScope(scopedDependency.scopes());

                // Maven dependencies
            if (dependency instanceof JkModuleDependency) {
                final JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                final List<LibPath> paths = toLibPath(resolveResult, moduleDependency, resolver.repositories(), ideScope);
                for (LibPath libPath : paths) {
                    if (!allPaths.contains(libPath.bin)) {
                        writeOrderEntryForLib(libPath);
                        allPaths.add(libPath.bin);
                    }
                }

                // File dependencies (file system + computed)
            } else if (dependency instanceof JkDependency.JkFileDependency) {
                final JkDependency.JkFileDependency fileDependency = (JkDependency.JkFileDependency) dependency;
                if (dependency instanceof JkComputedDependency) {
                    final File projectDir = getProjectFolderOf(fileDependency.files(), this.projectDependencies);
                    if (projectDir != null && !allModules.contains(projectDir)) {
                        writeOrderEntryForModule(projectDir.getName(), ideScope);
                        allModules.add(projectDir);
                    }
                }
                writeFileEntries(writer, fileDependency.files(), paths, ideScope);

            }
        }
    }

    private void writeFileEntries(XMLStreamWriter writer, Iterable<File> files, Set<String> paths, String ideScope) throws XMLStreamException {
        for (File file : files) {
            LibPath libPath = new LibPath();
            libPath.bin = file;
            libPath.scope = ideScope;
            writeOrderEntryForLib(libPath);
        }
    }

    private List<LibPath> toLibPath(JkResolveResult resolveResult, JkModuleDependency moduleDependency, JkRepos repos,
                                    String scope) {
        final List<LibPath> result = new LinkedList<LibPath>();
        final JkModuleId moduleId = moduleDependency.moduleId();
        final JkVersion version = resolveResult.versionOf(moduleId);
        final JkVersionedModule versionedModule = JkVersionedModule.of(moduleId, version);
        final List<File> files = resolveResult.filesOf(moduleId);
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

    private List<LibPath> toLibPath(JkDependency.JkFileDependency dependency, String scope) {
        final List<LibPath> result = new LinkedList<LibPath>();
        final Iterable<File> files = dependency.files();
        for (File file : files) {
            LibPath libPath = new LibPath();
            libPath.bin = file;
            libPath.scope = scope;
            libPath.source = fileWithSuffix(file, "sources");
            libPath.javadoc = fileWithSuffix(file, "javadoc");
            result.add(libPath);
        }
        return result;
    }

    private File fileWithSuffix(File file, String suffix) {
        String fileName = file.getPath();
        String beforeExtension = JkUtilsString.substringBeforeLast(fileName, ".");
        if (beforeExtension.equals("")) {
            File newFile = new File(beforeExtension + suffix);
            return newFile.exists() ? newFile : null;
        }
        File newFile = new File(beforeExtension + suffix + "." + JkUtilsString.substringAfterLast(fileName, "."));
        return newFile.exists() ? newFile : null;
    }

    private static Set<String> toStringScopes(Set<JkScope> scopes) {
        Set<String> result = new HashSet<String>();
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

    private void writeOrderEntrySourceFolder() throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeEmptyElement("orderEntry");
        writer.writeAttribute("type", "sourceFolder");
        writer.writeAttribute("forTests", "false");
        writer.writeCharacters("\n");
    }

    private void writeModules() throws XMLStreamException {
        for (final File depProjectDir : projectDependencies) {

            writer.writeCharacters(T2);
            writer.writeEmptyElement("orderEntry");
            writer.writeAttribute("type", "module");
            writer.writeAttribute("module-name", depProjectDir.getName());
            writer.writeAttribute("exported", "");
            writer.writeAttribute("scope", "TEST");
            writer.writeCharacters("\n");
        }
    }

    private void writeLocalLibs(JkScope scope) throws XMLStreamException {
        final List<File> binFiles = new LinkedList<File>();
        binFiles.addAll(dependencyResolver.dependenciesToResolve().fileSystemDepsOnly(scope).entries());
        if (scope.equals(JkJavaBuild.TEST)) {
            binFiles.addAll(buildDefDependencyResolver.dependenciesToResolve().localFileDependencies().entries());
        }
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
            writeOrderEntryForLib(libPath);
        }
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

    private static List<LibPath> libPaths(JkDependencyResolver dependencyResolver, JkRepos repos) {
        JkDependencies depsToResolve = dependencyResolver.dependenciesToResolve();
        if (!depsToResolve.containsModules()) {
            return new LinkedList<ImlGenerator.LibPath>();
        }
        final JkResolveResult resolveResult = dependencyResolver.resolve();
        final JkResolveResult compileResolveResult = dependencyResolver.resolve(JkJavaBuild.COMPILE);
        final JkResolveResult runtimeResolveResult = dependencyResolver.resolve(JkJavaBuild.RUNTIME);
        final JkResolveResult providedResolveResult = dependencyResolver.resolve(JkJavaBuild.PROVIDED);
        final JkResolveResult testResolveResult = dependencyResolver.resolve(JkJavaBuild.TEST);

        final List<LibPath> result = new LinkedList<LibPath>();
        for (final JkVersionedModule versionedModule : resolveResult.involvedModules()) {
            final LibPath libPath = new LibPath();
            final JkModuleId moduleId = versionedModule.moduleId();
            libPath.bin = resolveResult.filesOf(moduleId).get(0);
            libPath.source = repos.get(JkModuleDependency.of(versionedModule).classifier("sources"));
            libPath.javadoc = repos.get(JkModuleDependency.of(versionedModule).classifier("javadoc"));
            libPath.scope = scope(compileResolveResult, runtimeResolveResult, providedResolveResult,
                    testResolveResult, moduleId);
            result.add(libPath);
        }
        return result;
    }

    private static String scope(JkResolveResult compile, JkResolveResult runtime, JkResolveResult provided,
                                JkResolveResult test, JkModuleId moduleId) {
        if (provided.contains(moduleId)) {
            return "PROVIDED";
        }
        if (compile.contains(moduleId)) {
            return "COMPILE";
        }
        if (runtime.contains(moduleId)) {
            return "RUNTIME";
        }
        return "TEST";
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

    private static String replacePathWithVar(String path) {
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
     * If the specified folder is the output folder of an eclipse project than it returns the root of this project,
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

}
