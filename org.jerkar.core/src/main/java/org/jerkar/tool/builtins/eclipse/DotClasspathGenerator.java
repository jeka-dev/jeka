package org.jerkar.tool.builtins.eclipse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.jerkar.api.utils.*;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildDependency;
import org.jerkar.tool.JkConstants;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
final class DotClasspathGenerator {

    private static final String ENCODING = "UTF-8";

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    private final File projectDir;

    /**
     * default to projectDir/.classpath
     */
    File outputFile;

    /**
     * optional
     */
    String jreContainer;

    /**
     * attach javadoc to the lib dependencies
     */
    boolean includeJavadoc = true;

    /**
     * Used to generate JRE container
     */
    String sourceJavaVersion;

    /**
     * Can be empty but not null
     */
    JkFileTreeSet sources = JkFileTreeSet.empty();

    /**
     * Can be empty but not null
     */
    JkFileTreeSet testSources = JkFileTreeSet.empty();

    /**
     * Directory where are compiled test classes
     */
    File testClassDir;

    /**
     * Dependency resolver to fetch module dependencies
     */
    JkDependencyResolver dependencyResolver;

    /**
     * Dependency resolver to fetch module dependencies for build classes
     */
    JkDependencyResolver buildDefDependencyResolver;

    /**
     * Can be empty but not null
     */
    Iterable<File> projectDependencies = JkUtilsIterable.listOf();

    /**
     * Map jump file to project dir make .classpath depends on a project instead of a file
     */
    Map<File, File> fileDependencyToProjectSubstitution;

    /**
     * projects for which we don't want to use project dependencies
     **/
    Set<File> projectDependencyToFileSubstitutions;

    /**
     * Use absolute paths instead of classpath variables
     */
    public boolean useAbsolutePaths;

    /**
     * Constructs a {@link DotClasspathGenerator} jump the project base
     * directory
     */
    DotClasspathGenerator(File projectDir) {
        super();
        this.projectDir = projectDir;
        this.outputFile = new File(projectDir, ".classpath");
    }

    /**
     * Generate the .classpath file
     */
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
        writer.writeStartElement("classpath");
        writer.writeCharacters("\n");

        final Set<String> paths = new HashSet<String>();

        // Write sources for build classes
        if (new File(projectDir, JkConstants.BUILD_DEF_DIR).exists()) {
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("path", JkConstants.BUILD_DEF_DIR);
            writer.writeAttribute("output", JkConstants.BUILD_DEF_BIN_DIR);
            writer.writeCharacters("\n");
        }

        generateSrcAndTestSrc(writer);
        writeDependenciesEntries(writer, paths);
        writeJre(writer);

        // Write entries for dependencies located under build/libs
        final Iterable<File> files = buildDefDependencyResolver.dependenciesToResolve().localFileDependencies();
        writeFileEntries(writer, files, paths);

        // write entries for project build dependencies
        for (File projectFile : this.projectDependencies) {
            if (paths.contains(projectFile.getPath())) {
                continue;
            }
            paths.add(projectFile.getAbsolutePath());
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("combineaccessrules", "false");
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("exported", "true");
            writer.writeAttribute("path", "/" + projectFile.getName());
            writer.writeCharacters("\n");
        }


        // Write output
        writer.writeCharacters("\t");
        writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        writer.writeAttribute("kind", "output");
        writer.writeAttribute("path", "bin");
        writer.writeCharacters("\n");

        // Writer doc footer
        writer.writeEndDocument();
        writer.flush();
        writer.close();

        // Store generated file
        outputFile.delete();
        JkUtilsFile.writeStringAtTop(outputFile, fos.toString(ENCODING));
    }

    private static String eclipseJavaVersion(String compilerVersion) {
        if ("6".equals(compilerVersion)) {
            return "1.6";
        }
        if ("7".equals(compilerVersion)) {
            return "1.7";
        }
        if ("8".equals(compilerVersion)) {
            return "1.8";
        }
        return compilerVersion;
    }

    private void writeProjectEntry(File projectDir, XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {
        if (paths.add(projectDir.getAbsolutePath())) {
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("exported", "true");
            writer.writeAttribute("path", "/" + projectDir.getName());
            writer.writeCharacters("\n");
        }
    }

    private void writeFileEntries(XMLStreamWriter writer, Iterable<File> fileDeps, Set<String> paths) throws XMLStreamException {
        for (final File file : fileDeps) {
            writeFileEntry(file, writer, paths);
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
            container = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-"
                    + eclipseJavaVersion(sourceJavaVersion);
        }
        writer.writeAttribute("path", container);
        writer.writeCharacters("\n");
    }

    private void writeFileEntry(File file, XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {

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
        writeClasspathEntry(writer, file, source, javadoc, paths);

    }

    private void generateSrcAndTestSrc(XMLStreamWriter writer) throws XMLStreamException {

        final Set<String> sourcePaths = new HashSet<String>();

        // Test Sources
        for (final JkFileTree fileTree : testSources.fileTrees()) {
            if (!fileTree.root().exists()) {
                continue;
            }
            final String path = JkUtilsFile.getRelativePath(projectDir, fileTree.root()).replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writeIncludingExcluding(writer, fileTree);
            writer.writeAttribute("output",
                    JkUtilsFile.getRelativePath(projectDir, testClassDir).replace(File.separator, "/"));
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }

        // Sources
        for (final JkFileTree fileTree : sources.fileTrees()) {
            if (!fileTree.root().exists()) {
                continue;
            }
            final String path = JkUtilsFile.getRelativePath(projectDir, fileTree.root()).replace(File.separator, "/");
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

    private void writeDependenciesEntries(XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {

        // Get dependency resolution result jump both regular dependencies and build dependencies
        final JkResolveResult resolveResult = dependencyResolver.resolve(allScopes())
                .and(buildDefDependencyResolver.resolve());
        final JkDependencies allDeps = this.dependencyResolver.dependenciesToResolve()
                .and(this.buildDefDependencyResolver.dependenciesToResolve());

        JkRepos repos = dependencyResolver.repositories().and(buildDefDependencyResolver.repositories());

        // Write direct dependencies (maven module + file system lib + computed deps)
        for (final JkScopedDependency scopedDependency : allDeps) {
            final JkDependency dependency = scopedDependency.dependency();

            // Maven dependencies
            if (dependency instanceof JkModuleDependency) {
                final JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                JkModuleId moduleId = moduleDependency.moduleId();
                JkVersion version = resolveResult.versionOf(moduleId);
                JkVersionedModule versionedModule = JkVersionedModule.of(moduleId, version);
                writeModuleEntry(writer, versionedModule, resolveResult.filesOf(moduleId), repos, paths);
            }

            // Computed dependencies
            else if (dependency instanceof JkComputedDependency) {
                final JkComputedDependency computedDependency = (JkComputedDependency) dependency;
                if (computedDependency instanceof JkBuildDependency) {
                    final JkBuildDependency buildDependency = (JkBuildDependency) dependency;
                    final JkBuild build = buildDependency.projectBuild();
                    if (build instanceof JkJavaBuild) {
                        if (!projectDependencyToFileSubstitutions.contains(build.baseDir().root())) {
                            writeProjectEntry(build.baseDir().root(), writer, paths);
                        } else {
                            writeFileEntries(writer, buildDependency.files(), paths);
                        }
                    }
                } else {
                    writeFileEntries(writer, computedDependency.files(), paths);
                }

                // Other file dependencies
            } else if (dependency instanceof JkDependency.JkFileDependency) {
                final JkDependency.JkFileDependency fileDependency = (JkDependency.JkFileDependency) dependency;
                final File projectDir = getProjectDir(fileDependency.files());
                if (projectDir != null) {
                    writeProjectEntry(projectDir, writer, paths);
                } else {
                    writeFileEntries(writer, fileDependency.files(), paths);
                }
            }
        }

        // Write transitive maven dependencies
        writeExternalModuleEntries(writer, resolveResult, paths, repos);
    }

    private File getProjectDir(Iterable<File> files) {

        // check the files for explicit project settings
        for (final File file : files) {
            if (fileDependencyToProjectSubstitution.containsKey(file)) {

                // project dir explicitly set, always use that, even if null
                return fileDependencyToProjectSubstitution.get(file);
            }
        }

        // project dir not specified, try to guess
        for (final File file : files) {
            final File projectDir = getProjectFolderOf(file);
            if (projectDir != null) {
                return projectDir;
            }
        }

        return null;
    }

    private void writeExternalModuleEntries(final XMLStreamWriter writer,
                                            JkResolveResult resolveResult, Set<String> paths, JkRepos repos) throws XMLStreamException {
        for (final JkVersionedModule versionedModule : resolveResult.involvedModules()) {
            JkModuleId moduleId = versionedModule.moduleId();
            JkVersion version = resolveResult.versionOf(moduleId);
            if (version != null) {
                JkVersionedModule resolvedModule = JkVersionedModule.of(moduleId, version);
                writeModuleEntry(writer, resolvedModule, resolveResult.filesOf(moduleId), repos, paths);
            }
        }
    }

    private void writeModuleEntry(XMLStreamWriter writer, JkVersionedModule versionedModule, Iterable<File> files,
                                  JkRepos repos, Set<String> paths) throws XMLStreamException {
        File source = repos.get(JkModuleDependency.of(versionedModule).classifier("sources"));
        File javadoc = null;
        if (source == null || !source.exists()) {
            javadoc = repos.get(JkModuleDependency.of(versionedModule).classifier("javadoc"));
        }
        for (final File file : files) {
            writeClasspathEntry(writer, file, source, javadoc, paths);
        }
    }

    private void writeClasspathEntry(XMLStreamWriter writer, File bin, File source, File javadoc, Set<String> paths)
            throws XMLStreamException {
        final VarReplacement binReplacement = new VarReplacement(bin);
        if (binReplacement.skiped) {
            return;
        }
        String binPath = bin.getAbsolutePath();
        if (!useAbsolutePaths) {
            binPath = binReplacement.path;
        }
        if (paths.contains(binPath)) {
            return;
        }
        paths.add(binPath);
        writer.writeCharacters("\t");
        boolean mustWriteJavadoc = javadoc != null && javadoc.exists() && (source == null || !source.exists());
        if (!mustWriteJavadoc) {
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        } else {
            writer.writeStartElement(DotClasspathModel.CLASSPATHENTRY);
        }

        if (!useAbsolutePaths && binReplacement.replaced) {
            writer.writeAttribute("kind", "var");
        } else {
            writer.writeAttribute("kind", "lib");
        }

        writer.writeAttribute("path", binPath);
        writer.writeAttribute("exported", "true");
        if (source != null && source.exists()) {
            String srcPath = source.getAbsolutePath();
            if (!useAbsolutePaths) {
                srcPath = new VarReplacement(source).path;
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

    private static JkScope[] allScopes() {
        return new JkScope[]{JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED,
                JkJavaBuild.RUNTIME, JkJavaBuild.TEST};
    }

    private class VarReplacement {

        final boolean replaced;

        final String path;

        final boolean skiped;

        VarReplacement(File file) {
            final Map<String, String> map = JkOptions.getAllStartingWith(DotClasspathGenerator.OPTION_VAR_PREFIX);
            map.put(DotClasspathGenerator.OPTION_VAR_PREFIX + DotClasspathModel.JERKAR_REPO,
                    JkLocator.jerkarRepositoryCache().getAbsolutePath());
            if (!JkLocator.jerkarJarFile().isDirectory()) {
                map.put(DotClasspathGenerator.OPTION_VAR_PREFIX + DotClasspathModel.JERKAR_HOME,
                        JkLocator.jerkarHome().getAbsolutePath());
            }
            boolean replaced = false;
            String path = JkUtilsFile.canonicalPath(file).replace(File.separator, "/");

            // replace with var
            for (final Map.Entry<String, String> entry : map.entrySet()) {
                final File varDir = new File(entry.getValue());
                if (JkUtilsFile.isAncestor(varDir, file)) {
                    final String relativePath = JkUtilsFile.getRelativePath(varDir, file);
                    replaced = true;
                    path = entry.getKey().substring(DotClasspathGenerator.OPTION_VAR_PREFIX.length()) + "/"
                            + relativePath;
                    path = path.replace(File.separator, "/");
                    break;
                }
            }

            // Replace with relative path
            if (!replaced) {
                final String relpPath = toDependendeeProjectRelativePath(file);
                if (relpPath != null) {
                    if (file.getName().toLowerCase().endsWith(".jar")) {
                        skiped = true;
                    } else {
                        skiped = false;
                        path = relpPath;
                    }
                } else {
                    path = toRelativePath(file);
                    skiped = false;
                }
            } else {
                skiped = false;
            }

            this.path = path;
            this.replaced = replaced;
        }

    }

    private String toDependendeeProjectRelativePath(File file) {
        for (final File projectFile : this.projectDependencies) {
            if (JkUtilsFile.isAncestor(projectFile, file)) {
                final String relativePath = JkUtilsFile.getRelativePath(projectFile, file);
                final Project project = Project.of(new File(projectFile, ".project"));
                return "/" + project.name + "/" + relativePath;
            }
        }
        return null;
    }

    private String toRelativePath(File file) {
        if (JkUtilsFile.isAncestor(this.projectDir, file)) {
            return JkUtilsFile.getRelativePath(projectDir, file).replace(File.separatorChar, '/');
        }
        return JkUtilsFile.canonicalPath(file);
    }

    private static String toPatternString(List<String> pattern) {
        return JkUtilsString.join(pattern, "|");
    }

    /*
     * If the specified folder is the output folder of an eclipse project than it returns the asScopedDependency of this project,
     * else otherwise.
     */
    private static File getProjectFolderOf(File binaryFolder) {
        if (!binaryFolder.isDirectory()) {
            return null;
        }
        File folder = binaryFolder.getParentFile();
        while (folder != null) {
            File dotClasspath = new File(folder, ".classpath");
            if (!dotClasspath.exists()) {
                folder = folder.getParentFile();
                continue;
            }
            final DotClasspathModel model;
            try {
                model = DotClasspathModel.from(dotClasspath);
            } catch (RuntimeException e) {
                JkLog.warn("File " + dotClasspath + " can't be parsed. " + binaryFolder
                        + " dependency won't be considered as the output of an Eclipse project.");
                JkLog.warn("Cause exception is " + e.getMessage());
                return null;
            }
            File outputFile = new File(model.outputPath());
            if (!outputFile.isAbsolute()) {
                outputFile = new File(folder, model.outputPath());
            }
            if (JkUtilsFile.isSame(binaryFolder, outputFile)) {
                return folder;
            }
            folder = folder.getParentFile();
        }
        return null;
    }

}
