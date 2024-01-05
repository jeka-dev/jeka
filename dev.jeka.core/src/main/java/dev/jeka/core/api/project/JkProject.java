package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
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
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.tooling.docker.JkDockerBuild;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Stands for the whole project model for building purpose. It has the same purpose and scope than the Maven <i>POM</i> but
 * it also holds methods to actually perform builds.<br/>
 * A complete overview is available <a href="https://jeka-dev.github.io/jeka/reference-guide/build-library-project-build/#project-api">here</a>.
 * <p/>
 *
 * A <code>JkProject</code> consists in 4 parts : <ul>
 *    <li>{@link JkProjectCompilation} : responsible to generate compile production sources</li>
 *    <li>{@link JkProjectTesting} : responsible to compile test sources and run tests </li>
 *    <li>{@link JkProjectPackaging} : responsible to creates javadoc, sources and jars</li>
 *    <li>{@link JkMavenPublication} : responsible to publish the artifacts on Maven repositories</li>
 * </ul>
 * <p>
 */
public class JkProject implements JkIdeSupportSupplier {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private Path baseDir = Paths.get("");

    private String outputDir = JkConstants.OUTPUT_PATH;

    private JkJavaVersion jvmTargetVersion;

    private String sourceEncoding = DEFAULT_ENCODING;

    private String mainClass;

    private JkModuleId moduleId;

    private Supplier<JkVersion> versionSupplier = () -> JkVersion.UNSPECIFIED;

    private final Runnable buildAction;

    private final JkRunnables cleanExtraActions = JkRunnables.of();

    public final JkStandardFileArtifactProducer artifactProducer;

    private JkCoordinate.ConflictStrategy duplicateConflictStrategy = JkCoordinate.ConflictStrategy.FAIL;

    private boolean includeTextAndLocalDependencies = true;

    private LocalAndTxtDependencies cachedTextAndLocalDeps;

    private URL dependencyTxtUrl;

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
     * Object responsible for publishing artifacts to a Maven repository.
     */
    public final JkMavenPublication mavenPublication;

    /**
     * Function to modify the {@link JkIdeSupport} used for configuring IDEs.
     */
    public Function<JkIdeSupport, JkIdeSupport> ideSupportModifier = x -> x;

    private JkProject() {
        compilerToolChain = JkJavaCompilerToolChain.of();
        compilation = JkProjectCompilation.ofProd(this);
        testing = new JkProjectTesting(this);
        packaging = new JkProjectPackaging(this);
        artifactProducer = artifactProducer();
        dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral()).setUseCache(true);
        mavenPublication = mavenPublication(this);
        buildAction = packaging::createBinJar;
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

    public JkRunnables getCleanExtraActions() {
        return cleanExtraActions;
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
     * Sets the main class name to use in #runXxx and {@link #dockerImage()} methods.
     */
    public JkProject setMainClass(String mainClass) {
        this.mainClass = mainClass;
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
     * Creates {@link JkProcess} to execute the main method for this project.
     */
    public JkProcess<?> runMain(String jvmOptions, String ... programArgs) {
        compilation.runIfNeeded();
        return JkJavaProcess.ofJava(actualMainClass())
                .setClasspath(this.packaging.resolveRuntimeDependencies().getFiles())
                .setInheritIO(true)
                .setInheritSystemProperties(true)
                .setDestroyAtJvmShutdown(true)
                .addJavaOptions(JkUtilsString.translateCommandline(jvmOptions))
                .addParams(programArgs);
    }

    /**
     * Creates {@link JkProcess} to execute the jar having the specified artifact name.
     * The jar is created on the fly if it is not already present.
     *
     * @param artifactQualifier  The name of the artifact to run. In a project producing a side 'fat' jar, you can use
     *                           'fat'. If you want to run the main artifact, just use ''.
     * @param includeRuntimeDeps If <code>true</code>, the runtime dependencies will be added to the classpath. This should
     *                           values <code>false</code> in case of <i>fat</i> jar.
     * @param jvmOptions         Options to be passed to the jvm, as <code>-Dxxx=1 -Dzzzz=abbc -Xmx=256m</code>.
     * @param args               Program arguments to be passed in command line, as <code>--print --verbose myArg</code>
     */
    public JkProcess<?> runJar(String artifactQualifier, boolean includeRuntimeDeps, String jvmOptions, String args) {
        JkArtifactId artifactId = JkArtifactId.of(artifactQualifier, "jar");
        Path artifactPath = artifactProducer.getArtifactPath(artifactId);
        if (!Files.exists(artifactPath)) {
            artifactProducer.makeArtifacts(artifactId);
        }
        JkJavaProcess javaProcess = JkJavaProcess.ofJavaJar(artifactPath)
                .setDestroyAtJvmShutdown(true)
                .setLogCommand(true)
                .setLogOutput(true)
                .addJavaOptions(JkUtilsString.translateCommandline(jvmOptions))
                .addParams(JkUtilsString.translateCommandline(args));
        if (includeRuntimeDeps) {
            JkPathSequence pathSequence = packaging.resolveRuntimeDependencies().getFiles();
            javaProcess.setClasspath(pathSequence.getEntries());
        }
        return javaProcess;
    }

    /**
     * Same as {@link #runJar(String, boolean, String, String)} but specific for the main artefact.
     *
     * @see #runJar(String, boolean, String, String)
     */
    public JkProcess<?> runJar(boolean includeRuntimeDeps, String jvmOptions, String programArgs) {
        return runJar(JkArtifactId.MAIN_ARTIFACT_CLASSIFIER, includeRuntimeDeps, jvmOptions, programArgs);
    }

    /**
     * Creates a {@link JkDockerBuild} to build a docker image of the application.
     */
    public JkDockerBuild dockerImage() {
        compilation.runIfNeeded();
        return JkDockerBuild.of()
                .setClasses(JkPathTree.of(compilation.layout.resolveClassDir()))
                .setClasspath(packaging.resolveRuntimeDependencies().getFiles().getEntries())
                .setMainClass(actualMainClass());
    }

    // -------------------------- Other -------------------------

    @Override
    public String toString() {
        return "project " + getBaseDir().getFileName();
    }

    /**
     * Cleans directories where are generated files output by the project builds
     * (jars, generated sources, classes, reports, ...)
     */
    public JkProject clean() {
        Path output = getOutputDir();
        JkLog.info("Clean output directory " + output.toAbsolutePath().normalize());
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
        StringBuilder builder = new StringBuilder("Project Location : " + this.getBaseDir().toAbsolutePath().normalize() + "\n")
            .append("Production sources : " + compilation.layout.getInfo()).append("\n")
            .append("Test sources : " + testing.compilation.layout.getInfo()).append("\n")
            .append("Java Source Version : " + (jvmTargetVersion == null ? "Unspecified" : jvmTargetVersion  )+ "\n")
            .append("Source Encoding : " + sourceEncoding + "\n")
            .append("Source file count : " + compilation.layout.resolveSources()
                    .count(Integer.MAX_VALUE, false) + "\n")
            .append("Download Repositories : " + dependencyResolver.getRepos() + "\n")
            .append("Declared Compile Dependencies : " + compileDependencies.getEntries().size() + " elements.\n");
        compileDependencies.getVersionedDependencies().forEach(dep -> builder.append("  " + dep + "\n"));
        builder.append("Declared Runtime Dependencies : " + runtimeDependencies
                .getEntries().size() + " elements.\n");
        runtimeDependencies.getVersionedDependencies().forEach(dep -> builder.append("  " + dep + "\n"));
        builder.append("Declared Test Dependencies : " + testDependencies.getEntries().size() + " elements.\n");
        testDependencies.getVersionedDependencies().forEach(dep -> builder.append("  " + dep + "\n"));
        if (mavenPublication.getModuleId() != null) {
            builder.append(mavenPublication.info());
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
     * Modifies the IdeSupport for this project.
     */
    public void setJavaIdeSupport(Function<JkIdeSupport, JkIdeSupport> ideSupport) {
        this.ideSupportModifier = ideSupportModifier.andThen(ideSupport);
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
        return toDependency(packaging::createBinJar, artifactProducer.getMainArtifactPath(), transitivity);
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
     * Shorthand to build all missing artifacts for publication.
     */
    public void pack() {
        artifactProducer.makeAllMissingArtifacts();
    }

    /**
     * Specifies if Javadoc and sources jars should be included in pack/publish. Default is true;
     */
    public JkProject includeJavadocAndSources(boolean includeJavaDoc, boolean includeSources) {
        if (includeJavaDoc) {
            artifactProducer.putArtifact(JkArtifactId.JAVADOC_ARTIFACT_ID, packaging::createJavadocJar);
        } else {
            artifactProducer.removeArtifact(JkArtifactId.JAVADOC_ARTIFACT_ID);
        }
        if (includeSources) {
            artifactProducer.putArtifact(JkArtifactId.SOURCES_ARTIFACT_ID, packaging::createSourceJar);
        } else {
            artifactProducer.removeArtifact(JkArtifactId.SOURCES_ARTIFACT_ID);
        }
        return this;
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
     * Convenient method to get the path of the main built artifact (generally the
     * jar file containing the java classes of the application/library).
     */
    public Path getMainArtifactPath() {
        return artifactProducer.getMainArtifactPath();
    }

    /**
     * Creates a Ivy publication from this project.
     */
    public JkIvyPublication createIvyPublication() {
        return JkIvyPublication.of()
                .setVersionSupplier(this::getVersion)
                .setModuleIdSupplier(this::getModuleId)
                .addArtifacts(() -> artifactProducer)
                .configureDependencies(deps -> JkIvyPublication.getPublishDependencies(
                        compilation.getDependencies(),
                        packaging.getRuntimeDependencies(),
                        getDuplicateConflictStrategy()));
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
     * Returns the version of the projects. The version is used to :
     * <ul>
     *     <li>Publish artifact to Maven/Ivy repository</li>
     *     <li>Populate Manifest</li>
     *     <li>Reference additional reports</li>
     *     <li>Name Docker images</li>
     *     <li>...</li>
     * </ul>
     */
    public JkVersion getVersion() {
        return versionSupplier.get();
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
     * Convenient method to set a static version on this project.
     */
    public JkProject setVersion(String version) {
        this.versionSupplier = () -> JkVersion.of(version);
        return this;
    }

    LocalAndTxtDependencies textAndLocalDeps() {
        if (cachedTextAndLocalDeps != null) {
            return cachedTextAndLocalDeps;
        }
        LocalAndTxtDependencies localDeps = LocalAndTxtDependencies.ofLocal(
                baseDir.resolve(JkConstants.JEKA_DIR).resolve(JkConstants.PROJECT_LIBS_DIR));
        LocalAndTxtDependencies textDeps = dependencyTxtUrl == null
                ? LocalAndTxtDependencies.ofOptionalTextDescription(
                baseDir.resolve(JkConstants.JEKA_DIR).resolve(JkConstants.PROJECT_DEPENDENCIES_TXT_FILE))
                : LocalAndTxtDependencies.ofTextDescription(dependencyTxtUrl);
        cachedTextAndLocalDeps = localDeps.and(textDeps);
        return cachedTextAndLocalDeps;
    }

    JkProject setDependencyTxtUrl(URL url) {
        this.dependencyTxtUrl = url;
        return this;
    }

    private Element xmlDeps(Document document, String purpose, JkDependencySet deps) {
        JkResolveResult resolveResult = dependencyResolver.resolve(deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        Element element = tree.toDomElement(document, true);
        element.setAttribute("purpose", purpose);
        return element;
    }

    private void displayDependencyTree(String purpose, JkDependencySet deps) {
        JkLog.info("-----------------------------------------------------------");
        JkLog.info("Resolving Dependencies for " + purpose + " : ");
        JkLog.info("-----------------------------------------------------------");
        final JkResolveResult resolveResult = dependencyResolver.resolve(deps);
        final JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        JkLog.info("-----------------------------------------");
        JkLog.info("Dependency tree for " + purpose + " : ");
        JkLog.info("-----------------------------------------");
        JkLog.info(String.join("\n", tree.toStrings()));
        JkLog.info("");
        JkLog.info("-----------------------------------------");
        JkLog.info("Classpath for " + purpose + " : ");
        JkLog.info("-----------------------------------------");
        resolveResult.getFiles().getEntries().forEach(path -> JkLog.info(path.getFileName().toString()));
        JkLog.info("");
    }

    private JkStandardFileArtifactProducer artifactProducer() {
        JkStandardFileArtifactProducer artifactProducer = JkStandardFileArtifactProducer.of(
                () -> baseDir.resolve(outputDir),
                () -> moduleId != null ? moduleId.getDotNotation()
                        : baseDir.toAbsolutePath().getFileName().toString()
        );
        artifactProducer.putMainArtifact(packaging::createBinJar);
        artifactProducer.putArtifact(JkArtifactId.SOURCES_ARTIFACT_ID, packaging::createSourceJar);
        artifactProducer.putArtifact(JkArtifactId.JAVADOC_ARTIFACT_ID, packaging::createJavadocJar);
        return artifactProducer;
    }

    private String actualMainClass() {
        if (mainClass != null) {
            return mainClass;
        }
        compilation.runIfNeeded();
        JkUrlClassLoader ucl = JkUrlClassLoader.of(this.compilation.layout.resolveClassDir());
        return ucl.toJkClassLoader().findUniqueMainClass();
    }

    private static JkMavenPublication mavenPublication(JkProject project) {
        return JkMavenPublication.of()
                .setModuleIdSupplier(project::getModuleId)
                .setVersionSupplier(project::getVersion)
                .setArtifactLocatorSupplier(() -> project.artifactProducer)
                .configureDependencies(deps -> JkMavenPublication.computeMavenPublishDependencies(
                        project.compilation.getDependencies(),
                        project.packaging.getRuntimeDependencies(),
                        project.getDuplicateConflictStrategy()))
                .setBomResolutionRepos(project.dependencyResolver::getRepos);
    }


}