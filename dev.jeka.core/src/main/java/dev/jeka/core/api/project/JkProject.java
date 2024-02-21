package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Stands for the whole project model for building purpose. It has the same purpose and scope than the Maven <i>POM</i> but
 * it also holds methods to actually perform builds.<br/>
 * A complete overview is available <a href="https://jeka-dev.github.io/jeka/reference-guide/build-library-project-build/#project-api">here</a>.
 * <p/>
 *
 * A <code>JkProject</code> consists in 4 main parts : <ul>
 *    <li>{@link JkProjectCompilation} : responsible to generate compile production sources</li>
 *    <li>{@link JkProjectTesting} : responsible to compile test sources and run tests </li>
 *    <li>{@link JkProjectPackaging} : responsible to creates javadoc, sources and jars</li>
 *    <li>{@link JkMavenPublication} : responsible to publish the artifacts on Maven repositories</li>
 * </ul>
 * The {@link JkProject#pack()} method is supposed to generates the artifacts
 * (or simply perform actions as deploying docker image) locally. By default, it generates
 * a regular binary jar, but it can be customized for your needs.
 */
public final class JkProject implements JkIdeSupportSupplier {

    /**
     * This constant represents the value "auto" and is used in {@link JkProjectPackaging#setMainClass(String)} (String)}
     * to indicate that the main class should be discovered automatically..
     */
    public static final String AUTO_FIND_MAIN_CLASS = "auto";
    public static final String DEPENDENCIES_TXT_FILE = "dependencies.txt";
    public static final String PROJECT_LIBS_DIR = "libs";

    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Provides conventional path where artifact files are supposed to be generated.
     */
    public final JkArtifactLocator artifactLocator;

    /**
     * Actions to execute when {@link JkProject#pack()} is invoked.<p>
     * By default, the build action creates a regular binary jar. It can be
     * replaced by an action creating other jars/artifacts or doing special
     * action as publishing a Docker image, for example.
     */
    public final JkRunnables packActions = JkRunnables.of();

    private Consumer<Path> jarMaker;

    /**
     * Object responsible for resolving dependencies.
     */
    public final JkDependencyResolver dependencyResolver;

    /**
     * Defines the tool for compiling both production and test Java sources for this project.
     */
    public final JkJavaCompilerToolChain compilerToolChain;

    /**
     * Object responsible for generating and compiling production sources.
     */
    public final JkProjectCompilation compilation;

    /**
     * Object responsible for creating binary, fat, javadoc and sources jars.
     * It also defines the runtime dependencies of this project.
     */
    public final JkProjectPackaging packaging;

    /**
     * Object responsible for compiling and running tests.
     */
    public final JkProjectTesting testing;

    /**
     * Function to modify the {@link JkIdeSupport} used for configuring IDEs.
     */
    public Function<JkIdeSupport, JkIdeSupport> ideSupportModifier = x -> x;

    /**
     * Defines extra actions to execute when {@link JkProject#clean()} is invoked.
     */
    public final JkRunnables cleanExtraActions = JkRunnables.of();

    private Path baseDir = Paths.get("");

    private String outputDir = JkConstants.OUTPUT_PATH;

    private JkJavaVersion jvmTargetVersion;

    private String sourceEncoding = DEFAULT_ENCODING;

    private JkModuleId moduleId;

    private JkVersion version;

    private Supplier<JkVersion> versionSupplier = () -> null;

    private JkCoordinate.ConflictStrategy duplicateConflictStrategy = JkCoordinate.ConflictStrategy.FAIL;

    private boolean includeTextAndLocalDependencies = true;

    private LocalAndTxtDependencies cachedTextAndLocalDeps;

    private URL dependencyTxtUrl;

    private JkProject() {
        artifactLocator = artifactLocator();
        compilerToolChain = JkJavaCompilerToolChain.of();
        compilation = JkProjectCompilation.ofProd(this);
        testing = new JkProjectTesting(this);
        packaging = new JkProjectPackaging(this);
        dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral())
                .setDisplaySpinner(true)
                .setUseInMemoryCache(true);
        jarMaker = packaging::createBinJar;
        packActions.set(() -> jarMaker.accept(artifactLocator.getMainArtifactPath()));
    }

    /**
     * Creates a new project having the current working directory as base dir.
     */
    public static JkProject of() {
        return new JkProject();
    }

    /**
     * Applies the specified configuration consumer to this project.
     */
    public JkProject apply(Consumer<JkProject> projectConsumer) {
        projectConsumer.accept(this);
        return this;
    }

    /**
     * Returns a convenient facade for configuring the project.
     */
    public JkProjectFlatFacade flatFacade() {
        return new JkProjectFlatFacade(this);
    }

    // ---------------------------- Getters / setters --------------------------------------------

    /**
     * Returns the base directory from where are resolved all relative path concerning this
     * project (source folder, ...)
     */
    public Path getBaseDir() {
        return this.baseDir;
    }

    /**
     * Sets the base directory for this project.
     *
     * @see #getBaseDir()
     */
    public JkProject setBaseDir(Path baseDir) {
        this.baseDir = JkUtilsPath.relativizeFromWorkingDir(baseDir);
        return this;
    }
    
    /**
     * Returns path of the directory under which are produced build files
     */
    public Path getOutputDir() {
        return baseDir.resolve(outputDir);
    }

    /**
     * Sets the output path dir relative to base dir.
     */
    public JkProject setOutputDir(String relativePath) {
        this.outputDir = relativePath;
        return this;
    }

    /**
     * Returns the Java JVM version that will be used to compile sources and
     * generate bytecode.
     */
    public JkJavaVersion getJvmTargetVersion() {
        return jvmTargetVersion;
    }

    /**
     * Sets the Java JVM version that will be used to compile sources and
     * generate bytecode.
     */
    public JkProject setJvmTargetVersion(JkJavaVersion jvmTargetVersion) {
        this.jvmTargetVersion = jvmTargetVersion;
        return this;
    }

    /**
     * Returns the source encoding of source files.
     */
    public String getSourceEncoding() {
        return sourceEncoding;
    }

    /**
     * Sets the source encoding of source files.
     */
    public JkProject setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
        return this;
    }

    /**
     * Returns the strategy to use when twi dependencies with distinct versions
     * are declared in the project dependencies.
     */
    public JkCoordinate.ConflictStrategy getDuplicateConflictStrategy() {
        return duplicateConflictStrategy;
    }

    /**
     * Sets the strategy to use when twi dependencies with distinct versions
     * are declared in the project dependencies.
     */
    public JkProject setDuplicateConflictStrategy(JkCoordinate.ConflictStrategy duplicateConflictStrategy) {
        this.duplicateConflictStrategy = duplicateConflictStrategy;
        return this;
    }

    // ------------------------- Run ---------------------------


    /**
     * Sets a {@link Runnable} to create the JAR used by {@link #prepareRunJar(boolean)}
     */
    public JkProject setJarMaker(Consumer<Path> jarMaker) {
        this.jarMaker = jarMaker;
        return this;
    }

    /**
     * Creates {@link JkProcess} to execute the main method for this project.
     */
    public JkJavaProcess prepareRunMain() {
        String mainClass = packaging.getMainClass();
        JkUtilsAssert.state(mainClass != null, "No main class defined or found on this project.");
        Path classDir = compilation.layout.resolveClassDir();
        if (!JkPathTree.of(classDir).containFiles()) {
            compilation.run();
        }
        JkPathSequence classpath = JkPathSequence.of(this.compilation.layout.resolveClassDir())
                .and(this.packaging.resolveRuntimeDependenciesAsFiles());
        return JkJavaProcess.ofJava(mainClass)
                .setClasspath(classpath)
                .setDestroyAtJvmShutdown(true)
                .setLogCommand(true)
                .setInheritIO(true);
    }

    /**
     * Creates {@link JkJavaProcess} to execute the jar having the specified artifact name.
     * The jar is created on the fly if it is not already present.
     *
     * @param artifactClassifier  The classifier of the artifact to run. In a project producing a side 'fat' jar, you can use
     *                           'fat'. If you want to run the main artifact, just use ''.
     * @param includeRuntimeDeps If <code>true</code>, the runtime dependencies will be added to the classpath. This should
     *                           values <code>false</code> in case of <i>fat</i> jar.
     */
    public JkJavaProcess prepareRunJar(String artifactClassifier, boolean includeRuntimeDeps) {
        JkArtifactId artifactId = JkArtifactId.of(artifactClassifier, "jar");
        Path artifactPath = artifactLocator.getArtifactPath(artifactId);
        if (!Files.exists(artifactPath)) {
            jarMaker.accept(artifactPath);
        }
        JkJavaProcess javaProcess = JkJavaProcess.ofJavaJar(artifactPath)
                .setDestroyAtJvmShutdown(true)
                .setLogCommand(true)
                .setInheritIO(true);
        if (includeRuntimeDeps) {
            javaProcess.setClasspath(packaging.resolveRuntimeDependenciesAsFiles());
        }
        return javaProcess;
    }

    /**
     * Same as {@link #prepareRunJar(String, boolean)} but specific for the main artefact.
     *
     * @see #prepareRunJar(String, boolean)
     */
    public JkJavaProcess prepareRunJar(boolean includeRuntimeDeps) {
        return prepareRunJar(JkArtifactId.MAIN_ARTIFACT_CLASSIFIER, includeRuntimeDeps);
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + getBaseDir().toAbsolutePath().getFileName();
    }

    /**
     * Cleans directories where are generated files output by the project builds
     * (jars, generated sources, classes, reports, ...)
     */
    public JkProject clean() {
        Path output = getOutputDir();
        JkLog.verbose("Clean output directory %s", output.toAbsolutePath().normalize());
        if (Files.exists(output)) {
            JkPathTree.of(output).deleteContent();
        }
        cleanExtraActions.run();
        return this;
    }

    /**
     * Returns a human-readable text that mentions various settings for this project
     * (source locations, file count, declared dependencies, ...).
     */
    public String getInfo() {
        JkDependencySet compileDependencies = compilation.getDependencies();
        JkDependencySet runtimeDependencies = packaging.getRuntimeDependencies();
        JkDependencySet testDependencies = testing.compilation.getDependencies();
        StringBuilder builder = new StringBuilder();

        String declaredMainClassName = packaging.declaredMainClass();
        if (JkProject.AUTO_FIND_MAIN_CLASS.equals(declaredMainClassName)) {
            if (JkPathTree.of(compilation.layout.resolveClassDir()).exists()) {
                String effectiveMainClassName = packaging.getMainClass();
                if (effectiveMainClassName == null) {
                    effectiveMainClassName = "not main class found";
                }
                declaredMainClassName += " (" + effectiveMainClassName + ")";
            }
        }

        Jk2ColumnsText columnsText = Jk2ColumnsText.of(30, 200).setAdjustLeft(true)
                .add("ModuleId", moduleId)
                .add("Version", getVersion() )
                .add("Base Dir", this.getBaseDir().toAbsolutePath().normalize())
                .add("Production Sources", compilation.layout.getInfo())
                .add("Test Sources", testing.compilation.layout.getInfo())
                .add("Java Version", jvmTargetVersion == null ? "Unspecified" : jvmTargetVersion  )
                .add("Source Encoding", sourceEncoding + "\n")
                .add("Source File Count", compilation.layout.resolveSources()
                        .count(Integer.MAX_VALUE, false))
                .add("Test Source file count       ", testing.compilation.layout.resolveSources()
                        .count(Integer.MAX_VALUE, false))
                .add("Main Class Name", declaredMainClassName)
                .add("Download Repositories", dependencyResolver.getRepos().getRepos().stream()
                        .map(repo -> repo.getUrl()).collect(Collectors.toList()))
                .add("Manifest", packaging.getManifest().asTrimedString()); // manifest ad extra '' queuing char, this causes a extra empty line when displaying
        builder.append(columnsText.toString());

        if (JkLog.isVerbose()) { // add declared dependencies
            builder.append("\nCompile Dependencies          : \n");
            compileDependencies.getVersionedDependencies().forEach(dep -> builder.append("    " + dep + "\n"));
            builder.append("Runtime Dependencies          : \n");
            runtimeDependencies.getVersionedDependencies().forEach(dep -> builder.append("    " + dep + "\n"));
            builder.append("Test Dependencies             : \n");
            testDependencies.getVersionedDependencies().forEach(dep -> builder.append("    " + dep + "\n"));
        }
        return builder.toString();
    }

    /**
     * Displays the resolved dependency tree on the console.
     */
    public void displayDependencyTree() {
        displayDependencyTree("compile", compilation.getDependencies());
        displayDependencyTree("runtime", packaging.getRuntimeDependencies());
        displayDependencyTree("test", testing.compilation.getDependencies());
    }

    /**
     * Returns an ideSupport for this project.
     */
    @Override
    public JkIdeSupport getJavaIdeSupport() {
        JkQualifiedDependencySet qualifiedDependencies = JkQualifiedDependencySet.computeIdeDependencies(
                compilation.getDependencies(),
                packaging.getRuntimeDependencies(),
                testing.compilation.getDependencies(),
                JkCoordinate.ConflictStrategy.TAKE_FIRST);
        JkIdeSupport ideSupport = JkIdeSupport.of(baseDir)
            .setSourceVersion(jvmTargetVersion)
            .setProdLayout(compilation.layout)
            .setTestLayout(testing.compilation.layout)
            .setGeneratedSourceDirs(compilation.getGeneratedSourceDirs())
            .setDependencies(qualifiedDependencies)
            .setDependencyResolver(dependencyResolver);
        return ideSupportModifier.apply(ideSupport);
    }

    /**
     * Creates a dependency o the regular bin jar created by this project. <p/>
     * @see #toDependency(JkTransitivity)
     */
    public JkLocalProjectDependency toDependency() {
        return toDependency(JkTransitivity.RUNTIME);
    }

    /**
     * Creates a dependency o the regular bin jar of this project, with the specified transitivity. <p/>
     * If the project produces a fat jar, you may prefer to use {@link #toDependency(Runnable, Path, JkTransitivity)}
      * to have more control on the provided dependency artifact.
     * @see #toDependency(Runnable, Path, JkTransitivity)
     */
    public JkLocalProjectDependency toDependency(JkTransitivity transitivity) {
        return toDependency(packaging::createBinJar, artifactLocator.getMainArtifactPath(), transitivity);
    }

    /**
     * Creates a dependency on an artifact created by this project. The created dependency
     * is meant to be consumed by an external project.
     */
    public JkLocalProjectDependency toDependency(Runnable artifactMaker, Path artifactPath, JkTransitivity transitivity) {
       JkDependencySet exportedDependencies = compilation.getDependencies()
               .merge(packaging.getRuntimeDependencies()).getResult();
        return JkLocalProjectDependency.of(artifactMaker, artifactPath, this.baseDir, exportedDependencies)
                .withTransitivity(transitivity);
    }

    /**
     * Executes the pack action defined for this project.
     * @see JkProject#packActions
     */
    public void pack() {
        this.compilation.runIfNeeded();  // Better to launch it first explicitly for log clarity
        this.testing.runIfNeeded();
        this.packActions.run();
    }

    /**
     * Returns if the project dependencies include those mentioned in <i>jeka/project-dependencies.txt</i> flat file.
     */
    public boolean isIncludeTextAndLocalDependencies() {
        return includeTextAndLocalDependencies;
    }

    /**
     * Specifies if the project dependencies should include those mentioned in <i>jeka/project-dependencies.txt</i> flat file.
     * Values <code>true</code> by default.
     */
    public JkProject setIncludeTextAndLocalDependencies(boolean includeTextAndLocalDependencies) {
        this.includeTextAndLocalDependencies = includeTextAndLocalDependencies;
        return this;
    }

    /**
     * Returns an XML document containing the dependency trees of this project.
     * Mainly intended for 3rd party tools.
     */
    public Document getDependenciesAsXml()  {
        Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Element root = document.createElement("dependencies");
        document.appendChild(root);
        root.appendChild(xmlDeps(document, "compile", compilation.getDependencies()));
        root.appendChild(xmlDeps(document, "runtime", packaging.getRuntimeDependencies()));
        root.appendChild(xmlDeps(document, "test", testing.compilation.getDependencies()));
        return document;
    }

    /**
     * Returns the moduleId of this project. The moduleId is used to :
     * <ul>
     *     <li>Publish artifact to Maven/Ivy repository</li>
     *     <li>Populate Manifest</li>
     *     <li>Reference additional reports</li>
     *     <li>Infer produced artifact file name</li>
     *     <li>Name Docker images</li>
     *     <li>...</li>
     * </ul>
     */
    public JkModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Sets the moduleId for this project.
     */
    public JkProject setModuleId(JkModuleId moduleId) {
        this.moduleId = moduleId;
        return this;
    }

    public JkProject setModuleId(String moduleId) {
        return setModuleId(JkModuleId.of(moduleId));
    }

    /**
     * Returns the version of the projects. It returns version explicitly set
     * by {@link #setVersion(String)} if one has been set. Otherwise, it uses
     * the version returned by the version supplier.
     *
     * The version is used to :
     * <ul>
     *     <li>Publish artifact to Maven/Ivy repository</li>
     *     <li>Populate Manifest</li>
     *     <li>Reference additional reports</li>
     *     <li>Name Docker images</li>
     *     <li>...</li>
     * </ul>
     */
    public JkVersion getVersion() {
        if (version != null && !version.isUnspecified()) {
            return version;
        }
        return Optional.ofNullable(versionSupplier.get()).orElse(JkVersion.UNSPECIFIED);
    }

    /**
     * Sets the supplier for computing the project version. It can consist in returning an
     * hard-coded version or computing a version number from VCS.
     */
    public JkProject setVersionSupplier(Supplier<JkVersion> versionSupplier) {
        this.versionSupplier = versionSupplier;
        return this;
    }

    /**
     * Sets the explicit version of this project.
     */
    public JkProject setVersion(String version) {
        this.version = JkVersion.of(version);
        return this;
    }

    LocalAndTxtDependencies textAndLocalDeps() {
        if (cachedTextAndLocalDeps != null) {
            return cachedTextAndLocalDeps;
        }
        LocalAndTxtDependencies localDeps = LocalAndTxtDependencies.ofLocal(
                baseDir.resolve(PROJECT_LIBS_DIR));
        LocalAndTxtDependencies textDeps = dependencyTxtUrl == null
                ? LocalAndTxtDependencies.ofOptionalTextDescription(
                baseDir.resolve(DEPENDENCIES_TXT_FILE))
                : LocalAndTxtDependencies.ofTextDescription(dependencyTxtUrl);
        cachedTextAndLocalDeps = localDeps.and(textDeps);
        return cachedTextAndLocalDeps;
    }

    JkProject setDependencyTxtUrl(URL url) {
        this.dependencyTxtUrl = url;
        return this;
    }

    String relativeLocationLabel() {
        Path workingDir = Paths.get("").toAbsolutePath();
        Path baseDir = getBaseDir().toAbsolutePath().normalize();
        if (workingDir.equals(baseDir)) {
            return "";
        }
        return " [from " + workingDir.relativize(baseDir) + "]";
    }

    private Element xmlDeps(Document document, String purpose, JkDependencySet deps) {
        JkResolveResult resolveResult = dependencyResolver.resolve(deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        Element element = tree.toDomElement(document, true);
        element.setAttribute("purpose", purpose);
        return element;
    }

    private void displayDependencyTree(String purpose, JkDependencySet deps) {
        final JkResolveResult resolveResult = dependencyResolver.resolve(deps);
        final JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        JkLog.info("------------------------------------------------------------");
        JkLog.info("Dependency tree for " + purpose + " : ");
        JkLog.info("------------------------------------------------------------");
        JkLog.info(String.join("\n", tree.toStrings()));
        JkLog.info("");
        JkLog.info("----------------------------------");
        JkLog.info("Classpath for " + purpose + " : ");
        JkLog.info("----------------------------------");
        resolveResult.getFiles().getEntries().forEach(path -> JkLog.info(path.getFileName().toString()));
        JkLog.info("");
    }

    private JkArtifactLocator artifactLocator() {
        return JkArtifactLocator.of(
                () -> baseDir.resolve(outputDir),
                () -> moduleId != null ? moduleId.getDotNotation()
                        : baseDir.toAbsolutePath().getFileName().toString()
        );
    }

}