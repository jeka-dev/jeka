package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.java.JkJavaVersion;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Minimal information necessary to generate metadata project file for IDE.
 */
public class JkIdeSupport {



    public interface JkSupplier {

        JkIdeSupport getJavaIdeSupport();

    }

    private JkCompileLayout prodLayout;

    private JkCompileLayout testLayout;

    private JkQualifiedDependencySet dependencies;

    private JkJavaVersion sourceVersion;

    private JkDependencyResolver dependencyResolver;

    private List<Path> generatedSourceDirs = new LinkedList<>();

    private JkIdeSupport(Path baseDir) {
        this.prodLayout = JkCompileLayout.of().setBaseDir(baseDir);
        this.testLayout = JkCompileLayout.of()
                .setSourceMavenStyle(JkCompileLayout.Concern.TEST)
                .setStandardOutputDirs(JkCompileLayout.Concern.TEST)
                .setBaseDir(baseDir);
        this.dependencies = JkQualifiedDependencySet.of();
        this.sourceVersion = JkJavaVersion.V8;
        this.dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());;
    }

    public static JkIdeSupport of(Path baseDir) {
        return new JkIdeSupport(baseDir);
    }

    public JkCompileLayout getProdLayout() {
        return prodLayout;
    }

    public JkCompileLayout getTestLayout() {
        return testLayout;
    }

    public JkQualifiedDependencySet getDependencies() {
        return dependencies;
    }

    public JkJavaVersion getSourceVersion() {
        return sourceVersion;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public List<Path> getGeneratedSourceDirs() {
        return generatedSourceDirs;
    }

    public JkIdeSupport setProdLayout(JkCompileLayout prodLayout) {
        this.prodLayout = prodLayout;
        return this;
    }

    public JkIdeSupport setTestLayout(JkCompileLayout testLayout) {
        this.testLayout = testLayout;
        return this;
    }

    public JkIdeSupport setDependencies(JkQualifiedDependencySet dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public JkIdeSupport setDependencies(
            JkDependencySet allCompileDeps,
            JkDependencySet allRuntimeDeps,
            JkDependencySet allTestDeps) {
        return setDependencies(JkQualifiedDependencySet.computeIdeDependencies(allCompileDeps, allRuntimeDeps, allTestDeps));
    }

    public JkIdeSupport setSourceVersion(JkJavaVersion sourceVersion) {
        this.sourceVersion = sourceVersion;
        return this;
    }

    public JkIdeSupport setDependencyResolver(JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        return this;
    }

    public JkIdeSupport setGeneratedSourceDirs(List<Path> generatedSourceDirs) {
        this.generatedSourceDirs = generatedSourceDirs;
        return this;
    }
}
