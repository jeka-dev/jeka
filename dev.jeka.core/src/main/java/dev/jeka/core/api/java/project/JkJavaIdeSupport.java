package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.tool.JkClass;

import java.nio.file.Path;
import java.util.List;

/**
 * Minimal information necessary to generate metadata project file for IDE.
 */
public class JkJavaIdeSupport {

    public interface JkSupplier {

        JkJavaIdeSupport getJavaIdeSupport();

    }

    private JkCompileLayout<JkJavaIdeSupport> prodLayout;

    private JkCompileLayout<JkJavaIdeSupport>  testLayout;

    private JkQualifiedDependencySet dependencies;

    private JkJavaVersion sourceVersion;

    private JkDependencyResolver dependencyResolver;

    private JkJavaIdeSupport(Path baseDir) {
        this.prodLayout = JkCompileLayout.ofParent(this).setBaseDir(baseDir);
        this.dependencies = JkQualifiedDependencySet.of();
        this.sourceVersion = JkJavaVersion.V8;
        this.dependencyResolver = JkDependencyResolver.of().addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral());;
    }

    public static JkJavaIdeSupport of(Path baseDir) {
        return new JkJavaIdeSupport(baseDir);
    }

    public JkCompileLayout<JkJavaIdeSupport> getProdLayout() {
        return prodLayout;
    }

    public JkCompileLayout<JkJavaIdeSupport> getTestLayout() {
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

    public JkJavaIdeSupport setProdLayout(JkCompileLayout prodLayout) {
        this.prodLayout = prodLayout;
        return this;
    }

    public JkJavaIdeSupport setTestLayout(JkCompileLayout testLayout) {
        this.testLayout = testLayout;
        return this;
    }

    public JkJavaIdeSupport setDependencies(JkQualifiedDependencySet dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public JkJavaIdeSupport setDependencies(JkDependencySet compile, JkDependencySet runtime, JkDependencySet test,
                                            JkVersionedModule.ConflictStrategy conflictStrategy) {
        return setDependencies(JkQualifiedDependencySet.computeIdeDependencies(compile, runtime, test, conflictStrategy));
    }

    public JkJavaIdeSupport setDependencies(JkDependencySet compile, JkDependencySet runtime, JkDependencySet test) {
        return setDependencies(compile, runtime, test, JkVersionedModule.ConflictStrategy.FAIL);
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
