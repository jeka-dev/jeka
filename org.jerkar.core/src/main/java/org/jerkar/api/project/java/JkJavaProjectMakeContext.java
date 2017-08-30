package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.utils.JkUtilsFile;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class JkJavaProjectMakeContext {

    public static JkJavaProjectMakeContext of() {
        return new JkJavaProjectMakeContext();
    }

    private JkDependencyResolver dependencyResolver = JkDependencyResolver.of(JkRepo.mavenCentral());

    private JkJavaCompiler baseCompiler = JkJavaCompiler.base();

    private JkJavaCompiler testBaseCompiler = JkJavaCompiler.base();

    private JkUnit juniter = JkUnit.of().withOutputOnConsole(false);

    private Class<?> javadocDoclet = null;


    // Produce bynary production phase -----------------------------

    private UnaryOperator<JkJavaCompiler> configuredProductionCompiler = (compiler) -> {return compiler;};

    private Consumer<JkJarProject> sourceGenerator = (project) -> {};

    private Consumer<JkJarProject> resourceGenerator = (project) -> {};

    private Consumer<JkJarProject> resourceProcessor = (project) -> {
        JkResourceProcessor.of(project.getSourceLayout().resources())
                .and(project.getOutLayout().generatedResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().classDir());
    };

    private Consumer<JkJarProject> postCompiler = (project) -> {};


    // Test phase -----------------------------------------------------

    private UnaryOperator<JkJavaCompiler> configuredTestCompiler = (compiler) -> {return compiler;};

    private UnaryOperator<JkUnit> configuredJuniter = (juniter) -> {return juniter;};

    private Consumer<JkJarProject> testPostCompiler = (project) -> {};

    private Consumer<JkJarProject> testResourceGenerator = (project) -> {};

    private Consumer<JkJarProject> testResourceProcessor = (project) -> {
        JkResourceProcessor.of(project.getSourceLayout().testResources())
                .and(project.getOutLayout().generatedTestResourceDir())
                .and(project.getResourceInterpolators())
                .generateTo(project.getOutLayout().testClassDir());
    };

    private Consumer<JkJarProject> postTestRunner = (project) -> {};



    // Other --------------------------------------------------------------------

    private Consumer<JkJarProject> cleaner = (project) -> {
        JkUtilsFile.deleteDirContent(project.getOutLayout().outputDir());
    };


    public JkJavaProjectMakeContext() {
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

    public UnaryOperator<JkUnit> getConfiguredJuniter() {
        return configuredJuniter;
    }

    public JkJavaProjectMakeContext setConfiguredJuniter(UnaryOperator<JkUnit> configuredJuniter) {
        this.configuredJuniter = configuredJuniter;
        return this;
    }

    public Consumer<JkJarProject> getSourceGenerator() {
        return sourceGenerator;
    }

    public JkJavaProjectMakeContext setSourceGenerator(Consumer<JkJarProject> sourceGenerator) {
        this.sourceGenerator = sourceGenerator;
        return this;
    }

    public Consumer<JkJarProject> getResourceGenerator() {
        return (Consumer<JkJarProject>) resourceGenerator;
    }

    public JkJavaProjectMakeContext setResourceGenerator(Consumer<JkJarProject> resourceGenerator) {
        this.resourceGenerator = resourceGenerator;
        return this;
    }

    public Consumer<JkJarProject> getTestResourceGenerator() {
        return testResourceGenerator;
    }

    public Consumer<JkJarProject> getTestResourceProcessor() {
        return testResourceProcessor;
    }

    public JkJavaProjectMakeContext setTestResourceGenerator(Consumer<JkJarProject> testResourceGenerator) {
        this.testResourceGenerator = testResourceGenerator;
        return this;
    }

    public Consumer<JkJarProject> getPostCompiler() {
        return postCompiler;
    }

    public JkJavaProjectMakeContext setPostCompiler(Consumer<JkJarProject> postCompiler) {
        this.postCompiler = postCompiler;
        return this;
    }

    public Consumer<JkJarProject> getResourceProcessor() {
        return resourceProcessor;
    }

    public Consumer<JkJarProject> getCleaner() {
        return cleaner;
    }

    public Consumer<JkJarProject> getTestPostCompiler() {
        return testPostCompiler;
    }

    public Consumer<JkJarProject> getPostTestRunner() {
        return postTestRunner;
    }
}
