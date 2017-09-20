package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.java.*;
import org.jerkar.api.java.junit.JkTestSuiteResult;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Beware : Experimental !!!!!!!!!!!!!!!!!!!!!!!
 * The API is likely to change subsequently.
 * <p>
 * Object responsible to build (to make) a java project. It provides methods to perform common build
 * task (compile, test, javadoc, package to jar).
 * All defined task are extensible so you can modify/improve the build behavior.
 */
public class JkJavaProjectMaker {

    private JkJavaProject project;

    private JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.mavenCentral())
            .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaDepScopes.DEFAULT_SCOPE_MAPPING));

    private JkJavaCompiler baseCompiler = JkJavaCompiler.base();

    private JkJavaCompiler testBaseCompiler = JkJavaCompiler.base();

    private JkUnit juniter = JkUnit.of().withOutputOnConsole(false);

    private List<String> javadocOptions = new LinkedList<>();

    private JkPublishRepos publishRepos = JkPublishRepos.local();

    public boolean runTests = true;

    private final Status status = new Status();

    private Map<JkArtifactFileId, Runnable> artifactProducers = new LinkedHashMap<>();

    private final JkJavaProjectPackager packager;

    private Supplier<String> artifactFileNameSupplier = () -> {
        if (project.getVersionedModule() != null) {
            return fileName(project.getVersionedModule());
        } else if (project.getArtifactName() != null) {
            return project.getArtifactName();
        }
        return project.baseDir().getName();
    };

    // commons ------------------------

    JkJavaProjectMaker(JkJavaProject project) {
        this.project = project;
        this.packager = JkJavaProjectPackager.of(project);
    }


    protected void compileAndTestIfNeeded() {
        if (!status.compileDone) {
            compile();
        }
        if (runTests && !status.unitTestDone) {
            test();
        }
    }

    public void generateJavadoc() {
        if (!status.compileDone) {
            compile();
        }
        JkJavadocMaker.of(project.getSourceLayout().sources(), project.getOutLayout().getJavadocDir())
                .withClasspath(depsFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION))
                .andOptions(this.javadocOptions).process();
        status.javadocGenerated = true;
    }

    public JkPath depsFor(JkScope... scopes) {
        return dependencyResolver.get(project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME),
                scopes);
    }



    // Clean -----------------------------------------------

    public final JkRunnables cleaner = JkRunnables.of(() -> {
        JkUtilsFile.deleteDirContent(project.getOutLayout().outputDir());
    });

    public JkJavaProjectMaker clean() {
        status.reset();
        cleaner.run();
        return this;
    }

    // Compile phase (produce binaries and process resources) -----------------------------

    public final JkRunnables beforeCompile = JkRunnables.noOp();

    public final JkRunnables sourceGenerator = JkRunnables.noOp();

    public final JkRunnables resourceGenerator = JkRunnables.noOp();

    public final JkRunnables resourceProcessor = JkRunnables.of(() -> {
        JkResourceProcessor.of(project.getSourceLayout().resources())
                .and(project.getOutLayout().generatedResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().classDir());
    });

    public final JkRunnables compiler = JkRunnables.of(() -> {
        JkJavaCompiler comp = baseCompiler.andOptions(project.getCompileSpec().asOptions());
        comp = applyCompileSource(comp);
        comp.compile();
    });

    private JkJavaCompiler applyCompileSource(JkJavaCompiler baseCompiler) {
        JkPath classpath = depsFor(JkJavaDepScopes.SCOPES_FOR_COMPILATION);
        return baseCompiler
                .withClasspath(classpath)
                .andSources(project.getSourceLayout().sources())
                .andSources(JkFileTree.of(project.getOutLayout().generatedSourceDir()))
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

    public final JkRunnables testResourceProcessor = JkRunnables.of(() -> {
        JkResourceProcessor.of(project.getSourceLayout().testResources())
                .and(project.getOutLayout().generatedTestResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().testClassDir());
    });

    public final JkRunnables testCompiler = JkRunnables.of(() -> {
        JkJavaCompiler comp = testBaseCompiler.andOptions(this.project.getCompileSpec().asOptions());
        comp = applyTestCompileSource(comp);
        comp.compile();
    });

    private JkJavaCompiler applyTestCompileSource(JkJavaCompiler baseCompiler) {
        JkPath classpath = depsFor(JkJavaDepScopes.SCOPES_FOR_TEST).andHead(project.getOutLayout().classDir());
        return baseCompiler
                .withClasspath(classpath)
                .andSources(project.getSourceLayout().tests())
                .withOutputDir(project.getOutLayout().testClassDir());
    }

    protected JkUnit juniter() {
        final JkClasspath classpath = JkClasspath.of(project.getOutLayout().testClassDir(), project.getOutLayout().classDir())
                .and(depsFor(JkJavaDepScopes.SCOPES_FOR_TEST));
        final File junitReport = new File(project.getOutLayout().testReportDir(), "junit");
        return this.getJuniter().withClassesToTest(project.getOutLayout().testClassDir())
                .withClasspath(classpath)
                .withReportDir(junitReport);
    }

    public final JkRunnables testExecutor = JkRunnables.of(() -> {
        JkUnit juniter = juniter();
        JkTestSuiteResult result = juniter.run();
    });


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

    // Package --------------------------------------------------------------------

    public final JkRunnables beforePackage = JkRunnables.of(() -> {
    });

    public File makeArtifactFile(JkArtifactFileId artifactFileId) {
        if (artifactProducers.containsKey(artifactFileId)) {
            JkLog.startln("Producing artifact file " + getArtifactFile(artifactFileId).getName());
            artifactProducers.get(artifactFileId).run();
            JkLog.done();
            return getArtifactFile(artifactFileId);
        } else {
            throw new IllegalArgumentException("No artifact with classifier/extension " + artifactFileId + " is defined on project " + this);
        }
    }

    File getArtifactFile(JkArtifactFileId artifactId) {
        final String namePart = artifactFileNameSupplier.get();
        String classifier = artifactId.classifier() == null ? "" : "-" + artifactId.classifier();
        String extension = artifactId.extension() == null ? "" : "." + artifactId.extension();
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

    // ----------------------- publish

    public JkJavaProjectMaker publish() {
        JkPublisher.of(this.publishRepos, project.getOutLayout().outputDir())
                .publishMaven(project.getVersionedModule(), project, project.getDependencies(), project.getMavenPublicationInfo());
        return this;
    }

    // ---------------------- from scratch

    public File makeBinJar() {
        compileAndTestIfNeeded();
        return packager.mainJar();
    }

    public File makeSourceJar() {
        if (!status.compileDone) {
            this.sourceGenerator.run();
            status.compileDone = true;
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

    private static class Status {
        private boolean compileDone = false;
        private boolean unitTestDone = false;
        private boolean packagingDone = false;
        private boolean javadocGenerated = false;

        void reset() {
            compileDone = false;
            unitTestDone = false;
            packagingDone = false;
            javadocGenerated = false;
        }

    }


}
