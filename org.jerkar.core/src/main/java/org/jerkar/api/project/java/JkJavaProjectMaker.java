package org.jerkar.api.project.java;

import java.io.File;
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
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.file.JkZipper.JkCheckSumer;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;

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
                () -> JkUtilsFile.deleteDirContent(project.getOutLayout().outputDir()));
        this.resourceProcessor = JkRunnables.of(() -> JkResourceProcessor.of(project.getSourceLayout().resources())
                .and(project.getOutLayout().generatedResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().classDir()));
        this.compiler = JkRunnables.of(() -> {
            JkJavaCompiler comp = baseCompiler.andOptions(project.getCompileSpec().asOptions());
            comp = applyCompileSource(comp);
            comp.compile();
        });
        testResourceProcessor = JkRunnables.of(() -> JkResourceProcessor.of(project.getSourceLayout().testResources())
                .and(project.getOutLayout().generatedTestResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().testClassDir()));
        testCompiler = JkRunnables.of(() -> {
            JkJavaCompiler comp = testBaseCompiler.andOptions(this.project.getCompileSpec().asOptions());
            comp = applyTestCompileSource(comp);
            comp.compile();
        });
        artifactFileNameSupplier = () -> {
            if (project.getVersionedModule() != null) {
                return fileName(project.getVersionedModule());
            } else if (project.getArtifactName() != null) {
                return project.getArtifactName();
            }
            return project.baseDir().getName();
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
        .withClasspath(depsFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION))
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

    private JkJavaCompiler applyCompileSource(JkJavaCompiler baseCompiler) {
        final JkPathSequence classpath = depsFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION);
        return baseCompiler
                .withClasspath(classpath)
                .andSources(project.getSourceLayout().sources())
                .andSources(JkFileTree.of(project.getOutLayout().generatedSourceDir()).files(false))
                .withOutputDir(project.getOutLayout().classDir());
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

    private JkJavaCompiler applyTestCompileSource(JkJavaCompiler baseCompiler) {
        final JkPathSequence classpath = depsFor(JkJavaDepScopes.SCOPES_FOR_TEST).andHead(project.getOutLayout().classDir());
        return baseCompiler
                .withClasspath(classpath)
                .andSources(project.getSourceLayout().tests())
                .withOutputDir(project.getOutLayout().testClassDir());
    }

    protected JkUnit juniter() {
        final JkClasspath classpath = JkClasspath.of(project.getOutLayout().testClassDir(), project.getOutLayout().classDir())
                .and(depsFor(JkJavaDepScopes.SCOPES_FOR_TEST));
        final File junitReport = new File(project.getOutLayout().testReportDir(), "junit");
        return this.juniter.withClassesToTest(project.getOutLayout().testClassDir())
                .withClasspath(classpath)
                .withReportDir(junitReport);
    }

    public final JkRunnables testExecutor = JkRunnables.of(() -> juniter().run());

    public final JkRunnables afterTest = JkRunnables.of(() -> {
    });

    public JkJavaProjectMaker test() {
        JkLog.startln("Running unit tests");
        if (this.project.getSourceLayout().tests().countFiles(false) == 0) {
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

    public File makeArtifactFile(JkArtifactFileId artifactFileId) {
        if (artifactProducers.containsKey(artifactFileId)) {
            JkLog.startln("Producing artifact file " + getArtifactFile(artifactFileId).getName());
            this.artifactProducers.get(artifactFileId).run();
            JkLog.done();
            return getArtifactFile(artifactFileId);
        } else {
            throw new IllegalArgumentException("No artifact with classifier/extension " + artifactFileId + " is defined on project " + this.project);
        }
    }

    File getArtifactFile(JkArtifactFileId artifactId) {
        final String namePart = artifactFileNameSupplier.get();
        final String classifier = artifactId.classifier() == null ? "" : "-" + artifactId.classifier();
        final String extension = artifactId.extension() == null ? "" : "." + artifactId.extension();
        return new File(project.getOutLayout().outputDir(), namePart + classifier + extension);
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
        this.project.allArtifactFiles().forEach((file) -> JkCheckSumer.of(file).makeSumFiles(file, algorithms));
    }

    public void signArtifactFiles(JkPgp pgp) {
        this.project.allArtifactFiles().forEach((file) -> pgp.sign(file));
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
        JkPublisher.of(this.publishRepos, project.getOutLayout().outputDir())
        .publishMaven(project.getVersionedModule(), project, artifactFileIdsToNotPublish,
                this.getDefaultedDependencies(), project.getMavenPublicationInfo());
        return this;
    }

    public JkJavaProjectMaker publishIvy() {
        final JkDependencies dependencies = getDefaultedDependencies();
        final JkIvyPublication publication = JkIvyPublication.of(project.mainArtifactFile(), JkJavaDepScopes.COMPILE)
                .andOptional(project.artifactFile(JkJavaProject.SOURCES_FILE_ID), JkJavaDepScopes.SOURCES)
                .andOptional(project.artifactFile(JkJavaProject.JAVADOC_FILE_ID), JkJavaDepScopes.JAVADOC)
                .andOptional(project.artifactFile(JkJavaProject.TEST_FILE_ID), JkJavaDepScopes.TEST)
                .andOptional(project.artifactFile(JkJavaProject.TEST_SOURCE_FILE_ID), JkJavaDepScopes.SOURCES);
        final JkVersionProvider resolvedVersions = this.dependencyResolver
                .resolve(dependencies, dependencies.involvedScopes()).resolvedVersionProvider();
        JkPublisher.of(this.publishRepos, project.getOutLayout().outputDir())
        .publishIvy(project.getVersionedModule(), publication, dependencies,
                JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, Instant.now(), resolvedVersions);
        return this;
    }

    // ---------------------- from scratch

    public File makeBinJar() {
        compileAndTestIfNeeded();
        return packager.mainJar();
    }

    public File makeSourceJar() {
        if (!status.sourceGenerated) {
            this.sourceGenerator.run();
            status.sourceGenerated = true;
        }
        return packager.sourceJar();
    }

    public File makeJavadocJar() {
        if (!status.javadocGenerated) {
            generateJavadoc();
        }
        return packager.javadocJar();
    }

    public File makeTestJar() {
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
