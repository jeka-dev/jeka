/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkIdeSupport;
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
import java.util.stream.Collectors;

/**
 * Provides method to generate Eclipse .classpath metadata files.
 */
public final class JkEclipseClasspathGenerator {

    private static final String ENCODING = "UTF-8";

    private static final String CLASSPATH_ENTRY = "classpathentry";

    private static final String JEKA_HOME = "JEKA_HOME";

    private static final String JEKA_USER_HOME= "JEKA_USER_HOME";

    // --------------------- content --------------------------------

    private final JkIdeSupport ideSupport;

    // content for build class only
    private JkDependencyResolver defDependencyResolver;

    private JkDependencySet defDependencies;

    // content for build class only
    private List<Path> importedProjects = new LinkedList<>();

    private Map<JkDependency, Properties> attributes = new HashMap<>();

    private Map<JkDependency, Properties> accessRules = new HashMap<>();

    // --------------------- options --------------------------------

    private boolean includeJavadoc = true;

    private String jreContainer;

    /**
     * Use JEKA_REPO and JEKA_HOME variable instead of absolute path
     */
    private boolean usePathVariables;

    /**
     * Constructs a {@link JkEclipseClasspathGenerator}.
     */
    private JkEclipseClasspathGenerator(JkIdeSupport ideSupport) {
        this.ideSupport = ideSupport;
    }

    /**
     * Constructs a {@link JkEclipseClasspathGenerator}.
     */
    public static JkEclipseClasspathGenerator of(JkIdeSupport ideSupport) {
        return new JkEclipseClasspathGenerator(ideSupport);

    }

    private boolean hasJekaSrcDir() {
        return Files.exists(ideSupport.getProdLayout().getBaseDir().resolve(JkConstants.JEKA_SRC_DIR));
    }

    // -------------------------- setters ----------------------------

    /**
     * Set if generated .classpath file should contain javadoc link for libraries.
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
     * If the build script depends on external libraries, you must set the resolver of these dependencies here.
     */
    public JkEclipseClasspathGenerator setJekaSrcDependencies(JkDependencyResolver buildDependencyResolver,
                                                              JkDependencySet buildDependencies) {
        this.defDependencyResolver = buildDependencyResolver;
        this.defDependencies = buildDependencies;
        return this;
    }

    /**
     * For the specified dependency, specify a child attribute tag to add to the mapping classpathentry tag.
     * @param dependency The dependency paired to the classpathentry we want to generate `<attributes></attributes>` children
     *                   for. It can be a {@link dev.jeka.core.api.depmanagement.JkCoordinateDependency} or a
     *                   {@link dev.jeka.core.api.depmanagement.JkFileSystemDependency}.
     *                   If it is a module dependency, it can be a direct or transitive dependency and only group:name
     *                   is relevant.
     */
    public JkEclipseClasspathGenerator addAttribute(JkDependency dependency, String name, String value) {
        this.attributes.putIfAbsent(dependency, new Properties());
        this.attributes.get(dependency).put(name, value);
        return this;
    }

    /**
     * @See #addAttribute.
     */
    public JkEclipseClasspathGenerator addAttributes(JkDependency dependency, Properties attributes) {
        this.attributes.putIfAbsent(dependency, new Properties());
        this.attributes.get(dependency).putAll(attributes);
        return this;
    }

    /**
     * For the specified dependency, specify a child accessrule tag to add to the mapping classpathentry tag.
     * @param dependency The dependency paired to the classpathentry we want to generate `<attributes></attributes>` children
     *                   for. It can be a {@link dev.jeka.core.api.depmanagement.JkCoordinateDependency} or a
     *                   {@link dev.jeka.core.api.depmanagement.JkFileSystemDependency}.
     *                   If it is a module dependency, it can be a direct or transitive dependency and only group:name
     *                   is relevant.
     */
    public JkEclipseClasspathGenerator addAccessRule(JkDependency dependency, String kind, String pattern) {
        this.accessRules.putIfAbsent(dependency, new Properties());
        this.accessRules.get(dependency).put(kind, pattern);
        return this;
    }

    /**
     * @see  #addAccessRule(JkDependency, String, String)
     */
    public JkEclipseClasspathGenerator addAccessRules(JkDependency dependency, Properties rules) {
        this.accessRules.putIfAbsent(dependency, new Properties());
        this.accessRules.get(dependency).putAll(rules);
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

        // Write sources for jeka-src
        if (hasJekaSrcDir()) {
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "kind", "src",
                    "including", "**/*",
                    "path", JkConstants.JEKA_SRC_DIR);
        }
        generateSrcAndTestSrc(writer);

        // write entries for jeka-src imported projects
        for (final Path projectFile : this.importedProjects) {
            if (!paths.add(projectFile.toAbsolutePath().toString())) {
                continue;
            }
            writer.writeCharacters("\t");
            writeClasspathEl(writer, "combineaccessrules", "false", "kind", "src", "exported", "true",
                    "path", "/" + projectFile.getFileName().toString());
        }

        if (ideSupport.getDependencyResolver() != null) {
            writeDependenciesEntries(writer, ideSupport.getDependencies(), ideSupport.getDependencyResolver(), paths);
        }
        writeJre(writer);

        // add jeka-src dependencies
        if (hasJekaSrcDir() && defDependencyResolver != null) {
            JkQualifiedDependencySet qualifiedDependencies =
                    JkQualifiedDependencySet.ofDependencies(defDependencies.getEntries());
            writeDependenciesEntries(writer, qualifiedDependencies, defDependencyResolver, paths);
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
        writer.writeEmptyElement(CLASSPATH_ENTRY);
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            writer.writeAttribute(entry.getKey(), entry.getValue());
        }
        writer.writeCharacters("\n");
    }

    private static String eclipseJavaVersion(JkJavaVersion compilerVersion) {
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
        writer.writeEmptyElement(CLASSPATH_ENTRY);
        writer.writeAttribute("kind", "con");
        final String container;
        if (jreContainer != null) {
            container = jreContainer;
        } else {
            if (ideSupport.getSourceVersion() != null) {
                container = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/"
                        + eclipseJavaVersion(ideSupport.getSourceVersion());
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
            source = file.resolveSibling("../sources/" + name + "-sources.jar");
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
        JkCompileLayout testLayout = ideSupport.getTestLayout();
        for (final JkPathTree fileTree : testLayout.resolveSources().and(testLayout.resolveResources()).toList()) {
            if (!fileTree.exists()) {
                continue;
            }
            final String path = testLayout.getBaseDir().relativize(fileTree.getRoot()).toString().replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(CLASSPATH_ENTRY);
            writer.writeAttribute("kind", "src");
            writeIncludingExcluding(writer, fileTree);
            writer.writeAttribute("path", path);
            writer.writeCharacters("\n");
        }

        // Sources
        JkCompileLayout prodLayout = ideSupport.getProdLayout();
        for (final JkPathTree fileTree : prodLayout.resolveSources().and(prodLayout.resolveResources()).toList()) {
            if (!fileTree.exists()) {
                continue;
            }
            final String path = prodLayout.getBaseDir().relativize(fileTree.getRoot()).toString().replace(File.separator, "/");
            if (sourcePaths.contains(path)) {
                continue;
            }
            sourcePaths.add(path);
            writer.writeCharacters("\t");
            writer.writeEmptyElement(CLASSPATH_ENTRY);
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

    private void writeDependenciesEntries(XMLStreamWriter writer, JkQualifiedDependencySet dependencies,
                                          JkDependencyResolver resolver, Set<String> allPaths) throws XMLStreamException {

        // dependencies with IDE project dir will be omitted. The project dir will be added in other place.
        List<JkDependency> depList = dependencies.getEntries().stream()
                .map(JkQualifiedDependency::getDependency)
                .filter(dep -> dep.getIdeProjectDir() == null)
                .collect(Collectors.toList());
        JkDependencySet deps = JkDependencySet.of(depList).andVersionProvider(dependencies.getVersionProvider());
        final JkResolveResult resolveResult = resolver.resolve(deps);
        final JkRepoSet repos = resolver.getRepos();
        for (final JkResolvedDependencyNode node : resolveResult.getDependencyTree().toFlattenList()) {
            // Maven dependency
            if (node.isModuleNode()) {
                final JkResolvedDependencyNode.JkModuleNodeInfo moduleNodeInfo = node.getModuleInfo();
                JkCoordinate coordinate = JkCoordinate.of(moduleNodeInfo.getModuleId().toColonNotation());
                JkDependency dependency = JkCoordinateDependency.of(coordinate);
                Properties attributeProps = copyOfPropsOf(dependency, this.attributes);
                Properties accessruleProps = copyOfPropsOf(dependency, this.accessRules);
                writeModuleEntry(writer,
                        moduleNodeInfo.getResolvedCoordinate(),
                        moduleNodeInfo.getFiles(), repos, allPaths, attributeProps, accessruleProps);

                // File dependencies (file system + computed)
            } else {
                final JkResolvedDependencyNode.JkFileNodeInfo fileNodeInfo = (JkResolvedDependencyNode.JkFileNodeInfo) node.getNodeInfo();
                if (fileNodeInfo.isComputed()) {
                    final JkComputedDependency computedDependency = fileNodeInfo.computationOrigin();
                    final Path ideProjectBaseDir = computedDependency.getIdeProjectDir();
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

    private void writeModuleEntry(XMLStreamWriter writer, JkCoordinate coordinate, Iterable<Path> files,
                                  JkRepoSet repos, Set<String> paths, Properties attributeProps,
                                  Properties accessRuleProps) throws XMLStreamException {
        final Path source = repos.get(coordinate.withClassifiers("sources"));
        Path javadoc = null;
        if (source == null || !Files.exists(source) || this.includeJavadoc) {
            javadoc = repos.get(coordinate.withClassifiers("javadoc"));
        }
        if (javadoc != null) {
            attributeProps.put("javadoc_location", javadocAttributeValue(javadoc));
        }
        for (final Path file : files) {
            writeClasspathEl(writer, file, source, attributeProps, accessRuleProps, paths);
        }
    }

    private void writeClasspathEl(XMLStreamWriter writer, Path jar, Path source, Properties attributeProps,
                                  Properties accesRuleProps, Set<String> paths)
            throws XMLStreamException {
        String binPath = jar.toAbsolutePath().toString();
        if (!paths.add(binPath)) {
            return;
        }
        boolean usePathVariable = usePathVariables && jar.startsWith(JkLocator.getJekaUserHomeDir());
        boolean isVar = true;
        if (usePathVariable) {
            binPath = JEKA_USER_HOME + "/" + JkLocator.getJekaUserHomeDir().relativize(jar);
        } else {
            isVar = false;
            binPath = relativePathIfPossible(ideSupport.getProdLayout().getBaseDir(), jar).toString();
        }
        binPath = binPath.replace(File.separator, "/");
        writer.writeCharacters("\t");
        boolean emptyTag = attributeProps.isEmpty() && accesRuleProps.isEmpty();
        if (emptyTag) {
            writer.writeEmptyElement(CLASSPATH_ENTRY);
        } else {
            writer.writeStartElement(CLASSPATH_ENTRY);
        }
        writer.writeAttribute("kind", isVar ? "var" : "lib");
        writer.writeAttribute("path", binPath);
        writer.writeAttribute("exported", "true");
        if (source != null && Files.exists(source)) {
            String srcPath;
            if (usePathVariables && source.startsWith(JkLocator.getJekaUserHomeDir())) {
                srcPath = JEKA_USER_HOME + "/" + JkLocator.getJekaUserHomeDir().relativize(source).toString();
            } else if (usePathVariables && source.startsWith(JkLocator.getJekaHomeDir())) {
                srcPath = JEKA_HOME + "/" + JkLocator.getJekaHomeDir().relativize(source).toString();
            } else {
                srcPath = relativePathIfPossible(ideSupport.getProdLayout().getBaseDir(), source).toString();
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
        if (dep1 instanceof JkCoordinateDependency) {
            if (dep2 instanceof JkCoordinateDependency) {
                JkCoordinateDependency modDep1 = (JkCoordinateDependency) dep1;
                JkCoordinateDependency modDep2 = (JkCoordinateDependency) dep2;
                return modDep1.getCoordinate().getModuleId().equals(modDep2.getCoordinate().getModuleId());
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
