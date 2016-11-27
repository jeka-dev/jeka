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

import org.jerkar.api.depmanagement.JkAttachedArtifacts;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependency;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkFileSystemDependency;
import org.jerkar.api.depmanagement.JkModuleDepFile;
import org.jerkar.api.depmanagement.JkModuleDependency;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkResolveResult;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.depmanagement.JkVersionedModule;
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

/**
 * Provides method to generate and read Eclipse metadata files.
 */
final class DotClasspathGenerator {

    private static final String ENCODING = "UTF-8";

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    private final File projectDir;

    /** default to projectDir/.classpath */
    public File outputFile;

    /** optional */
    public String jreContainer;

    /** attach javadoc to the lib dependencies */
    public boolean includeJavadoc = true;

    /** Used to generate JRE container */
    public String sourceJavaVersion;

    /** Can be empty but not null */
    public JkFileTreeSet sources = JkFileTreeSet.empty();

    /** Can be empty but not null */
    public JkFileTreeSet testSources = JkFileTreeSet.empty();

    /** Directory where are compiled test classes */
    public File testClassDir;

    /** Dependency resolver to fetch module dependencies */
    public JkDependencyResolver dependencyResolver;

    /** Dependency resolver to fetch module dependencies for build classes */
    public JkDependencyResolver buildDefDependencyResolver;

    /** Can be empty but not null */
    public Iterable<File> projectDependencies = JkUtilsIterable.listOf();

    /**
     * Constructs a {@link JkDotClasspathGenerator} from the project base
     * directory
     */
    public DotClasspathGenerator(File projectDir) {
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
        // final OutputStream fos = new FileOutputStream(outputFile);
        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fos, ENCODING);
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("classpath");
        writer.writeCharacters("\n");

        final Set<String> paths = new HashSet<String>();
        generateJava(writer, paths);

        writeJre(writer);

        // Build class sources
        if (new File(projectDir, JkConstants.BUILD_DEF_DIR).exists()) {
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("path", JkConstants.BUILD_DEF_DIR);
            writer.writeAttribute("output", JkConstants.BUILD_DEF_BIN_DIR);
            writer.writeCharacters("\n");
        }

        // Write entries for dependencies located under build/libs
        final Iterable<File> files = buildDefDependencyResolver.dependenciesToResolve().localFileDependencies();

        writeFileEntries(files, writer, paths);

        // Write project dependencies
        for (final File depProjectDir : projectDependencies) {
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("exported", "true");
            writer.writeAttribute("path", "/" + depProjectDir.getName());
            writer.writeCharacters("\n");
        }

        // Write output
        writer.writeCharacters("\t");
        writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        writer.writeAttribute("kind", "output");
        writer.writeAttribute("path", "bin");
        writer.writeCharacters("\n");
        writer.writeEndDocument();
        writer.flush();
        writer.close();

        outputFile.delete();
        JkUtilsFile.writeStringAtTop(outputFile, fos.toString(ENCODING));
    }

    private static String eclipseJavaVersion(String compilerVersion) {
        if ("7".equals(compilerVersion)) {
            return "1.7";
        }
        if ("8".equals(compilerVersion)) {
            return "1.8";
        }
        return compilerVersion;
    }

    private void writeFileEntries(Iterable<File> fileDeps, XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {
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

    private void generateJava(XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {

        // Sources
        final Set<String> sourcePaths = new HashSet<String>();

        // Test Sources
        for (final JkFileTree jkFileTree : testSources.fileTrees()) {
            if (!jkFileTree.root().exists()) {
                continue;
            }
            final String path = JkUtilsFile.getRelativePath(projectDir, jkFileTree.root()).replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("output",
                    JkUtilsFile.getRelativePath(projectDir, testClassDir).replace(File.separator, "/"));
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }


        for (final JkFileTree jkFileTree : sources.fileTrees()) {
            if (!jkFileTree.root().exists()) {
                continue;
            }
            final String path = JkUtilsFile.getRelativePath(projectDir, jkFileTree.root()).replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
            writer.writeAttribute("kind", "src");
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }



        // Write entries for dependencies
        writeDependenciesEntries(writer, paths);

    }

    //    private void writeDependenciesEntriesOld(XMLStreamWriter writer) throws XMLStreamException {
    //        if (dependencyResolver.dependenciesToResolve().containsModules()) {
    //            final JkResolveResult resolveResult = dependencyResolver.resolve(JkJavaBuild.COMPILE, JkJavaBuild.RUNTIME,
    //                    JkJavaBuild.PROVIDED, JkJavaBuild.TEST);
    //            writeExternalModuleEntries(dependencyResolver, writer, resolveResult);
    //        }
    //        if (buildDefDependencyResolver.dependenciesToResolve().containsModules()) {
    //            final JkResolveResult buildresolve = buildDefDependencyResolver.resolve();
    //            writeExternalModuleEntries(buildDefDependencyResolver, writer, buildresolve);
    //        }
    //    }

    private void writeDependenciesEntries(XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {
        final JkResolveResult resolveResult = dependencyResolver.resolve(allScopes())
                .and(buildDefDependencyResolver.resolve());
        final JkDependencies allDeps = this.dependencyResolver.dependenciesToResolve()
                .and(this.buildDefDependencyResolver.dependenciesToResolve());
        final JkAttachedArtifacts attachedArtifacts;
        if (dependencyResolver.dependenciesToResolve().containsModules()) {
            attachedArtifacts = dependencyResolver.getAttachedArtifacts(
                    new HashSet<JkVersionedModule>(resolveResult.involvedModules()), JkJavaBuild.SOURCES,
                    JkJavaBuild.JAVADOC);
        } else {
            attachedArtifacts = null;
        }
        for (final JkScopedDependency scopedDependency : allDeps) {
            final JkDependency dependency = scopedDependency.dependency();
            if (dependency instanceof JkModuleDependency) {
                final JkModuleDependency moduleDependency = (JkModuleDependency) dependency;
                writeModuleEntry(moduleDependency.moduleId(), writer, resolveResult, attachedArtifacts, paths);
            } else if (dependency instanceof JkFileSystemDependency) {
                final JkFileSystemDependency fileSystemDependency = (JkFileSystemDependency) dependency;
                writeFileEntries(fileSystemDependency.files(), writer, paths);
            }
        }
        writeExternalModuleEntries(attachedArtifacts, writer, resolveResult, paths);
    }

    private void writeExternalModuleEntries(JkAttachedArtifacts attachedArtifacts, final XMLStreamWriter writer,
            JkResolveResult resolveResult, Set<String> paths) throws XMLStreamException {
        for (final JkVersionedModule versionedModule : resolveResult.involvedModules()) {
            writeModuleEntry(versionedModule.moduleId(), writer, resolveResult, attachedArtifacts, paths);
        }
    }

    private void writeModuleEntry(JkModuleId moduleId, XMLStreamWriter writer, JkResolveResult resolveResult,
            JkAttachedArtifacts jkAttachedArtifacts, Set<String> paths) throws XMLStreamException {
        File source = null;
        File javadoc = null;
        if (jkAttachedArtifacts != null) {
            final Set<JkModuleDepFile> sourcesArtifacts = jkAttachedArtifacts.getArtifacts(moduleId, JkJavaBuild.SOURCES);
            if (!sourcesArtifacts.isEmpty()) {
                source = sourcesArtifacts.iterator().next().localFile();
            }
            final Set<JkModuleDepFile> javadocArtifacts = jkAttachedArtifacts.getArtifacts(moduleId, JkJavaBuild.JAVADOC);
            if (!javadocArtifacts.isEmpty() && includeJavadoc) {
                javadoc = javadocArtifacts.iterator().next().localFile();
            }
        }
        writeClasspathEntry(writer, resolveResult.filesOf(moduleId).get(0), source, javadoc, paths);
    }

    private void writeClasspathEntry(XMLStreamWriter writer, File bin, File source, File javadoc, Set<String> paths)
            throws XMLStreamException {
        final VarReplacement binReplacement = new VarReplacement(bin);
        if (binReplacement.skiped) {
            return;
        }
        final String binPath = binReplacement.path;
        if (paths.contains(binPath)) {
            return;
        }
        paths.add(binPath);
        writer.writeCharacters("\t");
        if (javadoc == null || !javadoc.exists()) {
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        } else {
            writer.writeStartElement(DotClasspathModel.CLASSPATHENTRY);
        }

        if (binReplacement.replaced) {
            writer.writeAttribute("kind", "var");
        } else {
            writer.writeAttribute("kind", "lib");
        }

        writer.writeAttribute("path", binPath);
        if (source != null && source.exists()) {
            final VarReplacement sourceReplacement = new VarReplacement(source);
            writer.writeAttribute("sourcepath", sourceReplacement.path);
        }
        if (javadoc != null && javadoc.exists()) {
            writer.writeCharacters("\n\t\t");
            writer.writeStartElement("attributes");
            writer.writeCharacters("\n\t\t\t");
            writer.writeEmptyElement("attribute");
            writer.writeAttribute("name", "javadoc_location");
            writer.writeAttribute("value",
                    "jar:file:/" + JkUtilsFile.canonicalPath(javadoc).replace(File.separator, "/"));
            writer.writeCharacters("\n\t\t");
            writer.writeEndElement();
            writer.writeCharacters("\n\t");
            writer.writeEndElement();
        }
        writer.writeCharacters("\n");
    }

    private static JkScope[] allScopes() {
        return new JkScope[] {JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED,
                JkJavaBuild.RUNTIME, JkJavaBuild.TEST};
    }

    private class VarReplacement {

        public final boolean replaced;

        public final String path;

        public final boolean skiped;

        public VarReplacement(File file) {
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
                final String relativePath = JkUtilsFile.getRelativePath(projectFile, projectFile);
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

}
