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

package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsJdk;
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
import java.util.*;
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
 * <p>
 * The following represents a tree of the JkProject API.
 * <pre><code>
 * project
 * +- baseDir
 * +- outputDir (generally values to ${baseDir}/jeka-output dir)
 * +- artifactLocator (define paths of created artifact files -jar files- )
 * |  +- methods: getArtifactPath(), getMainArtifactPath(), ....
 * +- duplicateDependencyConflictStrategy
 * +- jvmTargetVersion
 * +- sourceEncoding
 * +- javaCompileToolChain
 * +- dependencyResolver
 * +- compilation  (produce individual binary files from production sources. This includes resource processing, code generation, processing on .class files, ...)
 * |  +- layout (where are located sources, resources and compiled classes)
 * |  +- source generators (plugin mechanism for generating source files)
 * |  +- dependencies   (stands for compile dependencies)
 * |  +- preCompileActions (including resources processing)
 * |  +- compileActions (including java sources compilation. Compilation for other languages can be added here)
 * |  +- postCompileActions
 * |  +- methods : resolveDependencies(), run()
 * +- test
 * |  +- testCompilation (same as for 'prod' compilation but configured for tests purpose)
 * |  +- breakOnFailure (true/false)
 * |  +- skipped (true/false)
 * |  +- testProcessor
 * |  |  +- forkedProcess (configured the forked process who will run tests)
 * |  |  +- preActions
 * |  |  +- postActions
 * |  |  +- engineBehavior
 * |  |  |  +- testReportDir
 * |  |  |  +- progressDisplayer
 * |  |  |  +- launcherConfiguration (based on junit5 platform API)
 * |  |  +- testSelection
 * |  |  |  +- includePatterns
 * |  |  |  +- includeTags
 * |  +- method : run()
 * +- packaging (produces javadoc and source jar and bin jars)
 * |  +- javadocConfiguration
 * |  +- runtimeDependencies
 * |  +- manifest
 * |  +- fatJar (customize produced fat/uber jar if any)
 * |  +- methods : createJavadocJar(), createSourceJar(), createBinJar(), createFatJar(), resolveRuntimeDependencies()
 * +- e2eTest
 * |  +- methods : add(), run()
 * +- qualityCheck
 * |  +- methods : add(), run()
 * + methods :  toDependency(transitivity), getIdeSupport(), pack(), getDependenciesAsXml(), includeLocalAndTextDependencies()
 * </code></pre>
 */
public final class JkProject implements JkIdeSupportSupplier, JkBuildable.Supplier {

    /**
     * Flag to indicate if we need to include, or not, runtime dependencies in some scenario.
     */
    public enum RuntimeDeps {

        INCLUDE, EXCLUDE;

        public static RuntimeDeps of(boolean include) {
            return include ? INCLUDE : EXCLUDE;
        }
    }

    /**
     * @deprecated Use {@link JkProjectPackaging#CREATE_JAR_ACTION} instead
     */
    @Deprecated
    public static final String CREATE_JAR_ACTION = JkProjectPackaging.CREATE_JAR_ACTION;

    public static final String DEPENDENCIES_TXT_FILE = "dependencies.txt";

    public static final String PROJECT_LIBS_DIR = "libs";

    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Provides conventional path where artifact files are supposed to be generated.
     */
    public final JkArtifactLocator artifactLocator;

    /**
     * @deprecated Use {@link JkProjectPackaging#actions} instead;
     */
    @Deprecated
    public final JkRunnables packActions;

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
     * @deprecated Use #pack instead
     */
    @Deprecated
    public final JkProjectPackaging packaging;

    /**
     * Object responsible for creating binary, fat, javadoc and sources jars.
     * It also defines the runtime dependencies of this project.
     */
    public final JkProjectPackaging pack;

    /**
     * @deprecated Use #test instead
     */
    @Deprecated
    public final JkProjectTesting testing;

    /**
     * Object responsible for compiling and running tests.
     */
    public final JkProjectTesting test;

    /**
     * Object responsible for running end-to-end tests. It simply consists in a runnable container where users
     * can register e2e tests and run it a later phase.
     */
    public final JkProjectE2eTest e2eTest = new JkProjectE2eTest();

    /**
     * Object responsible for running quality checkers. It simply consists in a runnable container where users
     * can register quality-checkers and run it a later phase.
     */
    public final JkRunnableContainer qualityCheck = new JkRunnableContainer("check-quality");

    /**
     * Function to modify the {@link JkIdeSupport} used for configuring IDEs.
     */
    public Function<JkIdeSupport, JkIdeSupport> ideSupportModifier = x -> x;

    /**
     * Defines extra actions to execute when {@link JkProject#clean()} is invoked.
     */
    public final JkRunnables cleanExtraActions = JkRunnables.of();

    /**
     * Customizes the Java options for running the main method of the project.
     * This variable allows consumers to modify the JVM arguments or other settings
     * in a flexible and programmatic way during the execution of the main method.
     */
    public final JkConsumers<List<String>> runJavaOptionCustomizer = JkConsumers.of();

    public final JpmsModules jpmsModules = new JpmsModules();

    /**
     * Convenient facade for configuring the project.
     */
    public final JkProjectFlatFacade flatFacade;

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

    private final Function<Path, JkProject> projectResolver;

    private final Map<String, String> properties;



    private JkProject(Function<Path, JkProject> projectResolver, Map<String, String> properties) {
        artifactLocator = artifactLocator();
        compilerToolChain = JkJavaCompilerToolChain.of();
        compilation = JkProjectCompilation.ofProd(this);
        test = new JkProjectTesting(this);
        testing = test;
        pack = new JkProjectPackaging(this);
        packActions = pack.actions;
        packaging = pack;
        dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral())
                .setUseInMemoryCache(true);
        flatFacade = new JkProjectFlatFacade(this);
        this.projectResolver = projectResolver;
        this.properties = properties;
    }

    public static JkProject of(Function<Path, JkProject> projectPathMapper, Map<String, String> properties) {
        return new JkProject(projectPathMapper, properties);
    }
    /**
     * Creates a new project having the current working directory as base dir.
     */
    public static JkProject of() {
        return new JkProject(path -> null, JkProperties.ofSysPropsThenEnv().getAll());
    }

    /**
     * Applies the specified configuration consumer to this project.
     */
    public JkProject apply(Consumer<JkProject> projectConsumer) {
        projectConsumer.accept(this);
        return this;
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
     * @deprecated Use {@link JkProjectPackaging#setJarMaker(Consumer)} instead
     */
    @Deprecated
    public JkProject setJarMaker(Consumer<Path> jarMaker) {
        this.pack.setJarMaker(jarMaker);
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
        JkJavaProcess javaProcess;
        if (this.jvmTargetVersion != null && !JkJavaVersion.ofCurrent().equals(this.jvmTargetVersion)) {
            Path jdkPath = JkUtilsJdk.getJdk(compilerToolChain.getJavaDistrib(), jvmTargetVersion.toString());
            javaProcess = JkJavaProcess.ofJavaFromJdk(jdkPath, mainClass);
        } else {
            javaProcess = JkJavaProcess.ofJava(mainClass);
        }
        List<String> javaOptions = getRunJavaOptions();
        return javaProcess
                .addJavaOptions(javaOptions)
                .setClasspath(classpath)
                .setDestroyAtJvmShutdown(true)
                .setLogCommand(true)
                .setInheritIO(true);
    }

    /**
     * Creates {@link JkJavaProcess} to execute the jar having the specified artifact name.
     * The jar is created on the fly if it is not already present.

     * @param runtimeDepInclusion If <code>INCLUDE</code>, the runtime dependencies will be added to the classpath. This should
     *                           values <code>EXCLUDE</code> in case of <i>fat</i> jar.
     */
    public JkJavaProcess prepareRunJar(RuntimeDeps runtimeDepInclusion) {
        Path artifactPath = artifactLocator.getMainArtifactPath();
        if (!Files.exists(artifactPath)) {
            packActions.run();
        }
        JkJavaProcess javaProcess;
        if (this.jvmTargetVersion != null && !JkJavaVersion.ofCurrent().equals(this.jvmTargetVersion)) {
            Path jdkPath = JkUtilsJdk.getJdk(compilerToolChain.getJavaDistrib(), jvmTargetVersion.toString());
            javaProcess = JkJavaProcess.ofJavaJarFromJdk(jdkPath, artifactPath);
        } else {
            javaProcess = JkJavaProcess.ofJavaJar(artifactPath);
        }
        List<String> javaOptions = getRunJavaOptions();
        javaProcess
                .addJavaOptions(javaOptions)
                .setDestroyAtJvmShutdown(true)
                .setLogCommand(true)
                .setInheritIO(true);
        if (runtimeDepInclusion == RuntimeDeps.INCLUDE) {
            javaProcess.setClasspath(packaging.resolveRuntimeDependenciesAsFiles());
        }
        return javaProcess;
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
     * Executes the full build process by executing clean, test, pack and test end-to-end.
     */
    public void fullBuild() {
        clean();
        test.run();
        pack.run();
        e2eTest.run();
    }

    public void fullBuildTask() {
        JkLog.startTask("full-build: " + this.baseDir);
        fullBuild();
        JkLog.endTask();
    }

    /**
     * Returns a human-readable text that mentions various settings for this project
     * (source locations, file count, declared dependencies, ...).
     */
    public String getInfo() {
        JkDependencySet compileDependencies = compilation.dependencies.get();
        JkDependencySet runtimeDependencies = packaging.runtimeDependencies.get();
        JkDependencySet testDependencies = testing.compilation.dependencies.get();
        StringBuilder builder = new StringBuilder();

        String declaredMainClassName;
        String manifest;
        try {
            declaredMainClassName = packaging.getMainClass();
            manifest = packaging.getManifest().asTrimedString();
        } catch (IllegalStateException e) {
            declaredMainClassName = "Cannot compute cause: " + e.getMessage();
            manifest = declaredMainClassName;
        }

        Jk2ColumnsText columnsText = Jk2ColumnsText.of(30, 200).setAdjustLeft(true)
                .add("ModuleId", moduleId != null ? moduleId : "UNSPECIFIED")
                .add("Version", version != null ? version : "UNSPECIFIED (defaulted to " + getVersion() + ")")
                .add("Java Version", jvmTargetVersion == null ? "UNSPECIFIED (inherit from JeKa: " + JkJavaVersion.ofCurrent() + ")" : jvmTargetVersion  )
                .add("Fork Compilation", compilation.isCompilationForked())
                .add("Base Dir", this.getBaseDir().toAbsolutePath().normalize())
                .add("Main Class Name", Optional.ofNullable(declaredMainClassName).orElse("UNSPECIFIED"))
                .add("Source Encoding", sourceEncoding + "\n")
                .add("Production Sources", compilation.layout.getInfo())
                .add("Test Sources", testing.compilation.layout.getInfo())
                .add("Source File Count", compilation.layout.resolveSources()
                        .count(Integer.MAX_VALUE, false))
                .add("Test Source file count       ", testing.compilation.layout.resolveSources()
                        .count(Integer.MAX_VALUE, false))
                .add("Test Class Dirs", test.selection.getTestClassRoots())
                .add("Test Inclusions", test.selection.getIncludePatterns())
                .add("Test Exclusions", test.selection.getExcludePatterns())
                .add("Test Progress Style", test.processor.engineBehavior.getProgressStyle())
                .add("Jmps module path", jpmsModules.getModulePaths())
                .add("Jmps add modules", jpmsModules.getAddModules())
                .add("Download Repositories", dependencyResolver.getRepos().getRepos().stream()
                        .map(repo -> repo.getUrl()).collect(Collectors.toList()))
                .add("Pack actions", this.packActions.getRunnableNames())
                .add("Manifest Base", manifest); // manifest ad extra '' queuing char, this causes a extra empty line when displaying
        builder.append(columnsText.toString());

        if (JkLog.isVerbose()) { // add declared dependencies
            builder.append("\nCompile Dependencies          : \n");
            compileDependencies.getVersionedDependencies().forEach(dep -> builder.append("    " + dep + "\n"));
            builder.append("Runtime Dependencies          : \n");
            runtimeDependencies.getVersionedDependencies().forEach(dep -> builder.append("    " + dep + "\n"));
            builder.append("Test Dependencies             : \n");
            testDependencies.getVersionedDependencies().forEach(dep -> builder.append("    " + dep + "\n"));
        } else {
            builder.append("\nUse the --verbose option to display declared dependencies.");
        }
        return builder.toString();
    }

    /**
     * Displays the resolved dependency tree on the console.
     */
    public void displayDependencyTree() {
        displayDependencyTree("compile", compilation.dependencies.get());
        displayDependencyTree("runtime", packaging.runtimeDependencies.get());
        displayDependencyTree("test", testing.compilation.dependencies.get());
    }

    /**
     * Returns an ideSupport for this project.
     */
    @Override
    public JkIdeSupport getJavaIdeSupport() {
        JkQualifiedDependencySet qualifiedDependencies = JkQualifiedDependencySet.computeIdeDependencies(
                compilation.dependencies.get(),
                packaging.runtimeDependencies.get(),
                testing.compilation.dependencies.get(),
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
        return toDependency(packaging::createBinJar, artifactLocator.getMainArtifactPath().toAbsolutePath(), transitivity);
    }

    /**
     * Creates a dependency on an artifact created by this project. The created dependency
     * is meant to be consumed by an external project.
     */
    public JkLocalProjectDependency toDependency(Runnable artifactMaker, Path artifactPath, JkTransitivity transitivity) {
       JkDependencySet exportedDependencies = compilation.dependencies.get()
               .merge(packaging.runtimeDependencies.get()).getResult();
        return JkLocalProjectDependency.of(artifactMaker, artifactPath, this.baseDir, exportedDependencies)
                .withTransitivity(transitivity);
    }

    /**
     * @deprecated Use {@link JkProjectPackaging#run()} instead
     */
    @Deprecated
    public void pack() {
        this.pack.run();
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
        root.appendChild(xmlDeps(document, "compile", compilation.dependencies.get()));
        root.appendChild(xmlDeps(document, "runtime", packaging.runtimeDependencies.get()));
        root.appendChild(xmlDeps(document, "test", testing.compilation.dependencies.get()));
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

    public List<String> getRunJavaOptions() {
        List<String> javaOptions = new LinkedList<>();
        JkPathSequence modulePath = jpmsModules.getModulePaths();
        if (!modulePath.toList().isEmpty()) {
            javaOptions.add("--module-path");
            javaOptions.add(modulePath.toPath());
        }
        List<String> addModules = jpmsModules.getAddModules();
        if (!addModules.isEmpty()) {
            javaOptions.add("--add-modules");
            javaOptions.add(String.join(",", addModules));
        }
        runJavaOptionCustomizer.accept(javaOptions);
        return javaOptions;
    }

    /**
     *  Returns the Java version effectively used for compiling and running.
     */
    public String getEffectiveJavaVersion() {
        return Optional.ofNullable(this.jvmTargetVersion).orElse(JkJavaVersion.ofCurrent()).toString();
    }

    LocalAndTxtDependencies textAndLocalDeps() {
        if (cachedTextAndLocalDeps != null) {
            return cachedTextAndLocalDeps;
        }
        LocalAndTxtDependencies localDeps = LocalAndTxtDependencies.ofLocal(
                baseDir.resolve(PROJECT_LIBS_DIR));
        LocalAndTxtDependencies textDeps = dependencyTxtUrl == null
                ? LocalAndTxtDependencies.ofOptionalTextDescription(
                baseDir.resolve(DEPENDENCIES_TXT_FILE), baseDir, projectResolver, properties)
                : LocalAndTxtDependencies.ofTextDescription(dependencyTxtUrl, baseDir, projectResolver, properties);

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
        JkResolveResult resolveResult = dependencyResolver.resolve(purpose, deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        Element element = tree.toDomElement(document, true);
        element.setAttribute("purpose", purpose);
        return element;
    }

    private void displayDependencyTree(String purpose, JkDependencySet deps) {
        final JkResolveResult resolveResult = dependencyResolver.resolve(purpose, deps);
        final JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        JkLog.info("------------------------------------------------------------");
        JkLog.info("Dependency tree for " + purpose + " : ");
        JkLog.info("------------------------------------------------------------");
        JkLog.info(String.join("\n", tree.toStrings()));
        JkLog.info("");
        JkLog.info("");
        String msg = "Classpath for " + purpose + " : ";
        JkLog.info(msg);
        JkLog.info(JkUtilsString.repeat("-", msg.length()));
        resolveResult.getFiles().getEntries().forEach(path -> JkLog.info(path.getFileName().toString()));
        System.out.println();
    }

    private JkArtifactLocator artifactLocator() {
        return JkArtifactLocator.of(
                () -> baseDir.resolve(outputDir),
                () -> moduleId != null ? moduleId.getDotNotation()
                        : baseDir.toAbsolutePath().getFileName().toString()
        );
    }

    public JkBuildable asBuildable() {

        return new JkBuildable() {

            @Override
            public void compileIfNeeded() {
                compilation.runIfNeeded();
            }

            @Override
            public JkDependencyResolver getDependencyResolver() {
                return dependencyResolver;
            }

            @Override
            public Path getMainJarPath() {
                return artifactLocator.getMainArtifactPath();
            }

            @Override
            public Adapted getAdapted() {
                return Adapted.PROJECT;
            }

            @Override
            public boolean compile(JkJavaCompileSpec compileSpec) {
                return JkProject.this.compilerToolChain.compile(compileSpec) != JkJavaCompilerToolChain.Status.FAILED;
            }

            @Override
            public JkDependencySet getCompiledDependencies() {
                return JkProject.this.compilation.dependencies.get();
            }

            @Override
            public JkDependencySet getRuntimesDependencies() {
                return JkProject.this.compilation.dependencies.get();
            }

            @Override
            public JkCoordinate.ConflictStrategy getDependencyConflictStrategy() {
                return JkProject.this.getDuplicateConflictStrategy();
            }

            @Override
            public void createSourceJar(Path targetFile) {
                JkProject.this.packaging.createSourceJar(targetFile);
            }

            @Override
            public void createJavadocJar(Path targetFile) {
                JkProject.this.packaging.createJavadocJar(targetFile);
            }

            @Override
            public void setVersionSupplier(java.util.function.Supplier<JkVersion> versionSupplier) {
                JkProject.this.setVersionSupplier(versionSupplier);
            }

            @Override
            public JkConsumers<JkManifest> getManifestCustomizers() {
                return JkProject.this.packaging.manifestCustomizer;
            }

            @Override
            public JkJavaProcess prepareRunJar() {
                return JkProject.this.prepareRunJar(RuntimeDeps.EXCLUDE);
            }

            @Override
            public JkArtifactLocator getArtifactLocator() {
                return JkProject.this.artifactLocator;
            }

            @Override
            public Path getClassDir() {
                return compilation.layout.resolveClassDir();
            }

            @Override
            public JkResolveResult resolveRuntimeDependencies() {
                return packaging.resolveRuntimeDependencies();
            }

            @Override
            public List<Path> getRuntimeDependenciesAsFiles() {
                return packaging.resolveRuntimeDependenciesAsFiles();
            }

            @Override
            public JkVersion getVersion() {
                return JkProject.this.getVersion();
            }

            @Override
            public JkModuleId getModuleId() {
                return moduleId;
            }

            @Override
            public Path getOutputDir() {
                return JkProject.this.getOutputDir();
            }

            @Override
            public Path getBaseDir() {
                return JkProject.this.getBaseDir();
            }

            @Override
            public String getMainClass() {
                return packaging.getOrFindMainClass();
            }

            @Override
            public String toString() {
                return JkProject.this.toString();
            }
        };
    }

    public static class JkRunnableContainer {

        private final String containerName;

        private final JkRunnables runnables = JkRunnables.of().setLogTasks(true);

        public JkRunnableContainer(String containerName) {
            this.containerName = containerName;
        }

        public JkRunnableContainer add(String name, Runnable runnable) {
            runnables.append(name, runnable);
            return this;
        }

        /**
         * Executes all registered runners for this project in the order of their execution chain.
         * Each tester is represented by a {@link Runnable} and executed sequentially.
         */
        public void run() {
            JkLog.startTask(containerName);
            if (runnables.getSize() == 0) {
                JkLog.info("No %s processor registered.", containerName);
            } else if (runnables.getSize() == 1) {
                runnables.setLogTasks(false);
                runnables.getRunnable(0).run();
                runnables.setLogTasks(true);
            } else {
                runnables.run();
            }
            JkLog.endTask();
        }

    }

    public class JkProjectE2eTest extends JkRunnableContainer{

        public JkProjectE2eTest() {
            super("test-end-to-end");
        }

        /**
         * Configures the standard end-to-end testing setup for the project.
         * <p>
         * This method adjusts the test selection to exclude the predefined end-to-end test pattern (tests located in `e2e` root package).
         * and register them to be run with `e2eTest` method.
         * </p>
         */
        public void setupBasic() {
            test.selection.addExcludePatterns(JkTestSelection.E2E_PATTERN);
            this.add("", () -> {
                JkTestProcessor processor = createProcessor();
                processor
                        .runMatchingPatterns(JkTestSelection.E2E_PATTERN)
                        .assertSuccess();
            });
        }

        /**
         * Creates and configures a JkTestProcessor instance for running tests.
         * The processor is set to use the full progress display style.
         *
         * @return a configured instance of JkTestProcessor ready for test execution
         */
        public JkTestProcessor createProcessor() {
            JkTestProcessor processor = test.createDefaultProcessor();
            processor.engineBehavior.setProgressDisplayer(JkTestProcessor.JkProgressStyle.FULL);
            return processor;
        }

    }

    public final static class JpmsModules {

        private JpmsModules() {}

        public final JkConsumers<List<Path>> modulePathCustomizer = JkConsumers.of();

        public final JkConsumers<List<String>> addModulesCustomizer = JkConsumers.of();

        public JkPathSequence getModulePaths() {
            List<Path> paths = new LinkedList<>();
            modulePathCustomizer.accept(paths);
            return JkPathSequence.of(paths);
        }

        public List<String> getAddModules() {
            List<String> modules = new LinkedList<>();
            addModulesCustomizer.accept(modules);
            return modules;
        }
    }


}