package dev.jeka.core.api.ide.eclipse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkProjectSourceLayout;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectDefinition;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsThrowable;
import dev.jeka.core.tool.JkConstants;

/**
 * Provides method to generate Eclipse .classpath metadata files.
 */
public final class JkEclipseClasspathGenerator {

    private static final String ENCODING = "UTF-8";

    // --------------------- content --------------------------------

    private final JkProjectSourceLayout sourceLayout;

    private final JkDependencyResolver dependencyResolver;

    private final JkDependencySet dependencies;

    // content for build class only
    private JkDependencyResolver runDependencyResolver;

    private JkDependencySet runDependencies;

    // content for build class only
    private List<Path> importedProjects = new LinkedList<>();

    // --------------------- options --------------------------------

    private boolean includeJavadoc = true;

    private final JkJavaVersion sourceVersion;

    private String jreContainer;

    /**
     * Use JERKAR_REPO and JERKAR_HOME variable instead of absolute path
     */
    private boolean usePathVariables;

    /**
     * Constructs a {@link JkEclipseClasspathGenerator}.
     */
    private JkEclipseClasspathGenerator(JkProjectSourceLayout sourceLayout, JkDependencySet dependencies,
                                        JkDependencyResolver resolver, JkJavaVersion sourceVersion) {
        this.sourceLayout = sourceLayout;
        this.dependencies = dependencies;
        this.dependencyResolver = resolver;
        this.sourceVersion = sourceVersion;
    }

    /**
     * Constructs a {@link JkEclipseClasspathGenerator}.
     */
    public static JkEclipseClasspathGenerator of(JkProjectSourceLayout sourceLayout, JkDependencySet dependencies,
                                        JkDependencyResolver resolver, JkJavaVersion sourceVersion) {
        return new JkEclipseClasspathGenerator(sourceLayout, dependencies, resolver, sourceVersion);

    }

    /**
     * Constructs a {@link JkEclipseClasspathGenerator}.
     */
    public static JkEclipseClasspathGenerator of(JkJavaProjectDefinition project, JkDependencyResolver resolver) {
        return new JkEclipseClasspathGenerator(project.getSourceLayout(), project.getDependencies(), resolver, project.getSourceVersion());
    }

    /**
     * Constructs a {@link JkEclipseClasspathGenerator}.
     */
    public static JkEclipseClasspathGenerator of(JkJavaProject javaProject) {
        return of(javaProject, javaProject.getMaker().getDependencyResolver());
    }

    private boolean hasBuildDef() {
        return new File(this.sourceLayout.getBaseDir().toFile(), JkConstants.DEF_DIR).exists();
    }


    // -------------------------- setters ----------------------------


    /**
     * Set whether or not generated .classpath file should contains javadoc link for libraries.
     */
    public JkEclipseClasspathGenerator setIncludeJavadoc(boolean includeJavadoc) {
        this.includeJavadoc = includeJavadoc;
        return this;
    }

    /**
     * Specifies the exact string to use as jre container.
     */
    public JkEclipseClasspathGenerator setJreContainer(String jreContainer) {
        this.jreContainer = jreContainer;
        return this;
    }

    /**
     * If <code>true</code> dependencies path will use JERKAR_HOME and JERKAR_REPO classpath variable instead of absolute paths.
     */
    public JkEclipseClasspathGenerator setUsePathVariables(boolean usePathVariables) {
        this.usePathVariables = usePathVariables;
        return this;
    }

    /**
     * If the build script depends on build script located in another projects, you must add those projects here.
     */
    public JkEclipseClasspathGenerator setImportedProjects(List<Path> importedBuildProjects) {
        this.importedProjects = importedBuildProjects;
        return this;
    }

    /**
     * If the build script depends on external libraries, you must set the resolver of this dependencies here.
     */
    public JkEclipseClasspathGenerator setRunDependencies(JkDependencyResolver buildDependencyResolver,
                                                          JkDependencySet buildDependencies) {
        this.runDependencyResolver = buildDependencyResolver;
        this.runDependencies = buildDependencies;
        return this;
    }

    // --------------------------------------------------------------

    /**
     * Generate the .classpath file
     */
    public String generate() {
        try {
            return _generate();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private String _generate() throws IOException, XMLStreamException, FactoryConfigurationError {
        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fos, ENCODING);
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("classpath");
        writer.writeCharacters("\n");

        final Set<String> paths = new HashSet<>();

        // Write sources for build classes
        if (hasBuildDef() && new File(sourceLayout.getBaseDir().toFile(), JkConstants.DEF_DIR).exists()) {
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "kind", "src",
                    "including", "**/*",
                    "path", JkConstants.DEF_DIR);
        }
        generateSrcAndTestSrc(writer);

        // write entries for project importedRuns
        for (final Path projectFile : this.importedProjects) {
            if (paths.contains(projectFile)) {
                continue;
            }
            paths.add(projectFile.toAbsolutePath().toString());
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "combineaccessrules", "false", "kind", "src", "exported", "true",
                    "path", "/" + projectFile.getFileName().toString());
        }

        if (this.dependencyResolver != null) {
            writeDependenciesEntries(writer, this.dependencies, this.dependencyResolver, paths);
        }
        writeJre(writer);

        // add build dependencies
        if (hasBuildDef() && runDependencyResolver != null) {
            final Iterable<Path> files = runDependencyResolver.resolve(runDependencies).getFiles();
            writeFileDepsEntries(writer, files, paths);
        }

        // Write output
        writer.writeCharacters("\t");
        writeClasspathEl(writer, "kind", "output", "path", "bin");

        // Writer doc footer
        writer.writeEndDocument();
        writer.flush();
        writer.close();

        return fos.toString(ENCODING);
    }

    /** convenient method to write classpath element shorter */
    private static void writeClasspathEl(XMLStreamWriter writer, String... items) throws XMLStreamException {
        final Map<String, String> map = JkUtilsIterable.mapOfAny((Object[]) items);
        writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            writer.writeAttribute(entry.getKey(), entry.getValue());
        }
        writer.writeCharacters("\n");
    }

    private static String eclipseJavaVersion(JkJavaVersion compilerVersion) {
        if (JkJavaVersion.V6 == compilerVersion) {
            return "1.6";
        }
        if (JkJavaVersion.V7 == compilerVersion) {
            return "1.7";
        }
        if (JkJavaVersion.V8 == compilerVersion) {
            return "1.8";
        }
        return compilerVersion.get();
    }

    private void writeProjectEntryIfNeeded(Path projectDir, XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {
        if (paths.add(projectDir.toAbsolutePath().toString())) {
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "kind", "src", "exported", "true",
                    "path", "/" + projectDir.getFileName().toString());
        }
    }

    private void writeFileDepsEntries(XMLStreamWriter writer, Iterable<Path> fileDeps, Set<String> paths) throws XMLStreamException {
        for (final Path file : fileDeps) {
            writeClasspathEl(file, writer, paths);
        }
    }

    private void writeJre(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters("\t");
        writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        writer.writeAttribute("kind", "con");
        final String container;
        if (jreContainer != null) {
            container = jreContainer;
        } else {
            if (sourceVersion != null) {
                container = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-"
                        + eclipseJavaVersion(sourceVersion);
            } else {
                container = "org.eclipse.jdt.launching.JRE_CONTAINER";
            }
        }
        writer.writeAttribute("path", container);
        writer.writeCharacters("\n");
    }

    private void writeClasspathEl(Path file, XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {

        final String name = JkUtilsString.substringBeforeLast(file.getFileSystem().toString(), ".jar");
        Path source = file.resolveSibling(name + "-sources.jar");
        if (!Files.exists(source)) {
            source = file.resolveSibling("../../libs-sources/" + name + "-getSources.jar");
        }
        if (!Files.exists(source)) {
            source = file.resolveSibling("libs-sources/" + name + "-getSources.jar");
        }
        Path javadoc = file.resolveSibling(name + "-javadoc.jar");
        if (!Files.exists(javadoc)) {
            javadoc = file.resolveSibling("../../libs-javadoc/" + name + "-javadoc.jar");
        }
        if (!Files.exists(javadoc)) {
            javadoc = file.resolveSibling("libs-javadoc/" + name + "-javadoc.jar");
        }
        writeClasspathEl(writer, file, source, javadoc, paths);

    }

    private void generateSrcAndTestSrc(XMLStreamWriter writer) throws XMLStreamException {

        final Set<String> sourcePaths = new HashSet<>();

        // Test Sources
        for (final JkPathTree fileTree : sourceLayout.getTests().and(sourceLayout.getTestResources()).getPathTrees()) {
            if (!fileTree.exists()) {
                continue;
            }
            final String path = sourceLayout.getBaseDir().relativize(fileTree.getRoot()).toString().replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writeIncludingExcluding(writer, fileTree);
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }

        // Sources
        for (final JkPathTree fileTree : sourceLayout.getSources().and(sourceLayout.getResources()).getPathTrees()) {
            if (!fileTree.exists()) {
                continue;
            }
            final String path = relativePathIfPossible(sourceLayout.getBaseDir(), fileTree.getRoot());
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writeIncludingExcluding(writer, fileTree);
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }

    }

    private static String relativePathIfPossible(Path base, Path candidate) {
        if (!candidate.startsWith(base)) {
            return candidate.toAbsolutePath().normalize().toString().replace(File.separator, "/");
        }
        return base.relativize(candidate).toString().replace(File.separator, "/");
    }

    private void writeIncludingExcluding(XMLStreamWriter writer, JkPathTree fileTree) throws XMLStreamException {
        // TODO
        final String including = ""; //toPatternString(fileTree.matcher().getIncludePatterns());
        if (!JkUtilsString.isBlank(including)) {
            writer.writeAttribute("including", including);
        }
        final String excluding = ""; //toPatternString(fileTree.matcher().getExcludePatterns());
        if (!JkUtilsString.isBlank(excluding)) {
            writer.writeAttribute("excluding", excluding);
        }
    }

    private void writeDependenciesEntries(XMLStreamWriter writer, JkDependencySet dependencies, JkDependencyResolver resolver, Set<String> allPaths) throws XMLStreamException {
        final JkResolveResult resolveResult = resolver.resolve(dependencies);
        final JkRepoSet repos = resolver.getRepos();
        for (final JkDependencyNode node : resolveResult.getDependencyTree().toFlattenList()) {
            // Maven dependency
            if (node.isModuleNode()) {
                final JkDependencyNode.JkModuleNodeInfo moduleNodeInfo = node.getModuleInfo();
                writeModuleEntry(writer,
                        moduleNodeInfo.getResolvedVersionedModule(),
                        moduleNodeInfo.getFiles(), repos, allPaths);

                // File dependencies (file system + computed)
            } else {
                final JkDependencyNode.JkFileNodeInfo fileNodeInfo = (JkDependencyNode.JkFileNodeInfo) node.getNodeInfo();
                if (fileNodeInfo.isComputed()) {
                    final JkComputedDependency computedDependency = fileNodeInfo.computationOrigin();
                    final Path ideProjectBaseDir = computedDependency.getIdeProjectBaseDir();
                    if (ideProjectBaseDir != null) {
                        if (!allPaths.contains(ideProjectBaseDir.toAbsolutePath().toString())) {
                            writeProjectEntryIfNeeded(ideProjectBaseDir, writer, allPaths);
                        }
                    } else {
                        writeFileDepsEntries(writer, node.getResolvedFiles(), allPaths);
                    }
                } else {
                    writeFileDepsEntries(writer, node.getResolvedFiles(), allPaths);
                }
            }
        }
    }

    private void writeModuleEntry(XMLStreamWriter writer, JkVersionedModule versionedModule, Iterable<Path> files,
                                  JkRepoSet repos, Set<String> paths) throws XMLStreamException {
        final Path source = repos.get(JkModuleDependency.of(versionedModule).withClassifier("sources"));
        Path javadoc = null;
        if (source == null || !Files.exists(source)) {
            javadoc = repos.get(JkModuleDependency.of(versionedModule).withClassifier("javadoc"));
        }
        for (final Path file : files) {
            writeClasspathEl(writer, file, source, javadoc, paths);
        }
    }

    private void writeClasspathEl(XMLStreamWriter writer, Path bin, Path source, Path javadoc, Set<String> paths)
            throws XMLStreamException {
        String binPath = bin.toAbsolutePath().toString();
        if (!paths.add(binPath)) {
            return;
        }
        boolean useRepoVariable = usePathVariables && bin.startsWith(JkLocator.getJerkarRepositoryCache());
        boolean isVar = true;
        if (useRepoVariable) {
            binPath = DotClasspathModel.JERKAR_REPO + "/" + JkLocator.getJerkarRepositoryCache().relativize(bin).toString();
        } else if (usePathVariables && bin.startsWith(JkLocator.getJerkarHomeDir())) {
            binPath = DotClasspathModel.JERKAR_HOME + "/" + JkLocator.getJerkarHomeDir().relativize(bin).toString();
        } else {
            isVar = false;
            binPath = sourceLayout.getBaseDir().relativize(bin).toString();
        }
        binPath = binPath.replace(File.separator, "/");
        writer.writeCharacters("\t");
        final boolean mustWriteJavadoc = includeJavadoc && javadoc != null
                && Files.exists(javadoc) && (source == null || !Files.exists(source));
        if (!mustWriteJavadoc) {
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        } else {
            writer.writeStartElement(DotClasspathModel.CLASSPATHENTRY);
        }
        writer.writeAttribute("kind", isVar ? "var" : "lib");
        writer.writeAttribute("path", binPath);
        writer.writeAttribute("exported", "true");
        if (source != null && Files.exists(source)) {
            String srcPath;
            if (usePathVariables && source.startsWith(JkLocator.getJerkarRepositoryCache())) {
                srcPath = DotClasspathModel.JERKAR_REPO + "/" + JkLocator.getJerkarRepositoryCache().relativize(source).toString();
            } else if (usePathVariables && source.startsWith(JkLocator.getJerkarHomeDir())) {
                srcPath = DotClasspathModel.JERKAR_HOME + "/" + JkLocator.getJerkarHomeDir().relativize(source).toString();
            }else {
                srcPath = sourceLayout.getBaseDir().relativize(source).toString();
            }
            srcPath = srcPath.replace(File.separator, "/");
            writer.writeAttribute("sourcepath", srcPath);
        }
        if (mustWriteJavadoc) {
            writer.writeCharacters("\n\t\t");
            writer.writeStartElement("attributes");
            writer.writeCharacters("\n\t\t\t");
            writer.writeEmptyElement("attribute");
            writer.writeAttribute("name", "javadoc_location");
            writer.writeAttribute("value",   // Eclipse does not andAccept variable for javadoc path
                    "jar:file:/" + javadoc.toAbsolutePath().normalize().toString()
                            .replace(File.separator, "/") + "!/");
            writer.writeCharacters("\n\t\t");
            writer.writeEndElement();
            writer.writeCharacters("\n\t");
            writer.writeEndElement();
        }
        writer.writeCharacters("\n");
    }


    private static String toPatternString(List<String> pattern) {
        return JkUtilsString.join(pattern, "|");
    }

}
