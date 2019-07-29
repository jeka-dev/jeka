package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.java.JkJavaVersion;

/**
 * Minimal information necessary to generate metadata project file for IDE.
 */
public class JkJavaProjectIde {

    private final JkProjectSourceLayout sourceLayout;

    private final JkDependencySet dependencies;

    private final JkJavaVersion sourceVersion;

    private final JkDependencyResolver dependencyResolver;

    private JkJavaProjectIde(JkProjectSourceLayout sourceLayout, JkDependencySet dependencySet,
                             JkJavaVersion sourceVersion, JkDependencyResolver dependencyResolver) {
        this.sourceLayout = sourceLayout;
        this.dependencies = dependencySet;
        this.sourceVersion = sourceVersion;
        this.dependencyResolver = dependencyResolver;
    }

    public static JkJavaProjectIde ofDefault() {
        return new JkJavaProjectIde(JkProjectSourceLayout.ofMavenStyle(), JkDependencySet.of(), JkJavaVersion.V8,
                JkDependencyResolver.of(JkRepo.ofLocal(), JkRepo.ofMavenCentral()));
    }

    public JkJavaProjectIde withSourceLayout(JkProjectSourceLayout sourceLayout) {
        return new JkJavaProjectIde(sourceLayout, this.dependencies, this.sourceVersion,
                this.dependencyResolver);
    }

    public JkJavaProjectIde withDependencies(JkDependencySet dependencies) {
        return new JkJavaProjectIde(this.sourceLayout, dependencies, this.sourceVersion,
                this.dependencyResolver);
    }

    public JkJavaProjectIde withSourceVersion(JkJavaVersion sourceVersion) {
        return new JkJavaProjectIde(this.sourceLayout, this.dependencies, sourceVersion,
                this.dependencyResolver);
    }

    public JkJavaProjectIde withDependencyResolver(JkDependencyResolver dependencyResolver) {
        return new JkJavaProjectIde(this.sourceLayout, this.dependencies, this.sourceVersion,
                dependencyResolver);
    }

    public JkProjectSourceLayout getSourceLayout() {
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
}
