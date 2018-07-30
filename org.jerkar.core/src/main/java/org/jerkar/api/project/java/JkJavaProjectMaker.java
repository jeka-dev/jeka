package org.jerkar.api.project.java;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.*;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkJavaTestSpec;
import org.jerkar.api.java.junit.JkTestSuiteResult;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.project.JkProjectOutLayout;
import org.jerkar.api.system.JkLog;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Objects responsible to build (to make) a java project. It provides methods to perform common build
 * tasks (compile, test, javadoc, package jars, publish artifacts) along methods to define how to build extra artifacts.
 *
 * All defined tasks are extensible using {@link JkRunnables} mechanism.
 */
public final class JkJavaProjectMaker implements JkArtifactProducer, JkFileSystemLocalizable {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    public static final JkArtifactId TEST_ARTIFACT_ID = JkArtifactId.of("test", "jar");

    public static final JkArtifactId TEST_SOURCE_ARTIFACT_ID = JkArtifactId.of("test-sources", "jar");

    private final JkJavaProject project;

    private JkDependencyResolver dependencyResolver;

    private JkJavaCompiler compiler = JkJavaCompiler.of();

    private JkJavaCompiler testCompiler = JkJavaCompiler.of();

    private Function<JkJavaTestSpec, JkTestSuiteResult> tester;

    private JkPathMatcher testClassMatcher = JkPathMatcher.accept("**/*Test.class");

    private List<String> javadocOptions = new LinkedList<>();

    private JkRepoSet publishRepos = JkRepoSet.local();

    private boolean skipTests = false;

    private final Status status = new Status();

    private final Map<JkArtifactId, Runnable> artifactProducers = new LinkedHashMap<>();

    private final JkJavaProjectPackager packager;

    private final Set<JkArtifactId> artifactFileIdsToNotPublish = new LinkedHashSet<>();

    private Supplier<String> artifactFileNameSupplier;

    private final Map<Set<JkScope>, JkPathSequence> depCache = new HashMap<>();

    private JkProjectOutLayout outLayout;

    // commons ------------------------

    public JkJavaProjectMaker(JkJavaProject project) {
        this.project = project;
        this.outLayout = JkProjectOutLayout.classicJava().withOutputDir(project.baseDir().resolve("build/output"));
        this.packager = JkJavaProjectPackager.of(project);
        this.cleaner = JkRunnables.of(
                () -> JkPathTree.of(getOutLayout().outputPath()).deleteContent());
        Charset charset = project.getCompileSpec().getEncoding() == null ? Charset.defaultCharset() :
                Charset.forName(project.getCompileSpec().getEncoding());
        this.resourceProcessor = JkRunnables.of(() -> JkResourceProcessor.of(project.getSourceLayout().resources())
                .and(getOutLayout().generatedResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(getOutLayout().classDir(), charset));
        this.compileRunner = JkRunnables.of(() -> {
            JkJavaCompileSpec compileSpec = compileSourceSpec();
            compiler.compile(compileSpec);
        });
        testResourceProcessor = JkRunnables.of(() -> JkResourceProcessor.of(project.getSourceLayout().testResources())
                .and(getOutLayout().generatedTestResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(getOutLayout().testClassDir(), charset));
        testCompileRunner = JkRunnables.of(() -> {
            JkJavaCompileSpec testCompileSpec = getTestCompileSpec();
            testCompiler.compile(testCompileSpec);
        });
        tester = getDefaultTester();
        artifactFileNameSupplier = () -> {
            if (project.getVersionedModule() != null) {
                return fileName(project.getVersionedModule());
            }
            return project.baseDir().getFileName().toString();
        };

        // defines artifacts
        this.defineArtifact(mainArtifactId(), () -> makeBinJar(getArtifactFile(mainArtifactId())));
        this.defineArtifact(SOURCES_ARTIFACT_ID, () -> makeSourceJar(getArtifactFile(SOURCES_ARTIFACT_ID)));
        this.defineArtifact(JAVADOC_ARTIFACT_ID, () -> makeJavadocJar(getArtifactFile(JAVADOC_ARTIFACT_ID)));
        this.defineArtifact(TEST_ARTIFACT_ID, () -> makeTestJar(getArtifactFile(TEST_ARTIFACT_ID)));
        this.defineArtifact(TEST_SOURCE_ARTIFACT_ID,
                () -> getPackager().testSourceJar(getArtifactFile(TEST_SOURCE_ARTIFACT_ID)));
    }

    private JkDependencyResolver dependencyResolver() {
        if (dependencyResolver == null) {
            dependencyResolver = JkDependencyResolver.of(JkRepo.mavenCentral())
                  .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaDepScopes.DEFAULT_SCOPE_MAPPING));
        }
        return dependencyResolver;
    }

    private void compileAndTestIfNeeded() {
        if (!status.compileDone) {
            compile();
        }
        if (!skipTests && !status.unitTestDone) {
            test();
        }
    }

    /**
     * Generates javadoc files (files + zip)
     */
    public void generateJavadoc() {
        JkJavadocMaker.of(project.getSourceLayout().sources(), getOutLayout().getJavadocDir())
                .withClasspath(getDependenciesFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION))
                .andOptions(this.javadocOptions).process();
        status.javadocGenerated = true;
    }

    /**
     * Returns lib paths standing for the resolution of this project dependencies for the specified dependency scopes.
     */
    public JkPathSequence getDependenciesFor(JkScope... scopes) {
        final Set<JkScope> scopeSet = new HashSet<>(Arrays.asList(scopes));
        return this.depCache.computeIfAbsent(scopeSet,
                scopes1 -> this.dependencyResolver().get(getDeclaredDependencies(), scopes));
    }

    /**
     * Returns dependencies declared for this project. Dependencies declared without specifying
     * scope are defaulted to scope {@link JkJavaDepScopes#COMPILE_AND_RUNTIME}
     */
    public JkDependencySet getDeclaredDependencies() {
        return project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME);
    }

    void cleanDepChache() {
        this.depCache.clear();
    }

    // Clean -----------------------------------------------

    /**
     * Holds runnables executed while {@link #clean()} method is invoked. Add your own runnable if you want to
     * improve the <code>clean</code> method.
     */
    public final JkRunnables cleaner;

    /**
     * Deletes project build outputs.
     */
    public JkJavaProjectMaker clean() {
        status.reset();
        cleaner.run();
        return this;
    }

    // Compile phase (produce binaries and process resources) -----------------------------

    public final JkRunnables preCompile = JkRunnables.noOp();

    public final JkRunnables sourceGenerator = JkRunnables.noOp();

    public final JkRunnables resourceGenerator = JkRunnables.noOp();

    public final JkRunnables resourceProcessor;

    public final JkRunnables compileRunner;

    private JkJavaCompileSpec compileSourceSpec() {
        JkJavaCompileSpec result = project.getCompileSpec().copy();
        final JkPathSequence classpath = getDependenciesFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION);
        return result
                .setClasspath(classpath)
                .addSources(project.getSourceLayout().sources())
                .addSources(getOutLayout().generatedSourceDir())
                .setOutputDir(getOutLayout().classDir());
    }

    public final JkRunnables postCompile = JkRunnables.noOp();

    public JkJavaProjectMaker compile() {
        JkLog.execute( "Compilation and resource processing", () -> {
            preCompile.run();
            sourceGenerator.run();
            resourceGenerator.run();
            compileRunner.run();
            resourceProcessor.run();
            postCompile.run();
            this.status.compileDone = true;
        });
        return this;
    }

    // Test  -----------------------------------------------------

    public final JkRunnables preTest = JkRunnables.of(() -> {});

    public final JkRunnables testResourceGenerator = JkRunnables.of(() -> {});

    public final JkRunnables testResourceProcessor;

    public final JkRunnables testCompileRunner;

    private JkJavaCompileSpec getTestCompileSpec() {
        JkJavaCompileSpec result = project.getCompileSpec().copy();
        final JkPathSequence classpath = getDependenciesFor(JkJavaDepScopes.SCOPES_FOR_TEST).andFirst(getOutLayout().classDir());
        return result
                .setClasspath(classpath)
                .addSources(project.getSourceLayout().tests())
                .setOutputDir(getOutLayout().testClassDir());
    }

    public final JkUnit getDefaultTester() {
        final Path junitReport = getOutLayout().testReportDir().resolve("junit");
        return JkUnit.of().withOutputOnConsole(false).withReport(JkUnit.JunitReportDetail.BASIC)
                .withReportDir(junitReport);
    }

    public JkJavaTestSpec getTestSpec() {
        return JkJavaTestSpec.of(getTestClasspath(),
                JkPathTreeSet.of(getOutLayout().testClassDir()).andMatcher(testClassMatcher));
    }

    public JkClasspath getTestClasspath() {
        return JkClasspath.of(getOutLayout().testClassDir())
                .and(getOutLayout().classDir())
                .andMany(getDependenciesFor(JkJavaDepScopes.SCOPES_FOR_TEST));
    }

    public final JkRunnables testExecutor = JkRunnables.of(() -> tester.apply(getTestSpec()));

    public final JkRunnables postTest = JkRunnables.of(() -> {
    });

    public JkJavaProjectMaker test() {
        JkLog.execute("Running unit tests", () -> {
            if (this.project.getSourceLayout().tests().count(0, false) == 0) {
                JkLog.info("No unit test found in : "
                        + this.project.getSourceLayout().tests());
                return;
            }
            if (!this.status.compileOutputPresent()) {
                compile();
            }
            preTest.run();
            testCompileRunner.run();
            testResourceGenerator.run();
            testResourceProcessor.run();
            testExecutor.run();
            postTest.run();
            this.status.unitTestDone = true;
        });
        return this;
    }

    // Pack --------------------------------------------------------------------


    Path getArtifactFile(JkArtifactId artifactId) {
        final String namePart = artifactFileNameSupplier.get();
        final String classifier = artifactId.classifier() == null ? "" : "-" + artifactId.classifier();
        final String extension = artifactId.extension() == null ? "" : "." + artifactId.extension();
        return getOutLayout().outputPath().resolve(namePart + classifier + extension);
    }

    String fileName(JkVersionedModule versionedModule) {
        String version = versionedModule.version().isUnspecified() ? "" : "-" + versionedModule.version().value();
        return versionedModule.moduleId().fullName() + version;
    }

    /**
     * Defines how to produce the specified artifact. <br/>
     * The specified artifact can be an already defined artifact (as 'main' artifact), in this
     * case the current definition will be overwritten. <br/>
     * The specified artifact can also be a new artifact (as an Uber jar for example). <br/>
     * {@link JkJavaProjectMaker} declares predefined artifact ids as {@link JkJavaProjectMaker#SOURCES_ARTIFACT_ID}
     * or {@link JkJavaProjectMaker#JAVADOC_ARTIFACT_ID}.
     */
    public JkJavaProjectMaker defineArtifact(JkArtifactId artifactId, Runnable runnable) {
        this.artifactProducers.put(artifactId, runnable);
        return this;
    }

    /**
     * Removes the definition of the specified artifacts. Once remove, invoking <code>makeArtifact(theRemovedArtifactId)</code>
     * will raise an exception.
     */
    public JkJavaProjectMaker undefineArtifact(JkArtifactId artifactId) {
        this.artifactProducers.remove(artifactId);
        return this;
    }

    /**
     * Chain of actions that will be executed at the end of {@link #pack(Iterable)} method.
     */
    public final JkRunnables postPack = JkRunnables.of(() -> {
    });


    /**
     * Invokes {@link #pack(Iterable)} for all artifacts defined in this maker.
     */
    public JkJavaProjectMaker packAllDefinedArtifacts() {
       return pack(artifactIds());
    }

    /**
     * Makes the missing specified artifacts (it won't re-generate already existing artifacts) and
     * executes the {@link #postPack} actions.
     */
    public JkJavaProjectMaker pack(Iterable<JkArtifactId> artifactIds) {
        makeArtifactsIfAbsent(artifactIds);
        postPack.run();
        return this;
    }

    /**
     * Creates a checksum file of each specified algorithm and each existing defined artifact file.
     * Checksum files will be created in same folder as their respecting artifact files with the same name suffixed
     * by '.' and the name of the checksumm algorithm. <br/>
     * Known working algorithm working on JDK8 platform includes <code>md5, sha-1, sha-2 and sha-256</code>.
     */
    public void checksum(String ...algorithms) {
        this.allArtifactPaths().stream().filter(Files::exists)
                .forEach((file) -> JkPathFile.of(file).checksum(algorithms));
    }

    /**
     * Signs each existing defined artifact files with the specified pgp signer.
     */
    public void signArtifactFiles(JkPgp pgp) {
        this.allArtifactPaths().stream().filter(Files::exists).forEach((file) -> pgp.sign(file));
    }

    // ----------------------- publish

    public JkJavaProjectMaker publish() {
        final JkPublisher publisher = JkPublisher.of(this.publishRepos);
        if (publisher.hasMavenPublishRepo()) {
            publishMaven();
        }
        if (publisher.hasIvyPublishRepo()) {
            publishIvy();
        }
        return this;
    }

    public JkJavaProjectMaker publishMaven() {
        JkPublisher.of(this.publishRepos, getOutLayout().outputPath())
        .publishMaven(project.getVersionedModule(), this, artifactFileIdsToNotPublish,
                this.getDeclaredDependencies(), project.getMavenPublicationInfo());
        return this;
    }

    public JkJavaProjectMaker publishIvy() {
        final JkDependencySet dependencies = getDeclaredDependencies();
        final JkIvyPublication publication = JkIvyPublication.of(mainArtifactPath(), JkJavaDepScopes.COMPILE)
                .andOptional(artifactPath(SOURCES_ARTIFACT_ID), JkJavaDepScopes.SOURCES)
                .andOptional(artifactPath(JAVADOC_ARTIFACT_ID), JkJavaDepScopes.JAVADOC)
                .andOptional(artifactPath(TEST_ARTIFACT_ID), JkJavaDepScopes.TEST)
                .andOptional(artifactPath(TEST_SOURCE_ARTIFACT_ID), JkJavaDepScopes.SOURCES);
        final JkVersionProvider resolvedVersions = this.dependencyResolver()
                .resolve(dependencies, dependencies.involvedScopes()).resolvedVersionProvider();
        JkPublisher.of(this.publishRepos, getOutLayout().outputPath())
        .publishIvy(project.getVersionedModule(), publication, dependencies,
                JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, Instant.now(), resolvedVersions);
        return this;
    }

    // ---------------------- from scratch

    public void makeBinJar(Path target) {
        compileAndTestIfNeeded();
        packager.mainJar(target);
    }


    public void makeSourceJar(Path target) {
        if (!status.sourceGenerated) {
            this.sourceGenerator.run();
            status.sourceGenerated = true;
        }
        packager.sourceJar(target);
    }

    public void makeJavadocJar(Path target) {
        if (!status.javadocGenerated) {
            generateJavadoc();
        }
        packager.javadocJar(target);
    }

    public void makeTestJar(Path target) {
        compileAndTestIfNeeded();
        if (!status.unitTestDone) {
            test();
        }
        packager.testJar(target);
    }

    public void makeTestJar() {
        makeTestJar(artifactPath(TEST_ARTIFACT_ID));
    }


    // -------------------- getters/setters -------------------------------------------------------


    public JkJavaProjectPackager getPackager() {
        return packager;
    }

    public JkProjectOutLayout getOutLayout() {
        return outLayout;
    }

    public JkJavaProjectMaker setOutLayout(JkProjectOutLayout outLayout) {
        if (outLayout.outputPath().isAbsolute()) {
            this.outLayout = outLayout;
        } else {
            this.outLayout = outLayout.withOutputDir(this.project.baseDir().resolve(outLayout.outputPath()));
        }
        return this;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver();
    }

    public JkJavaProjectMaker setDependencyResolver(JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        return this;
    }

    public JkJavaProjectMaker setDownloadRepos(JkRepoSet repos) {
        this.dependencyResolver = this.dependencyResolver().withRepos(repos);
        return this;
    }

    public JkJavaCompiler getCompiler() {
        return compiler;
    }

    public JkJavaProjectMaker setCompiler(JkJavaCompiler compiler) {
        this.compiler = compiler;
        return this;
    }

    public JkJavaCompiler getTestCompiler() {
        return testCompiler;
    }

    public JkJavaProjectMaker setTestCompiler(JkJavaCompiler testCompiler) {
        this.testCompiler = testCompiler;
        return this;
    }

    public Function<JkJavaTestSpec, JkTestSuiteResult> getTester() {
        return tester;
    }

    public JkJavaProjectMaker setTester(JkUnit tester) {
        this.tester = tester;
        return this;
    }

    public JkRepoSet getPublishRepos() {
        return this.publishRepos;
    }

    public JkJavaProjectMaker setPublishRepos(JkRepoSet publishRepos) {
        this.publishRepos = publishRepos;
        return this;
    }
    public List<String> getJavadocOptions() {
        return this.javadocOptions;
    }

    public JkJavaProjectMaker setJavadocOptions(List<String> options) {
        this.javadocOptions = options;
        return this;
    }

    public Supplier<String> getArtifactFileNameSupplier() {
        return artifactFileNameSupplier;
    }

    public JkJavaProjectMaker setArtifactFileNameSupplier(Supplier<String> artifactFileNameSupplier) {
        this.artifactFileNameSupplier = artifactFileNameSupplier;
        return this;
    }

    public Set<JkArtifactId> getArtifactFileIdsToNotPublish() {
        return artifactFileIdsToNotPublish;
    }

    public boolean isTestSkipped() {
        return skipTests;
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    public JkPathMatcher getTestClassMatcher() {
        return testClassMatcher;
    }

    public void setTestClassMatcher(JkPathMatcher testClassMatcher) {
        this.testClassMatcher = testClassMatcher;
    }
    // ----------- artifact management --------------------------------------

    public void makeArtifact(JkArtifactId artifactId) {
        if (artifactProducers.containsKey(artifactId)) {
            JkLog.execute("Producing artifact file " + project.baseDir().relativize(getArtifactFile(artifactId)),
                    this.artifactProducers.get(artifactId));
        } else {
            throw new IllegalArgumentException("No artifact " + artifactId + " is defined on project " + this.project);
        }
    }

    /**
     * Convenient method for defining a fat jar artifact having the specified classifier name.
     */
    public JkJavaProjectMaker defineFatJarArtifact(String classifier) {
        this.defineArtifact(JkArtifactId.of(classifier, "jar"),
                () -> {compileAndTestIfNeeded(); getPackager().fatJar(classifier);});
        return this;
    }

    // artifact producers -----------------------------------------------------------


    @Override
    public Path artifactPath(JkArtifactId artifactId) {
        return getArtifactFile(artifactId);
    }

    @Override
    public final Iterable<JkArtifactId> artifactIds() {
        return this.artifactProducers.keySet();
    }

    @Override
    public JkPathSequence runtimeDependencies(JkArtifactId artifactFileId) {
        if (artifactFileId.equals(mainArtifactId())) {
            return this.getDependencyResolver().get(
                    this.project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.RUNTIME);
        } else if (artifactFileId.isClassifier("test") && artifactFileId.isExtension("jar")) {
            return this.getDependencyResolver().get(
                    this.project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME), JkJavaDepScopes.SCOPES_FOR_TEST);
        } else {
            return JkPathSequence.of();
        }
    }

    @Override
    public Path baseDir() {
        return this.project.baseDir();
    }

    private class Status {
        private boolean sourceGenerated = false;

        private boolean compileDone = false;

        private boolean unitTestDone = false;

        private boolean javadocGenerated = false;

        void reset() {
            sourceGenerated = false;
            compileDone = false;
            unitTestDone = false;
            javadocGenerated = false;
        }

        boolean compileOutputPresent() {
            return Files.exists(JkJavaProjectMaker.this.getOutLayout().classDir());
        }

    }

    @Override
    public String toString() {
        return this.project.toString();
    }
}
