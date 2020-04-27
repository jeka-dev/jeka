package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkArtifactId;
import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJarPacker;
import dev.jeka.core.api.java.JkManifest;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Consumer;

/**
 * Tasks for packaging artifacts created by the holding project.
 */
public class JkJavaProjectProduction {

    private final JkJavaProject project;

    private final JkJavaProjectCompilation<JkJavaProjectProduction> compilation;

    private PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    private final JkManifest manifest;

    private JkPathTreeSet extraFilesToIncludeInFatJar = JkPathTreeSet.ofEmpty();
    
    /**
     * For Parent chaining
     */
    public JkJavaProject __;

    JkJavaProjectProduction(JkJavaProject project) {
        this.project = project;
        this.__ = project;
        compilation = JkJavaProjectCompilation.ofProd(project, this);
        manifest = JkManifest.ofParent(this);
    }

    public JkJavaProjectProduction apply(Consumer<JkJavaProjectProduction> consumer) {
        consumer.accept(this);
        return this;
    }

    public JkJavaProjectCompilation<JkJavaProjectProduction> getCompilation() {
        return compilation;
    }

    public JkManifest<JkJavaProjectProduction> getManifest() {
        return manifest;
    }

    public void createBinJar(Path target) {
        compilation.runIfNecessary();
        project.getTesting().runIfNecessary();
        JkJarPacker.of(compilation.getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeJar(target);
    }

    public void createBinJar() {
        createBinJar(project.getArtifactPath(JkArtifactId.ofMainArtifact("jar")));
    }

    public void createFatJar(Path target) {
        compilation.runIfNecessary();
        project.getTesting().runIfNecessary();
        Iterable<Path> classpath = project.getDependencyManagement()
                .fetchDependencies(JkScope.RUNTIME).getFiles();
        JkJarPacker.of(compilation.getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeFatJar(target, classpath, this.fatJarFilter);
    }

    public void createFatJar() {
        createFatJar(project.getArtifactPath(JkArtifactId.of("fat", "jar")));
    }

    public JkPathTreeSet getExtraFilesToIncludeInJar() {
        return this.extraFilesToIncludeInFatJar;
    }

    /**
     * File trees specified here will be added to the fat jar.
     */
    public JkJavaProjectProduction setExtraFilesToIncludeInFatJar(JkPathTreeSet extraFilesToIncludeInFatJar) {
        this.extraFilesToIncludeInFatJar = extraFilesToIncludeInFatJar;
        return this;
    }

}
