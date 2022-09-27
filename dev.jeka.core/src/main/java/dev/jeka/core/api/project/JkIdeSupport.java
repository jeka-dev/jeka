package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkProjectDependencies;
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

    private JkCompileLayout<JkIdeSupport> prodLayout;

    private JkCompileLayout<JkIdeSupport>  testLayout;

    private JkQualifiedDependencySet dependencies;

    private JkJavaVersion sourceVersion;

    private JkDependencyResolver dependencyResolver;

    private List<Path> generatedSourceDirs = new LinkedList<>();

    private JkIdeSupport(Path baseDir) {
        this.prodLayout = JkCompileLayout.ofParent(this).setBaseDir(baseDir);
        this.testLayout = JkCompileLayout.ofParent(this)
                .setSourceMavenStyle(JkCompileLayout.Concern.TEST)
                .setStandardOutputDirs(JkCompileLayout.Concern.TEST)
                .setBaseDir(baseDir);
        this.dependencies = JkQualifiedDependencySet.of();
        this.sourceVersion = JkJavaVersion.V8;
        this.dependencyResolver = JkDependencyResolver.of().addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral());;
    }

    public static JkIdeSupport of(Path baseDir) {
        return new JkIdeSupport(baseDir);
    }

    public JkCompileLayout<JkIdeSupport> getProdLayout() {
        return prodLayout;
    }

    public JkCompileLayout<JkIdeSupport> getTestLayout() {
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

    public JkIdeSupport setDependencies(JkProjectDependencies projectDependencies,
                                        JkCoordinate.ConflictStrategy conflictStrategy) {
        return setDependencies(JkQualifiedDependencySet.computeIdeDependencies(projectDependencies, conflictStrategy));
    }

    public JkIdeSupport setDependencies(JkProjectDependencies projectDependencies) {
        return setDependencies(projectDependencies, JkCoordinate.ConflictStrategy.FAIL);
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
