package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProjectIde;
import dev.jeka.core.api.java.project.JkProjectSourceLayout;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsThrowable;
import dev.jeka.core.tool.JkConstants;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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

    private Map<JkDependency, Properties> attributes = new HashMap<>();

    private Map<JkDependency, Properties> accessRules = new HashMap<>();

    // --------------------- options --------------------------------

    private boolean includeJavadoc = true;

    private final JkJavaVersion sourceVersion;

    private String jreContainer;

    /**
     * Use JEKA_REPO and JEKA_HOME variable instead of absolute path
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
    public static JkEclipseClasspathGenerator of(JkJavaProjectIde projectDef) {
        return new JkEclipseClasspathGenerator(projectDef.getSourceLayout(), projectDef.getDependencies(),
                projectDef.getDependencyResolver(), projectDef.getSourceVersion());
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
     * If <code>true</code> dependencies path will use JEKA_HOME and JEKA_REPO classpath variable instead of absolute paths.
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

    /**
     * For the specified dependency, specify a child attribute tag to add to the mapping classpathentry tag.
     */
    public JkEclipseClasspathGenerator addAttribute(JkDependency dependency, String name, String value) {
        this.attributes.putIfAbsent(dependency, new Properties());
        this.attributes.get(dependency).put(name, value);
        return this;
    }

    /**
     * For the specified dependency, specify a child accessrule tag to add to the mapping classpathentry tag.
     */
    public JkEclipseClasspathGenerator addAccessRule(JkDependency dependency, String kind, String pattern) {
        this.accessRules.putIfAbsent(dependency, new Properties());
        this.accessRules.get(dependency).put(kind, pattern);
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
            if (!paths.add(projectFile.toAbsolutePath().toString())) {
                continue;
            }
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
            writeFileDepsEntries(writer, files, paths, new Properties(), new Properties());
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
        if (JkJavaVersion.V1_3.equals(compilerVersion)) {
            return "J2SE-1.3";
        }
        if (JkJavaVersion.V1_4.equals(compilerVersion)) {
            return "J2SE-1.4";
        }
        if (JkJavaVersion.V5.equals(compilerVersion)) {
            return "J2SE-1.5";
        }
        if (JkJavaVersion.V6.equals(compilerVersion)) {
            return "JavaSE-1.6";
        }
        if (JkJavaVersion.V7.equals(compilerVersion)) {
            return "JavaSE-1.7";
        }
        if (JkJavaVersion.V8.equals(compilerVersion)) {
            return "JavaSE-1.8";
        }
        return "JavaSE-" + compilerVersion.get();
    }

    private void writeProjectEntryIfNeeded(Path projectDir, XMLStreamWriter writer, Set<String> paths) throws XMLStreamException {
        if (paths.add(projectDir.toAbsolutePath().toString())) {
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "kind", "src", "exported", "true",
                    "path", "/" + projectDir.getFileName().toString());
        }
    }

    private void writeFileDepsEntries(XMLStreamWriter writer, Iterable<Path> fileDeps, Set<String> paths,
                                      Properties attributeProps, Properties accessRuleProps) throws XMLStreamException {
        for (final Path file : fileDeps) {
            writeFileEntry(file, writer, paths, attributeProps, accessRuleProps);
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
                container = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/"
                        + eclipseJavaVersion(sourceVersion);
            } else {
                container = "org.eclipse.jdt.launching.JRE_CONTAINER";
            }
        }
        writer.writeAttribute("path", container);
        writer.writeCharacters("\n");
    }

    private void writeFileEntry(Path file, XMLStreamWriter writer, Set<String> paths, Properties attributeProps,
                                Properties accessRuleProps)
            throws XMLStreamException {
        final String name = JkUtilsString.substringBeforeLast(file.getFileName().toString(), ".jar");
        Path source = file.resolveSibling(name + "-sources.jar");
        if (!Files.exists(source)) {
            source = file.resolveSibling("../../libs-sources/" + name + "-sources.jar");
        }
        if (!Files.exists(source)) {
            source = file.resolveSibling("libs-sources/" + name + "-sources.jar");
        }
        Path javadoc = file.resolveSibling(name + "-javadoc.jar");
        if (!Files.exists(javadoc)) {
            javadoc = file.resolveSibling("../../libs-javadoc/" + name + "-javadoc.jar");
        }
        if (!Files.exists(javadoc)) {
            javadoc = file.resolveSibling("libs-javadoc/" + name + "-javadoc.jar");

        }
        if (Files.exists(javadoc) && includeJavadoc) {
            attributeProps.put("javadoc_location", javadocAttributeValue(javadoc));
        }
        writeClasspathEl(writer, file, source, attributeProps, accessRuleProps, paths);
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
            final String path = relativePathIfPossible(sourceLayout.getBaseDir(), fileTree.getRoot()).toString().replace(File.separator, "/");
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

    private static Path relativePathIfPossible(Path base, Path candidate) {
        if (!candidate.startsWith(base)) {
            return candidate.toAbsolutePath().normalize();
        }
        return base.relativize(candidate);
    }

    private void writeIncludingExcluding(XMLStreamWriter writer, JkPathTree fileTree) throws XMLStreamException {
        final String including = "";
        if (!JkUtilsString.isBlank(including)) {
            writer.writeAttribute("including", including);
        }
        final String excluding = "";
        if (!JkUtilsString.isBlank(excluding)) {
            writer.writeAttribute("excluding", excluding);
        }
    }

    private void writeDependenciesEntries(XMLStreamWriter writer, JkDependencySet dependencies,
                                          JkDependencyResolver resolver, Set<String> allPaths) throws XMLStreamException {
        final JkResolveResult resolveResult = resolver.resolve(dependencies);
        final JkRepoSet repos = resolver.getRepos();
        for (final JkDependencyNode node : resolveResult.getDependencyTree().toFlattenList()) {
            // Maven dependency
            if (node.isModuleNode()) {
                final JkDependencyNode.JkModuleNodeInfo moduleNodeInfo = node.getModuleInfo();
                JkDependency dependency = JkModuleDependency.of(moduleNodeInfo.getModuleId().getGroupAndName());
                Properties attributeProps = copyOfPropsOf(dependency, this.attributes);
                Properties accessruleProps = copyOfPropsOf(dependency, this.accessRules);
                writeModuleEntry(writer,
                        moduleNodeInfo.getResolvedVersionedModule(),
                        moduleNodeInfo.getFiles(), repos, allPaths, attributeProps, accessruleProps);

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
                        writeFileDepsEntries(writer, node.getResolvedFiles(), allPaths, new Properties(), new Properties());
                    }
                } else {
                    JkDependency fileDep = JkFileSystemDependency.of(fileNodeInfo.getFiles());
                    Properties attributeProps = copyOfPropsOf(fileDep, this.attributes);
                    Properties accessRuleProps = copyOfPropsOf(fileDep, this.accessRules);
                    writeFileDepsEntries(writer, node.getResolvedFiles(), allPaths, attributeProps, accessRuleProps);
                }
            }
        }
    }

    private void writeModuleEntry(XMLStreamWriter writer, JkVersionedModule versionedModule, Iterable<Path> files,
                                  JkRepoSet repos, Set<String> paths, Properties attributeProps,
                                  Properties accessRuleProps) throws XMLStreamException {
        final Path source = repos.get(JkModuleDependency.of(versionedModule).withClassifier("sources"));
        Path javadoc = null;
        if (source == null || !Files.exists(source) || this.includeJavadoc) {
            javadoc = repos.get(JkModuleDependency.of(versionedModule).withClassifier("javadoc"));
        }
        if (javadoc != null) {
            attributeProps.put("javadoc_location", javadocAttributeValue(javadoc));
        }
        for (final Path file : files) {
            writeClasspathEl(writer, file, source, attributeProps, accessRuleProps, paths);
        }
    }

    private void writeClasspathEl(XMLStreamWriter writer, Path bin, Path source, Properties attributeProps,
                                  Properties accesRuleProps, Set<String> paths)
            throws XMLStreamException {
        String binPath = bin.toAbsolutePath().toString();
        if (!paths.add(binPath)) {
            return;
        }
        boolean usePathVariable = usePathVariables && bin.startsWith(JkLocator.getJekaUserHomeDir());
        boolean isVar = true;
        if (usePathVariable) {
            binPath = DotClasspathModel.JEKA_USER_HOME + "/" + JkLocator.getJekaUserHomeDir().relativize(bin).toString();
        } else if (usePathVariables && bin.startsWith(JkLocator.getJekaHomeDir())) {
            binPath = DotClasspathModel.JEKA_HOME + "/" + JkLocator.getJekaHomeDir().relativize(bin).toString();
        } else {
            isVar = false;
            binPath = relativePathIfPossible(sourceLayout.getBaseDir(), bin).toString();
        }
        binPath = binPath.replace(File.separator, "/");
        writer.writeCharacters("\t");
        boolean emptyTag = attributeProps.isEmpty() && accesRuleProps.isEmpty();
        if (emptyTag) {
            writer.writeEmptyElement(DotClasspathModel.CLASSPATHENTRY);
        } else {
            writer.writeStartElement(DotClasspathModel.CLASSPATHENTRY);
        }
        writer.writeAttribute("kind", isVar ? "var" : "lib");
        writer.writeAttribute("path", binPath);
        writer.writeAttribute("exported", "true");
        if (source != null && Files.exists(source)) {
            String srcPath;
            if (usePathVariables && source.startsWith(JkLocator.getJekaUserHomeDir())) {
                srcPath = DotClasspathModel.JEKA_USER_HOME + "/" + JkLocator.getJekaUserHomeDir().relativize(source).toString();
            } else if (usePathVariables && source.startsWith(JkLocator.getJekaHomeDir())) {
                srcPath = DotClasspathModel.JEKA_HOME + "/" + JkLocator.getJekaHomeDir().relativize(source).toString();
            } else {
                srcPath = relativePathIfPossible(sourceLayout.getBaseDir(), source).toString();
            }
            srcPath = srcPath.replace(File.separator, "/");
            writer.writeAttribute("sourcepath", srcPath);
        }
        writeClasspathentryChildAttributes(writer, attributeProps);
        writeClasspathentryChildAccessRules(writer, accesRuleProps);
        if (!emptyTag) {
            writer.writeCharacters("\n\t");
            writer.writeEndElement();
        }
        writer.writeCharacters("\n");
    }

    private static String javadocAttributeValue(Path javadocPath) {
        return "jar:file:/" + javadocPath.toAbsolutePath().normalize().toString()
                .replace(File.separator, "/") + "!/";
    }

    private void writeClasspathentryChildAttributes(XMLStreamWriter writer, Properties props) throws XMLStreamException {
        if (props == null || props.isEmpty()) {
            return;
        }
        writer.writeCharacters("\n\t\t");
        writer.writeStartElement("attributes");
        for (String key : props.stringPropertyNames()) {
            writer.writeCharacters("\n\t\t\t");
            String value = props.getProperty(key);
            writer.writeEmptyElement("attribute");
            writer.writeAttribute("name", key);
            writer.writeAttribute("value", value);
        }
        writer.writeCharacters("\n\t\t");
        writer.writeEndElement();
    }

    private void writeClasspathentryChildAccessRules(XMLStreamWriter writer, Properties props) throws XMLStreamException {
        if (props == null || props.isEmpty()) {
            return;
        }
        writer.writeCharacters("\n\t\t");
        writer.writeStartElement("accessrules");
        for (String key : props.stringPropertyNames()) {
            writer.writeCharacters("\n\t\t\t");
            String value = props.getProperty(key);
            writer.writeEmptyElement("accessrule");
            writer.writeAttribute("kind", key);
            writer.writeAttribute("pattern", value);
        }
        writer.writeCharacters("\n\t\t");
        writer.writeEndElement();
    }

    private static Properties copyOfPropsOf(JkDependency dependency, Map<JkDependency, Properties> propMap) {
        JkDependency key = findMatchingKey(dependency, propMap.keySet());
        if (key == null) {
            return new Properties();
        }
        Properties props = propMap.get(key);
        Properties result = new Properties();
        result.putAll(props);
        return result;
    }

    private static JkDependency findMatchingKey(JkDependency dep1, Collection<JkDependency> deps) {
        return deps.stream().filter(dep -> depsMatchForExtraAttributes(dep1, dep)).findFirst().orElse(null);
    }

    private static boolean depsMatchForExtraAttributes(JkDependency dep1, JkDependency dep2) {
        if (dep1 instanceof JkModuleDependency) {
            if (dep2 instanceof JkModuleDependency) {
                JkModuleDependency modDep1 = (JkModuleDependency) dep1;
                JkModuleDependency modDep2 = (JkModuleDependency) dep2;
                return modDep1.getModuleId().equals(modDep2.getModuleId());
            }
            return false;
        }
        if (dep1 instanceof JkFileDependency) {
            if (dep2 instanceof JkFileSystemDependency) {
                return dep1.equals(dep2);
            }
        }
        return false;
    }

}
