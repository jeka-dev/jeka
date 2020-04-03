package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.java.JkJavaVersion;

import java.nio.file.Path;

/**
 * Minimal information necessary to generate metadata project file for IDE.
 */
public class JkJavaIdeSupport {

    private JkCompileLayout sourceLayout;

    private JkCompileLayout testSourceLayout;

    private JkDependencySet dependencies;

    private JkJavaVersion sourceVersion;

    private JkDependencyResolver dependencyResolver;

    private JkJavaIdeSupport(Path baseDir) {
        this.sourceLayout = JkCompileLayout.of().setBaseDir(baseDir)
                .setStandardSource(JkCompileLayout.Concern.PROD, JkCompileLayout.Style.MAVEN);
        this.testSourceLayout = JkCompileLayout.of().setBaseDir(baseDir)
                .setStandardSource(JkCompileLayout.Concern.TEST, JkCompileLayout.Style.MAVEN);
        this.dependencies = JkDependencySet.of();
        this.sourceVersion = JkJavaVersion.V8;
        this.dependencyResolver = JkDependencyResolver.of().addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral());;
    }

    public static JkJavaIdeSupport of(Path baseDir) {
        return new JkJavaIdeSupport(baseDir);
    }

    public JkCompileLayout getSourceLayout() {
        return sourceLayout;
    }

    public JkCompileLayout getTestSourceLayout() {
        return sourceLayout;
    }

    public JkDependencySet getDependencies() {
        return dependencies;
    }

    public JkJavaVersion getSourceVersion() {
        return sourceVersion;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public JkJavaIdeSupport setSourceLayout(JkCompileLayout sourceLayout) {
        this.sourceLayout = sourceLayout;
        return this;
    }

    public JkJavaIdeSupport setTestSourceLayout(JkCompileLayout testSourceLayout) {
        this.testSourceLayout = testSourceLayout;
        return this;
    }

    public JkJavaIdeSupport setDependencies(JkDependencySet dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public JkJavaIdeSupport setSourceVersion(JkJavaVersion sourceVersion) {
        this.sourceVersion = sourceVersion;
        return this;
    }

    public JkJavaIdeSupport setDependencyResolver(JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        return this;
    }
}
