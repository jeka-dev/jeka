package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.utils.JkUtilsFile;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class JkJavaProjectMakeContext {

    private JkJavaProject project;

    private JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.mavenCentral());

    private JkJavaCompiler baseCompiler = JkJavaCompiler.base();

    private JkJavaCompiler testBaseCompiler = JkJavaCompiler.base();

    private JkUnit juniter = JkUnit.of().withOutputOnConsole(false);

    private Class<?> javadocDoclet = null;


    // Produce bynary production phase -----------------------------

    private UnaryOperator<JkJavaCompiler> configuredProductionCompiler = (compiler) -> {return compiler;};

    private Runnable sourceGenerator = () -> {};

    private Runnable resourceGenerator = () -> {};

    private Runnable resourceProcessor = () -> {
        JkResourceProcessor.of(project.getSourceLayout().resources())
                .and(project.getOutLayout().generatedResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().classDir());
    };

    private Runnable postCompilePhase = () -> {};


    // Test phase -----------------------------------------------------

    private UnaryOperator<JkJavaCompiler> configuredTestCompiler = (compiler) -> {return compiler;};

    private Function<JkUnit, JkUnit> juniterConfigurer = UnaryOperator.identity();

    private Runnable testPostCompiler = () -> {};

    private Runnable testResourceGenerator = () -> {};

    private Runnable testResourceProcessor = () -> {
        JkResourceProcessor.of(project.getSourceLayout().testResources())
                .and(project.getOutLayout().generatedTestResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().testClassDir());
    };

    private Runnable postTestPhase = () -> {};



    // Other --------------------------------------------------------------------

    private Runnable cleaner = () -> {
        JkUtilsFile.deleteDirContent(project.getOutLayout().outputDir());
    };

    private Runnable postPack = () -> {};

    // -------------------------------------------

    public JkJavaProjectMakeContext(JkJavaProject project) {
        this.project = project;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public JkJavaProjectMakeContext setDependencyResolver(JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        return this;
    }

    public JkJavaCompiler getBaseCompiler() {
        return baseCompiler;
    }

    public JkJavaProjectMakeContext setBaseCompiler(JkJavaCompiler baseCompiler) {
        this.baseCompiler = baseCompiler;
        return this;
    }

    public JkJavaCompiler getTestBaseCompiler() {
        return testBaseCompiler;
    }

    public JkJavaProjectMakeContext setTestBaseCompiler(JkJavaCompiler testBaseCompiler) {
        this.testBaseCompiler = testBaseCompiler;
        return this;
    }

    public JkUnit getJuniter() {
        return juniter;
    }

    public JkJavaProjectMakeContext setJuniter(JkUnit juniter) {
        this.juniter = juniter;
        return this;
    }

    public Class<?> getJavadocDoclet() {
        return javadocDoclet;
    }

    public JkJavaProjectMakeContext setJavadocDoclet(Class<?> javadocDoclet) {
        this.javadocDoclet = javadocDoclet;
        return this;
    }

    public UnaryOperator<JkJavaCompiler> getConfiguredProductionCompiler() {
        return configuredProductionCompiler;
    }

    public JkJavaProjectMakeContext setConfiguredProductionCompiler(UnaryOperator<JkJavaCompiler> configuredProductionCompiler) {
        this.configuredProductionCompiler = configuredProductionCompiler;
        return this;
    }

    public UnaryOperator<JkJavaCompiler> getConfiguredTestCompiler() {
        return configuredTestCompiler;
    }

    public JkJavaProjectMakeContext setConfiguredTestCompiler(UnaryOperator<JkJavaCompiler> configuredTestCompiler) {
        this.configuredTestCompiler = configuredTestCompiler;
        return this;
    }

    public Function<JkUnit, JkUnit> getJuniterConfigurer() {
        return juniterConfigurer;
    }

    public JkJavaProjectMakeContext chainJuniterConfigurer(final Function<JkUnit, JkUnit> configurer) {
        this.juniterConfigurer = this.juniterConfigurer.andThen(configurer);
        return this;
    }

    public Runnable getSourceGenerator() {
        return sourceGenerator;
    }

    public JkJavaProjectMakeContext setSourceGenerator(Runnable sourceGenerator) {
        this.sourceGenerator = sourceGenerator;
        return this;
    }

    public Runnable getResourceGenerator() {
        return resourceGenerator;
    }

    public JkJavaProjectMakeContext setResourceGenerator(Runnable resourceGenerator) {
        this.resourceGenerator = resourceGenerator;
        return this;
    }

    public Runnable getTestResourceGenerator() {
        return testResourceGenerator;
    }

    public Runnable getTestResourceProcessor() {
        return testResourceProcessor;
    }

    public JkJavaProjectMakeContext setTestResourceGenerator(Runnable testResourceGenerator) {
        this.testResourceGenerator = testResourceGenerator;
        return this;
    }

    public JkJavaProjectMakeContext chainTestResourceGenerator(Runnable testResourceGenerator) {
        this.testResourceGenerator = chain(this.testResourceProcessor, testResourceGenerator);
        return this;
    }

    public Runnable getPostCompilePhase() {
        return postCompilePhase;
    }

    public JkJavaProjectMakeContext setPostCompilePhase(Runnable postCompilePhase) {
        this.postCompilePhase = postCompilePhase;
        return this;
    }

    public Runnable getResourceProcessor() {
        return resourceProcessor;
    }

    public Runnable getCleaner() {
        return cleaner;
    }

    public Runnable getTestPostCompiler() {
        return testPostCompiler;
    }

    public Runnable getPostTestPhase() {
        return postTestPhase;
    }

    private Runnable chain(Runnable runnable1, Runnable runnable2) {
        return () -> {runnable1.run(); runnable2.run();};
    }
}
