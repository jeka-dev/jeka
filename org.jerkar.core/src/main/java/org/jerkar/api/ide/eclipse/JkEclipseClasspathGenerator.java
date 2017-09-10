package org.jerkar.api.ide.eclipse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyNode;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolveResult;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProjectDefinition;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.tool.JkConstants;

/**
 * Provides method to generate Eclipse .classpath metadata files.
 */
@Deprecated // Experimental !!!!
public final class JkEclipseClasspathGenerator {

    private static final String ENCODING = "UTF-8";

    // --------------------- content --------------------------------

    private JkProjectSourceLayout sourceLayout;

    private JkDependencyResolver dependencyResolver;

    private JkDependencies dependencies;

    // content for build class only
    private JkDependencyResolver buildDefDependencyResolver;

    private JkDependencies buildDependencies;

    // content for build class only
    private List<File> slaveProjects = new LinkedList<File>();

    // --------------------- options --------------------------------

    private boolean includeJavadoc = true;

    private JkJavaVersion sourceVersion;

    private String jreContainer;

    /**
     * Use JERKAR_REPO and JERKAR_HOME variable instead of absolute path
     */
    private boolean usePathVariables;

    private boolean hasBuildScript;

    public JkEclipseClasspathGenerator(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
    }

    public JkEclipseClasspathGenerator(JkJavaProjectDefinition project, JkDependencyResolver resolver) {
        this.sourceLayout = project.getSourceLayout();
        this.dependencies = project.getDependencies();
        this.dependencyResolver = resolver;
        this.sourceVersion = project.getCompileVersion().source();
    }

    // -------------------------- setters ----------------------------


    public JkEclipseClasspathGenerator setStructure(JkProjectSourceLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        return this;
    }

    public JkEclipseClasspathGenerator setDependencyResolver(JkDependencies dependencies, JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        this.dependencies = dependencies;
        return this;
    }

    public JkEclipseClasspathGenerator setIncludeJavadoc(boolean includeJavadoc) {
        this.includeJavadoc = includeJavadoc;
        return this;
    }

    public JkEclipseClasspathGenerator setSourceJavaVersion(JkJavaVersion sourceVersion) {
        this.sourceVersion = sourceVersion;
        return this;
    }

    public JkEclipseClasspathGenerator setJreContainer(String jreContainer) {
        this.jreContainer = jreContainer;
        return this;
    }

    public JkEclipseClasspathGenerator setUsePathVariables(boolean usePathVariables) {
        this.usePathVariables = usePathVariables;
        return this;
    }

    public JkEclipseClasspathGenerator setHasBuildScript(boolean hasBuildScript) {
        this.hasBuildScript = hasBuildScript;
        return this;
    }

    /**
     * If the build script depends on build script located in another projects, you must add those projects here.
     */
    public JkEclipseClasspathGenerator setSlaveProjects(List<File> slaveProjects) {
        this.slaveProjects = slaveProjects;
        return this;
    }

    /**
     * If the build script depends on external libraries, you must set the resolver of this dependencies here.
     */
    public JkEclipseClasspathGenerator setBuildDefDependencyResolver(JkDependencyResolver buildDefDependencyResolver) {
        this.buildDefDependencyResolver = buildDefDependencyResolver;
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

        final Set<String> paths = new HashSet<String>();

        // Write sources for build classes
        if (hasBuildScript && new File(sourceLayout.baseDir(), JkConstants.BUILD_DEF_DIR).exists()) {
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "kind", "src",
                    "path", JkConstants.BUILD_DEF_DIR,
                    "output", JkConstants.BUILD_DEF_BIN_DIR);
        }

        generateSrcAndTestSrc(writer);
        if (this.dependencyResolver != null) {
            writeDependenciesEntries(writer, this.dependencies, this.dependencyResolver, paths);
        }
        writeJre(writer);

        // add build dependencies
        if (hasBuildScript && buildDefDependencyResolver != null) {
            final Iterable<File> files = buildDependencies.localFileDependencies();
            writeFileDepsEntries(writer, files, paths);
        }

        // write entries for project slaves
        for (final File projectFile : this.slaveProjects) {
            if (paths.contains(projectFile.getPath())) {
                continue;
            }
            paths.add(projectFile.getAbsolutePath());
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "combineaccessrules", "false", "kind", "src", "exported", "true",
                    "path", "/" + projectFile.getName());
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
        return compilerVersion.name();
    }

    private void writeProjectEntryIfNeeded(File projectDir, XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {
        if (paths.add(projectDir.getAbsolutePath())) {
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "kind", "src", "exported", "true",
                    "path", "/" + projectDir.getName());
        }
    }

    private void writeFileDepsEntries(XMLStreamWriter writer, Iterable<File> fileDeps, Set<String> paths) throws XMLStreamException {
        for (final File file : fileDeps) {
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

    private void writeClasspathEl(File file, XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {

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
        writeClasspathEl(writer, file, source, javadoc, paths);

    }

    private void generateSrcAndTestSrc(XMLStreamWriter writer) throws XMLStreamException {

        final Set<String> sourcePaths = new HashSet<String>();

        // Test Sources
        for (final JkFileTree fileTree : sourceLayout.tests().and(sourceLayout.testResources()).fileTrees()) {
            if (!fileTree.root().exists()) {
                continue;
            }
            final String path = JkUtilsFile.getRelativePath(sourceLayout.baseDir(), fileTree.root()).replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writeIncludingExcluding(writer, fileTree);
            //writer.writeAttribute("output",
            //        relativePathIfPossible(structure.baseDir(), structure.testClassDir()));
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }

        // Sources
        for (final JkFileTree fileTree : sourceLayout.sources().and(sourceLayout.resources()).fileTrees()) {
            if (!fileTree.root().exists()) {
                continue;
            }
            final String path = relativePathIfPossible(sourceLayout.baseDir(), fileTree.root());
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

    private static String relativePathIfPossible(File base, File candidate) {
        if (!JkUtilsFile.isAncestor(base, candidate)) {
            return JkUtilsFile.canonicalPath(candidate).replace(File.separator, "/");
        }
        return JkUtilsFile.getRelativePath(base, candidate).replace(File.separator, "/");
    }

    private void writeIncludingExcluding(XMLStreamWriter writer, JkFileTree fileTree) throws XMLStreamException {
        final String including = toPatternString(fileTree.filter().getIncludePatterns());
        if (!JkUtilsString.isBlank(including)) {
            writer.writeAttribute("including", including);
        }
        final String excluding = toPatternString(fileTree.filter().getExcludePatterns());
        if (!JkUtilsString.isBlank(excluding)) {
            writer.writeAttribute("excluding", excluding);
        }
    }

    private void writeDependenciesEntries(XMLStreamWriter writer, JkDependencies dependencies, JkDependencyResolver resolver, Set<String> allPaths) throws XMLStreamException {
        final JkResolveResult resolveResult = resolver.resolve(dependencies);
        final JkRepos repos = resolver.repositories();
        for (final JkDependencyNode node : resolveResult.dependencyTree().flatten()) {
            // Maven dependency
            if (node.isModuleNode()) {
                final JkDependencyNode.ModuleNodeInfo moduleNodeInfo = node.moduleInfo();
                writeModuleEntry(writer,
                        moduleNodeInfo.resolvedVersionedModule(),
                        moduleNodeInfo.files(), repos, allPaths);

                // File dependencies (file system + computed)
            } else {
                final JkDependencyNode.FileNodeInfo fileNodeInfo = (JkDependencyNode.FileNodeInfo) node.nodeInfo();
                if (fileNodeInfo.isComputed()) {
                    final JkComputedDependency computedDependency = fileNodeInfo.computationOrigin();
                    final File ideprojectBaseDir = computedDependency.ideProjectBaseDir();
                    if (ideprojectBaseDir != null) {
                        if (!allPaths.contains(ideprojectBaseDir.getAbsolutePath())) {
                            writeProjectEntryIfNeeded(ideprojectBaseDir, writer, allPaths);
                        }
                    } else {
                        writeFileDepsEntries(writer, node.allFiles(), allPaths);
                    }
                } else {
                    writeFileDepsEntries(writer, node.allFiles(), allPaths);
                }
            }
        }
    }

    private void writeModuleEntry(XMLStreamWriter writer, JkVersionedModule versionedModule, Iterable<File> files,
            JkRepos repos, Set<String> paths) throws XMLStreamException {
        final File source = repos.get(JkModuleDependency.of(versionedModule).classifier("sources"));
        File javadoc = null;
        if (source == null || !source.exists()) {
            javadoc = repos.get(JkModuleDependency.of(versionedModule).classifier("javadoc"));
        }
        for (final File file : files) {
            writeClasspathEl(writer, file, source, javadoc, paths);
        }
    }

    private void writeClasspathEl(XMLStreamWriter writer, File bin, File source, File javadoc, Set<String> paths)
            throws XMLStreamException {
        String binPath = bin.getAbsolutePath();
        if (!paths.add(binPath)) {
            return;
        }
        if (usePathVariables) {
            binPath = DotClasspathModel.JERKAR_REPO + "/" + toRelativePath(JkLocator.jerkarRepositoryCache(), bin);
        } else {
            binPath = binPath.replace(File.separator, "/");
        }
        writer.writeCharacters("\t");
        final boolean mustWriteJavadoc = includeJavadoc && javadoc != null
                && javadoc.exists() && (source == null || !source.exists());
        if (!mustWriteJavadoc) {
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        } else {
            writer.writeStartElement(DotClasspathModel.CLASSPATHENTRY);
        }
        writer.writeAttribute("kind", usePathVariables ? "var" : "lib");
        writer.writeAttribute("path", binPath);
        writer.writeAttribute("exported", "true");
        if (source != null && source.exists()) {
            String srcPath = source.getAbsolutePath();
            if (usePathVariables) {
                srcPath = DotClasspathModel.JERKAR_REPO + "/" + toRelativePath(JkLocator.jerkarRepositoryCache(), source);
            } else {
                srcPath = srcPath.replace(File.separator, "/");
            }
            writer.writeAttribute("sourcepath", srcPath);
        }
        if (mustWriteJavadoc) {
            writer.writeCharacters("\n\t\t");
            writer.writeStartElement("attributes");
            writer.writeCharacters("\n\t\t\t");
            writer.writeEmptyElement("attribute");
            writer.writeAttribute("name", "javadoc_location");
            writer.writeAttribute("value",   // Eclipse does not accept variable for javadoc path
                    "jar:file:/" + JkUtilsFile.canonicalPath(javadoc).replace(File.separator, "/") + "!/");
            writer.writeCharacters("\n\t\t");
            writer.writeEndElement();
            writer.writeCharacters("\n\t");
            writer.writeEndElement();
        }
        writer.writeCharacters("\n");
    }


    private static String toRelativePath(File base, File file) {
        if (JkUtilsFile.isAncestor(base, file)) {
            return JkUtilsFile.getRelativePath(base, file).replace(File.separatorChar, '/');
        }
        return JkUtilsFile.canonicalPath(file);
    }

    private static String toPatternString(List<String> pattern) {
        return JkUtilsString.join(pattern, "|");
    }


}
