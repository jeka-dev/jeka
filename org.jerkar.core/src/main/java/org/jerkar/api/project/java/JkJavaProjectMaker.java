package org.jerkar.api.project.java;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkIvyPublication;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkPublisher;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolutionParameters;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkVersionProvider;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.file.JkCheckSum;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkJavaTestSpec;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.system.JkLog;

/**
 * Beware : Experimental !!!!!!!!!!!!!!!!!!!!!!!
 * The API is likely to change subsequently.
 * <p>
 * Object responsible to build (to make) a java project. It provides methods to perform common build
 * task (compile, test, javadoc, package to jar).
 * All defined task are extensible so you can modify/improve the build behavior.
 */
public class JkJavaProjectMaker {

    private final JkJavaProject project;

    private JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.mavenCentral())
            .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaDepScopes.DEFAULT_SCOPE_MAPPING));

    private JkJavaCompiler baseCompiler = JkJavaCompiler.base();

    private JkJavaCompiler testBaseCompiler = JkJavaCompiler.base();

    private JkUnit juniter = JkUnit.of().withOutputOnConsole(false).withReport(JkUnit.JunitReportDetail.BASIC);

    private List<String> javadocOptions = new LinkedList<>();

    private JkPublishRepos publishRepos = JkPublishRepos.local();

    private boolean skipTests = false;

    private final Status status = new Status();

    private final Map<JkArtifactFileId, Runnable> artifactProducers = new LinkedHashMap<>();

    private final JkJavaProjectPackager packager;

    private final Set<JkArtifactFileId> artifactFileIdsToNotPublish = new LinkedHashSet<>();

    private Supplier<String> artifactFileNameSupplier;

    private JkPgp pgpSigner;

    private final Map<Set<JkScope>, JkPathSequence> depCache = new HashMap<>();

    // commons ------------------------

    JkJavaProjectMaker(JkJavaProject project) {
        this.project = project;
        this.packager = JkJavaProjectPackager.of(project);
        this.cleaner = JkRunnables.of(
                () -> JkPathTree.of(project.getOutLayout().outputPath()).deleteContent());
        this.resourceProcessor = JkRunnables.of(() -> JkResourceProcessor.of(project.getSourceLayout().resources())
                .and(project.getOutLayout().generatedResourceDir().toFile())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().classDir().toFile()));
        this.compiler = JkRunnables.of(() -> {
            JkJavaCompileSpec compileSpec = compileSourceSpec();
            baseCompiler.compile(compileSpec);
        });
        testResourceProcessor = JkRunnables.of(() -> JkResourceProcessor.of(project.getSourceLayout().testResources())
                .and(project.getOutLayout().generatedTestResourceDir().toFile())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().testClassDir().toFile()));
        testCompiler = JkRunnables.of(() -> {
            JkJavaCompileSpec testCompileSpec = testCompileSpec();
            testBaseCompiler.compile(testCompileSpec);
        });
        artifactFileNameSupplier = () -> {
            if (project.getVersionedModule() != null) {
                return fileName(project.getVersionedModule());
            } else if (project.getArtifactName() != null) {
                return project.getArtifactName();
            }
            return project.baseDir().getFileName().toString();
        };
    }


    protected void compileAndTestIfNeeded() {
        if (!status.compileDone) {
            compile();
        }
        if (!skipTests && !status.unitTestDone) {
            test();
        }
    }

    public void generateJavadoc() {
        JkJavadocMaker.of(project.getSourceLayout().sources(), project.getOutLayout().getJavadocDir())
                .withClasspathPath(depsFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION).entries())
                .andOptions(this.javadocOptions).process();
        status.javadocGenerated = true;
    }

    public JkPathSequence depsFor(JkScope... scopes) {
        final Set<JkScope> scopeSet = new HashSet<>(Arrays.asList(scopes));
        return this.depCache.computeIfAbsent(scopeSet,
                scopes1 -> this.dependencyResolver.get(getDefaultedDependencies(), scopes ));
    }

    public JkDependencies getDefaultedDependencies() {
        return project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME);
    }

    void cleanDepChache() {
        this.depCache.clear();
    }

    // Clean -----------------------------------------------

    public final JkRunnables cleaner;

    public JkJavaProjectMaker clean() {
        status.reset();
        cleaner.run();
        return this;
    }

    // Compile phase (produce binaries and process resources) -----------------------------

    public final JkRunnables beforeCompile = JkRunnables.noOp();

    public final JkRunnables sourceGenerator = JkRunnables.noOp();

    public final JkRunnables resourceGenerator = JkRunnables.noOp();

    public final JkRunnables resourceProcessor;

    public final JkRunnables compiler;

    private JkJavaCompileSpec compileSourceSpec() {
        JkJavaCompileSpec result = project.getCompileSpec().copy();
        final JkPathSequence classpath = depsFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION);
        return result
                .setClasspath(classpath)
                .addSources(project.getSourceLayout().sources().files())
                .addSources(JkPathTree.of(project.getOutLayout().generatedSourceDir()).files())
                .setOutputDir(project.getOutLayout().classDir());
    }

    public final JkRunnables afterCompile = JkRunnables.of(() -> {
    });

    public JkJavaProjectMaker compile() {
        JkLog.startln("Compiling");
        beforeCompile.run();
        sourceGenerator.run();
        resourceGenerator.run();
        compiler.run();
        resourceProcessor.run();
        afterCompile.run();
        this.status.compileDone = true;
        JkLog.done();
        return this;
    }

    // Test  -----------------------------------------------------

    public final JkRunnables beforeTest = JkRunnables.of(() -> {
    });

    public final JkRunnables testResourceGenerator = JkRunnables.of(() -> {
    });

    public final JkRunnables testResourceProcessor;

    public final JkRunnables testCompiler;

    private JkJavaCompileSpec testCompileSpec() {
        JkJavaCompileSpec result = project.getCompileSpec().copy();
        final JkPathSequence classpath = depsFor(JkJavaDepScopes.SCOPES_FOR_TEST).andFirst(project.getOutLayout().classDir());
        return result
                .setClasspath(classpath)
                .addSources(project.getSourceLayout().tests().files())
                .setOutputDir(project.getOutLayout().testClassDir());
    }

    private JkUnit juniter() {
        final JkClasspath classpath = JkClasspath.of(project.getOutLayout().testClassDir())
                .and(project.getOutLayout().classDir())
                .andMany(depsFor(JkJavaDepScopes.SCOPES_FOR_TEST));
        final Path junitReport = project.getOutLayout().testReportDir().resolve("junit");
        return this.juniter.withReportDir(junitReport);
    }

    private JkJavaTestSpec testSpec() {
        final JkClasspath classpath = JkClasspath.of(project.getOutLayout().testClassDir())
                .and(project.getOutLayout().classDir())
                .andMany(depsFor(JkJavaDepScopes.SCOPES_FOR_TEST));
        return JkJavaTestSpec.of(classpath, JkPathTreeSet.of(project.getOutLayout().testClassDir()));
    }

    public final JkRunnables testExecutor = JkRunnables.of(() -> juniter().run(testSpec()));

    public final JkRunnables afterTest = JkRunnables.of(() -> {
    });

    public JkJavaProjectMaker test() {
        JkLog.startln("Running unit tests");
        if (this.project.getSourceLayout().tests().count(0, false) == 0) {
            JkLog.info("No unit test found in : " + this.project.getSourceLayout().tests());
            JkLog.done();
            return this;
        }
        if (!this.status.compileDone) {
            compile();
        }
        beforeTest.run();
        testCompiler.run();
        testResourceGenerator.run();
        testResourceProcessor.run();
        testExecutor.run();
        afterTest.run();
        this.status.unitTestDone = true;
        JkLog.done();
        return this;
    }

    // Pack --------------------------------------------------------------------

    public final JkRunnables beforePackage = JkRunnables.of(() -> {
    });

    public Path makeArtifactFile(JkArtifactFileId artifactFileId) {
        if (artifactProducers.containsKey(artifactFileId)) {
            JkLog.startln("Producing artifact file " + getArtifactFile(artifactFileId).getFileName());
            this.artifactProducers.get(artifactFileId).run();
            JkLog.done();
            return getArtifactFile(artifactFileId);
        } else {
            throw new IllegalArgumentException("No artifact with classifier/extension " + artifactFileId + " is defined on project " + this.project);
        }
    }

    Path getArtifactFile(JkArtifactFileId artifactId) {
        final String namePart = artifactFileNameSupplier.get();
        final String classifier = artifactId.classifier() == null ? "" : "-" + artifactId.classifier();
        final String extension = artifactId.extension() == null ? "" : "." + artifactId.extension();
        return project.getOutLayout().outputPath().resolve(namePart + classifier + extension);
    }

    protected String fileName(JkVersionedModule versionedModule) {
        return versionedModule.moduleId().fullName() + "-" + versionedModule.version().name();
    }

    Iterable<JkArtifactFileId> getArtifactFileIds() {
        return this.artifactProducers.keySet();
    }

    JkJavaProjectMaker addArtifactFile(JkArtifactFileId artifactFileId, Runnable runnable) {
        this.artifactProducers.put(artifactFileId, runnable);
        return this;
    }

    JkJavaProjectMaker removeArtifactFile(JkArtifactFileId artifactFileId) {
        this.artifactProducers.remove(artifactFileId);
        return this;
    }

    boolean contains(JkArtifactFileId artifactFileId) {
        return this.artifactProducers.containsKey(artifactFileId);
    }

    public final JkRunnables afterPackage = JkRunnables.of(() -> {
    });

    public JkJavaProjectMaker pack() {
        beforePackage.run();
        this.compileAndTestIfNeeded();
        project.makeAllArtifactFiles();
        afterPackage.run();
        status.packagingDone = true;
        return this;
    }

    public void checksum(String ...algorithms) {
        this.project.allArtifactPaths().forEach((file) -> JkCheckSum.of(file).produce(algorithms));
    }

    public void signArtifactFiles(JkPgp pgp) {
        this.project.allArtifactPaths().forEach((file) -> pgp.sign(file));
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
        JkPublisher.of(this.publishRepos, project.getOutLayout().outputPath())
        .publishMaven(project.getVersionedModule(), project, artifactFileIdsToNotPublish,
                this.getDefaultedDependencies(), project.getMavenPublicationInfo());
        return this;
    }

    public JkJavaProjectMaker publishIvy() {
        final JkDependencies dependencies = getDefaultedDependencies();
        final JkIvyPublication publication = JkIvyPublication.of(project.mainArtifactPath(), JkJavaDepScopes.COMPILE)
                .andOptional(project.artifactPath(JkJavaProject.SOURCES_FILE_ID), JkJavaDepScopes.SOURCES)
                .andOptional(project.artifactPath(JkJavaProject.JAVADOC_FILE_ID), JkJavaDepScopes.JAVADOC)
                .andOptional(project.artifactPath(JkJavaProject.TEST_FILE_ID), JkJavaDepScopes.TEST)
                .andOptional(project.artifactPath(JkJavaProject.TEST_SOURCE_FILE_ID), JkJavaDepScopes.SOURCES);
        final JkVersionProvider resolvedVersions = this.dependencyResolver
                .resolve(dependencies, dependencies.involvedScopes()).resolvedVersionProvider();
        JkPublisher.of(this.publishRepos, project.getOutLayout().outputPath())
        .publishIvy(project.getVersionedModule(), publication, dependencies,
                JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, Instant.now(), resolvedVersions);
        return this;
    }

    // ---------------------- from scratch

    public Path makeBinJar() {
        compileAndTestIfNeeded();
        return packager.mainJar();
    }

    public Path makeSourceJar() {
        if (!status.sourceGenerated) {
            this.sourceGenerator.run();
            status.sourceGenerated = true;
        }
        return packager.sourceJar();
    }

    public Path makeJavadocJar() {
        if (!status.javadocGenerated) {
            generateJavadoc();
        }
        return packager.javadocJar();
    }

    public Path makeTestJar() {
        compileAndTestIfNeeded();
        if (!status.unitTestDone) {
            test();
        }
        return packager.testJar();
    }


    // -------------------- getters/setters -------------------------------------------------------


    public JkJavaProjectPackager getPackager() {
        return packager;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public JkJavaProjectMaker setDependencyResolver(JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        return this;
    }

    public JkJavaProjectMaker setDownloadRepos(JkRepos repos) {
        this.dependencyResolver = this.dependencyResolver.withRepos(repos);
        return this;
    }

    public JkJavaCompiler getBaseCompiler() {
        return baseCompiler;
    }

    public JkJavaProjectMaker setBaseCompiler(JkJavaCompiler baseCompiler) {
        this.baseCompiler = baseCompiler;
        return this;
    }

    public JkJavaCompiler getTestBaseCompiler() {
        return testBaseCompiler;
    }

    public JkJavaProjectMaker setTestBaseCompiler(JkJavaCompiler testBaseCompiler) {
        this.testBaseCompiler = testBaseCompiler;
        return this;
    }

    public JkUnit getJuniter() {
        return juniter;
    }

    public JkJavaProjectMaker setJuniter(JkUnit juniter) {
        this.juniter = juniter;
        return this;
    }

    public JkPublishRepos getPublishRepos() {
        return this.publishRepos;
    }

    public JkJavaProjectMaker setPublishRepos(JkPublishRepos publishRepos) {
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

    public Set<JkArtifactFileId> getArtifactFileIdsToNotPublish() {
        return artifactFileIdsToNotPublish;
    }

    public boolean isTestSkipped() {
        return skipTests;
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    private static class Status {
        private boolean sourceGenerated = false;
        private boolean compileDone = false;
        private boolean unitTestDone = false;
        private boolean packagingDone = false;
        private boolean javadocGenerated = false;

        void reset() {
            sourceGenerated = false;
            compileDone = false;
            unitTestDone = false;
            packagingDone = false;
            javadocGenerated = false;
        }

    }


}
