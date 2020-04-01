package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.java.JkJavaVersion;

/**
 * Minimal information necessary to generate metadata project file for IDE.
 */
public class JkJavaIdeSupport {

    private final JkProjectSourceLayout sourceLayout;

    private final JkDependencySet dependencies;

    private final JkJavaVersion sourceVersion;

    private final JkDependencyResolver dependencyResolver;

    private JkJavaIdeSupport(JkProjectSourceLayout sourceLayout, JkDependencySet dependencySet,
                             JkJavaVersion sourceVersion, JkDependencyResolver dependencyResolver) {
        this.sourceLayout = sourceLayout;
        this.dependencies = dependencySet;
        this.sourceVersion = sourceVersion;
        this.dependencyResolver = dependencyResolver;
    }

    public static JkJavaIdeSupport ofDefault() {
        return new JkJavaIdeSupport(JkProjectSourceLayout.ofMavenStyle(), JkDependencySet.of(), JkJavaVersion.V8,
                JkDependencyResolver.of().addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral()));
    }

    public JkJavaIdeSupport withSourceLayout(JkProjectSourceLayout sourceLayout) {
        return new JkJavaIdeSupport(sourceLayout, this.dependencies, this.sourceVersion,
                this.dependencyResolver);
    }

    public JkJavaIdeSupport withDependencies(JkDependencySet dependencies) {
        return new JkJavaIdeSupport(this.sourceLayout, dependencies, this.sourceVersion,
                this.dependencyResolver);
    }

    public JkJavaIdeSupport withSourceVersion(JkJavaVersion sourceVersion) {
        return new JkJavaIdeSupport(this.sourceLayout, this.dependencies, sourceVersion,
                this.dependencyResolver);
    }

    public JkJavaIdeSupport withDependencyResolver(JkDependencyResolver dependencyResolver) {
        return new JkJavaIdeSupport(this.sourceLayout, this.dependencies, this.sourceVersion,
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
