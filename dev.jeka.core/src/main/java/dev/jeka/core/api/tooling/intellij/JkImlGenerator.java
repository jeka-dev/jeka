package dev.jeka.core.api.tooling.intellij;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaIdeSupport;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.JkConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
public final class JkImlGenerator {

    private static final String FOR_JEKA_ATTRIUTE = "forJeka";

    private static final String ENCODING = "UTF-8";

    private static final String T1 = "  ";

    private static final String T2 = T1 + T1;

    private static final String T3 = T2 + T1;

    private static final String T4 = T3 + T1;

    private static final String T5 = T4 + T1;

    private final JkJavaIdeSupport ideSupport;

    /** Dependency resolver to fetch module dependencies for build classes */
    private JkDependencyResolver defDependencyResolver;

    private JkDependencySet defDependencies;

    /** Can be empty but not null */
    private Iterable<String> extraJekaModules = JkUtilsIterable.listOf();

    private boolean forceJdkVersion;

    private boolean failOnDepsResolutionError;

    /* When true, path will be mentioned with $JEKA_HOME$ and $JEKA_REPO$ instead of explicit absolute path. */
    private boolean useVarPath;

    // Keep trace of already processed module-library entries to avoid duplicates
    private final Set<String> processedLibEntries = new HashSet<>();

    // Keep trace of already processed module entries to avoid duplicates
    private final Set<String> processedModueEntries = new HashSet<>();

    private XMLStreamWriter writer;

    private Path explicitJekaHome;

    private JkImlGenerator(JkJavaIdeSupport ideSupport) {
        this.ideSupport = ideSupport;
    }

    /**
     * Constructs a {@link JkImlGenerator} to the project base directory
     */
    public static JkImlGenerator of(JkJavaIdeSupport ideSupport) {
        return new JkImlGenerator(ideSupport);
    }

    /** Generate the .classpath file */
    public String generate() {
        try {
            return _generate();
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private String _generate() throws IOException, XMLStreamException, FactoryConfigurationError {
        final ByteArrayOutputStream fos = new ByteArrayOutputStream();
        writer = createWriter(fos);
        writeHead();
        writeOutput();
        writeJdk();
        writeContent();
        writeOrderEntrySourceFolder();
        Context context = new Context();
        JkResolveResult commandResolveResult = null;
        if (this.defDependencyResolver != null) {
            commandResolveResult = resolve(this.defDependencies, this.defDependencyResolver);
            markJekaDeps(commandResolveResult, context);
        }
        if (this.ideSupport.getDependencyResolver()!= null) {
            JkResolveResult resolveResult = resolve(ideSupport.getDependencies(), ideSupport.getDependencyResolver());
            writeDependencies(resolveResult, ideSupport.getDependencyResolver().getRepos(),
                    context, false);
        }
        if (this.defDependencyResolver != null) {
            writeDependencies(commandResolveResult, this.defDependencyResolver.getRepos(), context, true);
        }
        writeExtraJekaLodules(this.extraJekaModules);
        writeFoot();
        writer.close();
        return fos.toString(ENCODING);
    }

    private void writeHead() throws XMLStreamException {
        Path pluginXml = findPluginXml();
        boolean pluginModule = pluginXml != null;
        writer.writeStartDocument(ENCODING, "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("module");
        writer.writeAttribute("type", pluginModule ? "PLUGIN_MODULE" : "JAVA_MODULE");
        writer.writeAttribute("version", "4");
        writer.writeCharacters("\n" + T1);
        if (pluginModule) {
            writer.writeEmptyElement("component");
            writer.writeAttribute("name", "DevKit.ModuleBuildProperties");
            writer.writeAttribute("url", "file://$MODULE_DIR$/" + ideSupport.getProdLayout()
                    .getBaseDir().relativize(pluginXml)
                    .toString().replace("\\", "/"));
            writer.writeCharacters("\n"  + T1);
        }
        writer.writeStartElement("component");
        writer.writeAttribute("name", "NewModuleRootManager");
        writer.writeAttribute("inherit-compileRunner-output", "false");
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
        writer.writeCharacters(T2);
        writer.writeEmptyElement("output");
        writer.writeAttribute("url", "file://$MODULE_DIR$/.idea/output/production");
        writer.writeCharacters("\n");
        writer.writeCharacters(T2);
        writer.writeEmptyElement("output-test");
        writer.writeAttribute("url", "file://$MODULE_DIR$/.idea/output/test");
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
        writer.writeAttribute("url", "file://$MODULE_DIR$/" + JkConstants.DEF_DIR);
        writer.writeAttribute("isTestSource", "true");
        writer.writeCharacters("\n");
        writer.writeCharacters(T2);


        // Write test sources
        final Path projectDir = ideSupport.getProdLayout().getBaseDir();
        if (ideSupport.getTestLayout() != null) {
            for (final JkPathTree fileTree : ideSupport.getTestLayout().resolveSources().toList()) {
                if (fileTree.exists()) {
                    writer.writeCharacters(T1);
                    writer.writeEmptyElement("sourceFolder");

                    final String path = projectDir.relativize(fileTree.getRoot()).normalize().toString().replace('\\', '/');
                    writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
                    writer.writeAttribute("isTestSource", "true");
                    writer.writeCharacters("\n");
                }
            }

            for (final JkPathTree fileTree : ideSupport.getTestLayout().resolveResources().toList()) {
                if (fileTree.exists() && !contains(ideSupport.getTestLayout().resolveSources(), fileTree.getRootDirOrZipFile())) {
                    writer.writeCharacters(T3);
                    writer.writeEmptyElement("sourceFolder");
                    final String path = projectDir.relativize(fileTree.getRoot()).normalize().toString().replace('\\', '/');
                    writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
                    writer.writeAttribute("type", "java-test-resource");
                    writer.writeCharacters("\n");
                }
            }
        }

        // Write production sources

        if (ideSupport.getProdLayout() != null) {
            for (final JkPathTree fileTree : ideSupport.getProdLayout().resolveSources().toList()) {
                if (fileTree.exists()) {
                    writer.writeCharacters(T3);
                    writer.writeEmptyElement("sourceFolder");
                    final String path = projectDir.relativize(fileTree.getRoot()).normalize().toString().replace('\\', '/');
                    writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
                    writer.writeAttribute("isTestSource", "false");
                    writer.writeCharacters("\n");
                }
            }

            // Write production test resources
            for (final JkPathTree fileTree : ideSupport.getProdLayout().resolveResources().toList()) {
                if (fileTree.exists() && !contains(ideSupport.getProdLayout().resolveSources(), fileTree.getRootDirOrZipFile())) {
                    writer.writeCharacters(T3);
                    writer.writeEmptyElement("sourceFolder");
                    final String path = projectDir.relativize(fileTree.getRoot()).normalize().toString().replace('\\', '/');
                    writer.writeAttribute("url", "file://$MODULE_DIR$/" + path);
                    writer.writeAttribute("type", "java-resource");
                    writer.writeCharacters("\n");
                }
            }
        }

        writer.writeCharacters(T3);
        writer.writeEmptyElement("excludeFolder");
        writer.writeAttribute("url", "file://$MODULE_DIR$/" + JkConstants.OUTPUT_PATH);
        writer.writeCharacters("\n");
        writer.writeCharacters(T3);
        writer.writeEmptyElement("excludeFolder");
        writer.writeAttribute("url", "file://$MODULE_DIR$/" + JkConstants.WORK_PATH);
        writer.writeCharacters("\n");
        writer.writeCharacters(T3);
        writer.writeEmptyElement("excludeFolder");
        writer.writeAttribute("url", "file://$MODULE_DIR$/.idea/output");
        writer.writeCharacters("\n");
        writer.writeCharacters(T2);
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private static boolean contains(JkPathTreeSet treeSet, Path path) {
        for (JkPathTree tree : treeSet.toList()) {
            if (JkUtilsPath.isSameFile(tree.getRoot(), path)) {
                return true;
            }
        }
        return false;
    }

    private void writeExtraJekaLodules(Iterable<String> moduleNames)
            throws XMLStreamException {
        for (final String moduleName : moduleNames) {
            if (!processedModueEntries.contains(moduleName)) {
                writeOrderEntryForModule(moduleName, "TEST", true);
                processedModueEntries.add(moduleName);
            }
        }
    }

    private void markJekaDeps(JkResolveResult resolveResult, Context context) {
        final JkDependencyNode tree = resolveResult.getDependencyTree();
        for (final JkDependencyNode node : tree.toFlattenList()) {
            if (node.isModuleNode()) { //maven module
                toLibPath(node.getModuleInfo(), null, null).forEach(libPath -> context.forJekaPaths.add(libPath.bin));
            } else {
                final JkDependencyNode.JkFileNodeInfo fileNodeInfo = (JkDependencyNode.JkFileNodeInfo) node.getNodeInfo();
                if (fileNodeInfo.isComputed()) {
                    final Path projectDir = fileNodeInfo.computationOrigin().getIdeProjectDir();
                    if (projectDir != null && !context.allModules.contains(projectDir)) {
                        context.allModules.add(projectDir);
                    }
                } else {
                    context.forJekaPaths.addAll(fileNodeInfo.getFiles());
                }
            }
        }
    }

    private JkResolveResult resolve(JkDependencySet dependencies, JkDependencyResolver resolver) {
        return resolver.resolve(dependencies.minusModuleDependenciesWithIdeProjectDir());
    }

    private void writeDependencies(JkResolveResult resolveResult, JkRepoSet repos,
                                   Context context,
                                   boolean forceTest) throws XMLStreamException {
        if (resolveResult.getErrorReport().hasErrors()) {
            if (failOnDepsResolutionError) {
                throw new IllegalStateException("Fail at resolvig dependencies : " + resolveResult.getErrorReport());
            } else {
                JkLog.warn(resolveResult.getErrorReport().toString());
                JkLog.warn("The generated iml file won't take in account missing files.");
            }
        }
        final JkDependencyNode tree = resolveResult.getDependencyTree();
        for (final JkDependencyNode node : tree.toFlattenList()) {

            // Maven dependency
            if (node.isModuleNode()) {
                final String ideScope = forceTest ? "TEST" : ideScope(node.getModuleInfo().getResolvedScopes());
                final List<LibPath> paths = toLibPath(node.getModuleInfo(), repos, ideScope);
                for (final LibPath libPath : paths) {
                    if (!context.allPaths.contains(libPath.bin)) {
                        boolean forJeka = context.allPaths.contains(libPath.bin);
                        writeOrderEntryForLib(libPath, context);
                        context.allPaths.add(libPath.bin);
                    }
                }

                // File dependencies (file ofSystem + computed)
            } else {
                final String ideScope = forceTest ? "TEST" : ideScope(node.getNodeInfo().getDeclaredScopes());
                final JkDependencyNode.JkFileNodeInfo fileNodeInfo = (JkDependencyNode.JkFileNodeInfo) node.getNodeInfo();
                if (fileNodeInfo.isComputed()) {
                    final Path projectDir = fileNodeInfo.computationOrigin().getIdeProjectDir();
                    if (projectDir != null && !context.allModules.contains(projectDir)) {
                        boolean forJeka = context.forJekaPaths.contains(projectDir);
                        writeOrderEntryForModule(projectDir.getFileName().toString(), ideScope, forJeka);
                        context.allModules.add(projectDir);
                    }
                } else {
                    writeFileEntries(fileNodeInfo.getFiles(), processedLibEntries, ideScope, context);
                }
            }
        }
    }

    private void writeFileEntries(Iterable<Path> files, Set<String> paths, String ideScope, Context context) throws XMLStreamException {
        for (final Path file : files) {
            final LibPath libPath = new LibPath();
            libPath.bin = file;
            libPath.scope = ideScope;
            libPath.source = lookForSources(file);
            libPath.javadoc = lookForJavadoc(file);
            writeOrderEntryForLib(libPath, context);
            paths.add(file.toString());
        }
    }

    private List<LibPath> toLibPath(JkDependencyNode.JkModuleNodeInfo moduleInfo, JkRepoSet repos, String scope) {
        final List<LibPath> result = new LinkedList<>();
        final JkModuleId moduleId = moduleInfo.getModuleId();
        final JkVersion version = moduleInfo.getResolvedVersion();
        final JkVersionedModule versionedModule = JkVersionedModule.of(moduleId, version);
        final List<Path> files = moduleInfo.getFiles();
        for (final Path file : files) {
            final LibPath libPath = new LibPath();
            libPath.bin = file;
            libPath.scope = scope;
            if (repos != null) {
                libPath.source = repos.get(JkModuleDependency.of(versionedModule).withClassifier("sources"));
                libPath.javadoc = repos.get(JkModuleDependency.of(versionedModule).withClassifier("javadoc"));
            }
            result.add(libPath);
        }
        return result;
    }

    private static Set<String> toStringScopes(Set<JkScope> scopes) {
        final Set<String> result = new HashSet<>();
        for (final JkScope scope : scopes) {
            result.add(scope.getName());
        }
        return result;
    }

    private static String ideScope(Set<JkScope> scopesArg) {
        final Set<String> scopes = toStringScopes(scopesArg);
        if (scopes.contains(JkScope.COMPILE.getName())) {
            return "COMPILE";
        }
        if (scopes.contains(JkScope.PROVIDED.getName())) {
            return "PROVIDED";
        }
        if (scopes.contains(JkScope.RUNTIME.getName())) {
            return "RUNTIME";
        }
        if (scopes.contains(JkScope.TEST.getName())) {
            return "TEST";
        }
        return "COMPILE";
    }

    private void writeJdk() throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeEmptyElement("orderEntry");
        if (this.forceJdkVersion  && ideSupport.getSourceVersion() != null) {
            writer.writeAttribute("type", "jdk");
            final String jdkVersion = jdkVersion(this.ideSupport.getSourceVersion());
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

    private void writeOrderEntryForLib(LibPath libPath, Context context) throws XMLStreamException {
        writer.writeCharacters(T2);
        writer.writeStartElement("orderEntry");
        writer.writeAttribute("type", "module-library");
        if (libPath.scope != null) {
            writer.writeAttribute("scope", libPath.scope);
        }
        writer.writeAttribute("exported", "");
        if (context.forJekaPaths.contains(libPath.bin)) {
            writer.writeAttribute(FOR_JEKA_ATTRIUTE, "");
        }
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

    private void writeOrderEntryForModule(String ideaModuleName, String scope, boolean forJeka) throws XMLStreamException {
        if (processedModueEntries.contains(ideaModuleName)) {
            return;
        }
        processedModueEntries.add(ideaModuleName);
        writer.writeCharacters(T2);
        writer.writeEmptyElement("orderEntry");
        writer.writeAttribute("type", "module");
        if (scope != null) {
            writer.writeAttribute("scope", scope);
        }
        writer.writeAttribute("module-name", ideaModuleName);
        writer.writeAttribute("exported", "");
        if (forJeka) {
            writer.writeAttribute(FOR_JEKA_ATTRIUTE, "");
        }
        writer.writeCharacters("\n");
    }

    private void writeLibType(String type, Path file) throws XMLStreamException {
        writer.writeCharacters(T4);
        if (file != null) {
            writer.writeStartElement(type);
            writer.writeCharacters("\n");
            writer.writeCharacters(T5);
            writer.writeEmptyElement("root");
            writer.writeAttribute("url", ideaPath(ideSupport.getProdLayout().getBaseDir(), file));
            writer.writeCharacters("\n" + T4);
            writer.writeEndElement();
        } else {
            writer.writeEmptyElement(type);
        }
    }

    private String ideaPath(Path projectDir, Path file) {
        boolean jarFile = file.getFileName().toString().toLowerCase().endsWith(".jar");
        String type = jarFile ? "jar" : "file";
        Path basePath  = projectDir;
        String varName = "MODULE_DIR";
        if (useVarPath && file.toAbsolutePath().startsWith(JkLocator.getJekaUserHomeDir())) {
            basePath = JkLocator.getJekaUserHomeDir();
            varName = "JEKA_USER_HOME";
        } else if (useVarPath && file.toAbsolutePath().startsWith(jekaHome())) {
            basePath = jekaHome();
            varName = "JEKA_HOME";
        }
        String result;
        if (file.startsWith(basePath)) {
            final String relPath = basePath.relativize(file).normalize().toString();
            result = type + "://$" + varName + "$/" + replacePathWithVar(relPath).replace('\\', '/');
        } else {
            if (file.isAbsolute()) {
                result = type + "://" + file.normalize().toString().replace('\\', '/');
            } else {
                result = type + "://$MODULE_DIR$/" + file.normalize().toString().replace('\\', '/');
            }
        }
        if (jarFile) {
            result = result + "!/";
        }
        return result;
    }

    private Path jekaHome() {
        if (explicitJekaHome != null) {
            return explicitJekaHome;
        }
        return JkLocator.getJekaHomeDir();
    }

    private static String jdkVersion(JkJavaVersion javaVersion) {
        if (JkJavaVersion.V1_4.equals(javaVersion)) {
            return "1.4";
        }
        if (JkJavaVersion.V5.equals(javaVersion)) {
            return "1.5";
        }
        if (JkJavaVersion.V6.equals(javaVersion)) {
            return "1.6";
        }
        if (JkJavaVersion.V7.equals(javaVersion)) {
            return "1.7";
        }
        if (JkJavaVersion.V8.equals(javaVersion)) {
            return "1.8";
        }
        return javaVersion.get();
    }

    private static class LibPath {
        Path bin;
        Path source;
        Path javadoc;
        String scope;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final LibPath libPath = (LibPath) o;

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
        final String userHome = JkLocator.getJekaUserHomeDir().toAbsolutePath().normalize().toString().replace('\\', '/');
        final String home = jekaHome().toAbsolutePath().normalize().toString().replace('\\', '/');
        final String result = path.replace(userHome, "$JEKA_USER_HOME$");
        if (!result.equals(path)) {
            return result;
        }
        return path.replace(home, "$JEKA_HOME$");
    }

    private static XMLStreamWriter createWriter(ByteArrayOutputStream fos) {
        try {
            return XMLOutputFactory.newInstance().createXMLStreamWriter(fos, ENCODING);
        } catch (final XMLStreamException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    private Path lookForSources(Path binary) {
        final String name = binary.getFileName().toString();
        final String nameWithoutExt = JkUtilsString.substringBeforeLast(name, ".");
        final String ext = JkUtilsString.substringAfterLast(name, ".");
        final String sourceName = nameWithoutExt + "-sources." + ext;
        final List<Path> folders = JkUtilsIterable.listOf(
                binary.resolve(".."),
                binary.resolve("../../../libs-sources"),
                binary.resolve("../../libs-sources"),
                binary.resolve("../libs-sources"));
        final List<String> names = JkUtilsIterable.listOf(sourceName, nameWithoutExt + "-sources.zip");
        return lookFileHere(folders, names);
    }

    private Path lookForJavadoc(Path binary) {
        final String name = binary.getFileName().toString();
        final String nameWithoutExt = JkUtilsString.substringBeforeLast(name, ".");
        final String ext = JkUtilsString.substringAfterLast(name, ".");
        final String sourceName = nameWithoutExt + "-javadoc." + ext;
        final List<Path> folders = JkUtilsIterable.listOf(
                binary.resolve(".."),
                binary.resolve("../../../libs-javadoc"),
                binary.resolve("../../libs-javadoc"),
                binary.resolve("../libs-javadoc"));
        final List<String> names = JkUtilsIterable.listOf(sourceName, nameWithoutExt + "-javadoc.zip");
        return lookFileHere(folders, names);
    }

    private Path lookFileHere(Iterable<Path> folders, Iterable<String> names) {
        for (final Path folder : folders) {
            for (final String name : names) {
                final Path candidate = folder.resolve(name).normalize();
                if (Files.exists(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    // --------------------------- setters ------------------------------------------------


    public JkImlGenerator setExtraJekaModules(Iterable<String> extraModules) {
        this.extraJekaModules = extraModules;
        return this;
    }

    public JkImlGenerator setForceJdkVersion(boolean forceJdkVersion) {
        this.forceJdkVersion = forceJdkVersion;
        return this;
    }

    public JkImlGenerator setUseVarPath(boolean useVarPath) {
        this.useVarPath = useVarPath;
        return this;
    }

    public JkImlGenerator setFailOnDepsResolutionError(boolean fail) {
        this.failOnDepsResolutionError = fail;
        return this;
    }

    public JkImlGenerator setDefDependencyResolver(JkDependencyResolver defDependencyResolver) {
        this.defDependencyResolver = defDependencyResolver;
        return this;
    }

    public JkImlGenerator setDefDependencies(JkDependencySet defDependencies) {
        this.defDependencies = defDependencies;
        return this;
    }

    public JkImlGenerator setExplicitJekaHome(Path jekaHome) {
        this.explicitJekaHome = jekaHome;
        return this;
    }

    public JkImlGenerator setWriter(XMLStreamWriter writer) {
        this.writer = writer;
        return this;
    }

    private Path findPluginXml() {
        List<Path> candidates = ideSupport.getProdLayout().resolveResources().getExistingFiles("META-INF/plugin.xml");
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream().filter(JkImlGenerator::isPlateformPlugin).findFirst().orElse(null);
    }

    private static boolean isPlateformPlugin(Path pluginXmlFile) {
        try {
            Document doc = JkUtilsXml.documentFrom(pluginXmlFile);
            Element root = JkUtilsXml.directChild(doc, "idea-plugin");
            return  root != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static class Context {
        Set<Path> allModules = new HashSet<>();
        Set<Path> allPaths  = new HashSet<>();
        Set<Path> forJekaModules = new HashSet<>();
        Set<Path> forJekaPaths = new HashSet<>();
    }
}
