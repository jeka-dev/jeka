package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkProjectDependencies;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJarPacker;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Responsible to produce jar files. It  compilation and unit testing.
 * Compilation and tests can be run independently without creating jars.
 * <p>
 * Java Project Jar Production has common characteristics :
 * <ul>
 *     <li>Contains Java source files to be compiled</li>
 *     <li>All Java sources file (prod + test) are wrote against the same Java version and encoding</li>
 *     <li>The project may contain unit tests</li>
 *     <li>It can depends on any accepted dependencies (Maven module, other project, files on fs, ...)</li>
 *     <li>Part of the sources/resources may be generated</li>
 *     <li>By default, passing test suite is required to produce Jars.</li>
 * </ul>
 * Integration tests are outside of {@link JkProjectConstruction} scope.
 */
public class JkProjectConstruction {

    private static final JkJavaVersion DEFAULT_JAVA_VERSION = JkJavaVersion.V8;

    private static final String DEFAULT_ENCODING = "UTF-8";

    private final JkProject project;

    private final JkJavaCompiler<JkProjectConstruction> compiler;

    private JkJavaVersion jvmTargetVersion = DEFAULT_JAVA_VERSION;

    private String sourceEncoding = DEFAULT_ENCODING;

    private final JkDependencyResolver<JkProjectConstruction> dependencyResolver;

    private final JkProjectCompilation<JkProjectConstruction> compilation;

    private final JkProjectTesting testing;

    private PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    private final JkManifest manifest;

    private JkPathTreeSet fatJarContentCustomizer = JkPathTreeSet.ofEmpty();

    private UnaryOperator<JkDependencySet> dependencySetModifier = x -> x;

    private boolean textAndLocalDependenciesAdded;
    
    /**
     * For Parent chaining
     */
    public JkProject __;

    JkProjectConstruction(JkProject project) {
        this.project = project;
        this.__ = project;
        dependencyResolver = JkDependencyResolver.ofParent(this)
                .addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral())
                .setUseCache(true);
        compiler = JkJavaCompiler.ofParent(this);
        compilation = JkProjectCompilation.ofProd(this);
        testing = new JkProjectTesting(this);
        manifest = JkManifest.ofParent(this);
    }

    public JkProjectConstruction apply(Consumer<JkProjectConstruction> consumer) {
        consumer.accept(this);
        return this;
    }

    public JkDependencyResolver<JkProjectConstruction> getDependencyResolver() {
        return dependencyResolver;
    }

    /**
     * Returns the compiler compiling Java sources for this project. The returned instance is mutable
     * so users can modify it from this method return.
     */
    public JkJavaCompiler<JkProjectConstruction> getCompiler() {
        return compiler;
    }

    /**
     * Sets the Java version used for both source and target.
     */
    public JkProjectConstruction setJvmTargetVersion(JkJavaVersion jvmTargetVersion) {
        this.jvmTargetVersion = jvmTargetVersion;
        return this;
    }

    /**
     * Gets the Java version used as source and target version
     */
    public JkJavaVersion getJvmTargetVersion() {
        return jvmTargetVersion != null ? jvmTargetVersion : DEFAULT_JAVA_VERSION;
    }

    /**
     * Returns encoding to use to read Java source files
     */
    public String getSourceEncoding() {
        return sourceEncoding;
    }

    /**
     * Set the encoding to use to read Java source files
     */
    public JkProjectConstruction setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
        return this;
    }

    public JkProjectCompilation<JkProjectConstruction> getCompilation() {
        return compilation;
    }

    public JkProjectDependencies getProjectDependencies() {
        return JkProjectDependencies.of(compilation.getDependencies(), getRuntimeDependencies(),
                testing.getCompilation().getDependencies());
    }

    public JkProjectTesting getTesting() {
        return testing;
    }

    public JkManifest<JkProjectConstruction> getManifest() {
        return manifest;
    }

    JkProject getProject() {
        return project;
    }

    private void addManifestDefaults() {
        JkModuleId moduleId = project.getPublication().getModuleId();
        String version = project.getPublication().getVersion().getValue();
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_TITLE) == null && moduleId != null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_TITLE, moduleId.getName());
        }
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_VENDOR) == null && moduleId != null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_VENDOR, moduleId.getGroup());
        }
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_VERSION) == null && version != null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_VERSION, version);
        }
    }

    public void createBinJar(Path target) {
        compilation.runIfNeeded();
        testing.runIfNeeded();
        addManifestDefaults();
        JkJarPacker.of(compilation.getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeJar(target);
    }

    public void createBinJar() {
        createBinJar(project.getArtifactProducer().getArtifactPath(JkArtifactId.ofMainArtifact("jar")));
    }

    public void createFatJar(Path target) {
        compilation.runIfNeeded();
        testing.runIfNeeded();
        JkLog.startTask("Packing fat jar...");
        Iterable<Path> classpath = resolveRuntimeDependencies().getFiles();
        addManifestDefaults();
        JkJarPacker.of(compilation.getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeFatJar(target, classpath, this.fatJarFilter);
        JkLog.endTask();
    }

    public void createFatJar() {
        createFatJar(project.getArtifactProducer().getArtifactPath(JkArtifactId.of("fat", "jar")));
    }

    public JkPathTreeSet getExtraFilesToIncludeInJar() {
        return this.fatJarContentCustomizer;
    }

    /**
     * Allows customizing thz content of produced fat jar.
     */
    public JkProjectConstruction customizeFatJarContent(Function<JkPathTreeSet, JkPathTreeSet> customizer) {
        this.fatJarContentCustomizer = customizer.apply(fatJarContentCustomizer);
        return this;
    }

    /**
     * Specify the dependencies to add or remove from the production compilation dependencies to
     * get the runtime dependencies.
     * @param modifier An function that define the runtime dependencies from the compilation ones.
     */
    public JkProjectConstruction configureRuntimeDependencies(UnaryOperator<JkDependencySet> modifier) {
        this.dependencySetModifier = modifier;
        return this;
    }

    public JkDependencySet getRuntimeDependencies() {
        return dependencySetModifier.apply(compilation.getDependencies());
    }

    public JkResolveResult resolveRuntimeDependencies() {
        return dependencyResolver.resolve(getRuntimeDependencies()
                .normalised(project.getDuplicateConflictStrategy()));
    }

    public void addTextAndLocalDependencies() {
        if (textAndLocalDependenciesAdded) {
            return;
        }
        Path baseDir = project.getBaseDir();
        JkProjectDependencies localDeps = JkProjectDependencies.ofLocal(
                baseDir.resolve(JkConstants.JEKA_DIR + "/libs"));
        JkProjectDependencies textDeps = JkProjectDependencies.ofTextDescriptionIfExist(
                baseDir.resolve(JkConstants.JEKA_DIR + "/libs/dependencies.txt"));
        JkProjectDependencies extraDeps = localDeps.and(textDeps);
        getCompilation().configureDependencies(deps -> deps.and(extraDeps.getCompile()));
        configureRuntimeDependencies(deps -> deps.and(extraDeps.getRuntime()));
        getTesting().getCompilation().configureDependencies(deps -> extraDeps.getTest().and(deps));
        textAndLocalDependenciesAdded = true;
    }

    public Document getDependenciesAsXml()  {
        Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Element root = document.createElement("dependencies");
        document.appendChild(root);
        root.appendChild(xmlDeps(document, "compile", getCompilation().getDependencies()));
        root.appendChild(xmlDeps(document, "runtime", getRuntimeDependencies()));
        root.appendChild(xmlDeps(document, "test", getTesting().getCompilation().getDependencies()));
        return document;
    }

    private Element xmlDeps(Document document, String purpose, JkDependencySet deps) {
        JkResolveResult resolveResult = this.getProject().getConstruction().getDependencyResolver().resolve(deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        Element element = tree.toDomElement(document, true);
        element.setAttribute("purpose", purpose);
        return element;
    }

}
