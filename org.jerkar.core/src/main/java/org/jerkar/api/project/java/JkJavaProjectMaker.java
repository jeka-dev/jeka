package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkPublisher;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.java.junit.JkTestSuiteResult;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.utils.JkUtilsFile;

import java.io.File;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class JkJavaProjectMaker {

    private JkJavaProject project;

    private JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.mavenCentral());

    private JkJavaCompiler baseCompiler = JkJavaCompiler.base();

    private JkJavaCompiler testBaseCompiler = JkJavaCompiler.base();

    private JkUnit juniter = JkUnit.of().withOutputOnConsole(false);

    private Class<?> javadocDoclet = null;

    private JkPublisher publisher = JkPublisher.local();


    // Hint for caching build result
    private boolean commonBuildDone;

    public JkJavaProjectMaker(JkJavaProject project) {
        this.project = project;
    }

    // commons ------------------------


    protected void commonBuild() {
        if (commonBuildDone) {
            return;
        }
        runBinaryPhase();
        if (this.getJuniter() != null) {
            runUnitTestPhase();
        }
        commonBuildDone = true;
    }


    // Clean phase -----------------------------------------------

    private JkRunnables cleaner = JkRunnables.of(() -> {
        JkUtilsFile.deleteDirContent(project.getOutLayout().outputDir());
    });

    public void runCleanPhase() {
        commonBuildDone = false;
        cleaner.run();
    }

    // Produce binary production phase -----------------------------

    public final JkRunnables sourceGenerator = JkRunnables.of( () -> {} );

    public final JkRunnables resourceGenerator = JkRunnables.of( () -> {} );

    public final JkRunnables resourceProcessor = JkRunnables.of( () -> {
        JkResourceProcessor.of(project.getSourceLayout().resources())
                .and(project.getOutLayout().generatedResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().classDir());
    });

    private Runnable compiler = () -> {
        JkJavaCompiler comp = baseCompiler.andOptions(project.getCompileSpec().asOptions());
        comp = applyCompileSource(comp, this.dependencyResolver);
        comp.compile();
    };

    private JkJavaCompiler applyCompileSource(JkJavaCompiler baseCompiler, JkDependencyResolver dependencyResolver) {
        JkPath classpath = dependencyResolver.get(
                project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME),
                JkJavaDepScopes.SCOPES_FOR_COMPILATION);
        return baseCompiler
                .withClasspath(classpath)
                .andSources(project.getSourceLayout().sources())
                .andSources(JkFileTree.of(project.getOutLayout().generatedSourceDir()))
                .withOutputDir(project.getOutLayout().classDir());
    }

    private JkRunnables postBinaryPhase = JkRunnables.of(() -> {});

    public JkJavaProjectMaker runBinaryPhase() {
        sourceGenerator.run();
        resourceGenerator.run();
        compiler.run();
        resourceProcessor.run();
        postBinaryPhase.run();
        return this;
    }

    // Test phase -----------------------------------------------------


    private JkRunnables testResourceGenerator = JkRunnables.of(() -> {});

    private JkRunnables testResourceProcessor = JkRunnables.of(() -> {
        JkResourceProcessor.of(project.getSourceLayout().testResources())
                .and(project.getOutLayout().generatedTestResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().testClassDir());
    });

    private JkRunnables testCompiler = JkRunnables.of(() -> {
        JkJavaCompiler comp = testBaseCompiler.andOptions(this.project.getCompileSpec().asOptions());
        comp.compile();
    });

    protected JkUnit juniter() {
        final JkClasspath classpath = JkClasspath.of(project.getOutLayout().testClassDir(), project.getOutLayout().classDir()).and(
                this.dependencyResolver.get(project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME)
                        , JkJavaDepScopes.SCOPES_FOR_TEST));
        final File junitReport = new File(project.getOutLayout().testReportDir(), "junit");
        return this.getJuniter().withClassesToTest(project.getOutLayout().testClassDir())
                .withClasspath(classpath)
                .withReportDir(junitReport);
    }

    private JkRunnables testExecutor = JkRunnables.of(() -> {
        JkUnit juniter = juniter();
        JkTestSuiteResult result = juniter.run();
    });


    private JkRunnables postTestPhase = JkRunnables.of(() -> {});

    public JkJavaProjectMaker runUnitTestPhase() {
        testCompiler.run();
        testResourceGenerator.run();
        testResourceProcessor.run();
        testExecutor.run();
        postTestPhase.run();
        return this;
    }

    // Packaging phase --------------------------------------------------------------------

    public File generateJavadoc() {
        JkJavadocMaker maker = JkJavadocMaker.of(project.getSourceLayout().sources(), project.getOutLayout().getJavadocDir())
                .withDoclet(this.getJavadocDoclet());
        maker.process();
        return project.getOutLayout().getJavadocDir();
    }

    public File packMainJar() {
        return getJarMaker().mainJar(project.getManifest(), project.getOutLayout().classDir(), JkFileTreeSet.empty());
    }

    public File packFatJar(String suffix) {
        JkClasspath classpath = JkClasspath.of(this.dependencyResolver.get(project.getDependencies(), JkJavaDepScopes.RUNTIME));
        return getJarMaker().fatJar(project.getManifest(), suffix, project.getOutLayout().classDir(), classpath,
                project.getExtraFilesToIncludeInFatJar());
    }

    public File packTestJar() {
        return getJarMaker().testJar(project.getOutLayout().testClassDir());
    }



    public File packSourceJar() {
        return getJarMaker().jar(project.getSourceLayout().sources().and(project.getSourceLayout().resources()), "sources");
    }

    public File packTestSourceJar() {
        return getJarMaker().jar(project.getSourceLayout().tests().and(project.getSourceLayout().testResources()), "testSources");
    }

    public File packJavadocJar() {
        return getJarMaker().jar(JkFileTreeSet.of(project.getOutLayout().getJavadocDir()), "javadoc");
    }

    private JkRunnables postPack = JkRunnables.of(() -> {});

    public JkJavaProjectMaker runPackagePhase() {
        project.doAllArtifactFiles();
        return this;
    }


    // ----------------------- publish phase

    public JkJavaProjectMaker runPublishPhase() {
        this.getPublisher().publishMaven(project.getVersionedModule(), project, project.getDependencies(), project.getMavenPublicationInfo());
        return this;
    }


    // -------------------- getters/setters -------------------------------------------------------


    public JkJarMaker getJarMaker() {
        return JkJarMaker.of(project.getOutLayout().outputDir(), project.getArtifactName());
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

    public JkPublisher getPublisher() {
        return this.publisher;
    }

    public JkJavaProjectMaker setPublisher(JkPublisher publisher) {
        this.publisher = publisher;
        return this;
    }

    public Class<?> getJavadocDoclet() {
        return javadocDoclet;
    }

    public JkJavaProjectMaker setJavadocDoclet(Class<?> javadocDoclet) {
        this.javadocDoclet = javadocDoclet;
        return this;
    }


}
