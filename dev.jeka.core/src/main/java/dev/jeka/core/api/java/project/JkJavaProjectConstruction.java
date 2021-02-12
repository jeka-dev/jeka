package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJarPacker;
import dev.jeka.core.api.java.JkManifest;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Responsible to produce jar files. It involves compilation and unit testing.
 * Compilation and tests can be run independently without creating jars.
 * <p>
 * Java Project Jar Production has common characteristics :
 * <ul>
 *     <li>Contains Java source files to be compiled</li>
 *     <li>All Java sources file (prod + test) are wrote against the same Java version and encoding</li>
 *     <li>The project may contain unit tests</li>
 *     <li>It can depends on any accepted dependencies (Maven module, other project, files on fs, ...)</li>
 *     <li>Part of the sources/resources may be generated</li>
 *     <li>By default, passing test suite is required to produce Jars.</li>
 * </ul>
 * Integration tests are outside of {@link JkJavaProjectConstruction} scope.
 */
public class JkJavaProjectConstruction {

    private final JkJavaProject project;

    private final JkDependencyResolver<JkJavaProjectConstruction> dependencyResolver;

    private final JkJavaProjectCompilation<JkJavaProjectConstruction> compilation;

    private final JkJavaProjectTesting testing;

    private PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    private final JkManifest manifest;

    private JkPathTreeSet extraFilesToIncludeInFatJar = JkPathTreeSet.ofEmpty();

    private UnaryOperator<JkDependencySet> dependencySetModifier = x -> x;
    
    /**
     * For Parent chaining
     */
    public JkJavaProject __;

    JkJavaProjectConstruction(JkJavaProject project) {
        this.project = project;
        this.__ = project;
        dependencyResolver = JkDependencyResolver.ofParent(this).addRepos(JkRepo.ofLocal(), JkRepo.ofMavenCentral());
        compilation = JkJavaProjectCompilation.ofProd(this);
        testing = new JkJavaProjectTesting(this);
        manifest = JkManifest.ofParent(this);
    }

    public JkJavaProjectConstruction apply(Consumer<JkJavaProjectConstruction> consumer) {
        consumer.accept(this);
        return this;
    }

    public JkDependencyResolver<JkJavaProjectConstruction> getDependencyResolver() {
        return dependencyResolver;
    }

    public JkJavaProjectCompilation<JkJavaProjectConstruction> getCompilation() {
        return compilation;
    }

    public JkJavaProjectTesting getTesting() {
        return testing;
    }

    public JkManifest<JkJavaProjectConstruction> getManifest() {
        return manifest;
    }

    public JkJavaProject getProject() {
        return project;
    }

    private void addManifestDefaults() {
        JkModuleId moduleId = project.getPublication().getModuleId();
        JkVersion version = project.getPublication().getVersion();
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_TITLE) == null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_TITLE, moduleId.getName());
        }
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_VENDOR) == null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_VENDOR, moduleId.getGroup());
        }
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_VERSION) == null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_VERSION, version.getValue());
        }
    }

    public void createBinJar(Path target) {
        compilation.runIfNecessary();
        testing.runIfNecessary();
        addManifestDefaults();
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
        testing.runIfNecessary();
        Iterable<Path> classpath = dependencyResolver.resolve(getRuntimeDependencies()
                .normalised(project.getDuplicateConflictStrategy())).getFiles();
        addManifestDefaults();
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
    public JkJavaProjectConstruction setExtraFilesToIncludeInFatJar(JkPathTreeSet extraFilesToIncludeInFatJar) {
        this.extraFilesToIncludeInFatJar = extraFilesToIncludeInFatJar;
        return this;
    }

    /**
     * Specify the dependencies to add or remove from the production compilation dependencies to
     * get the runtime dependencies.
     * @param modifier An function that define the runtime dependencies from the compilation ones.
     */
    public JkJavaProjectConstruction setRuntimeDependencies(UnaryOperator<JkDependencySet> modifier) {
        this.dependencySetModifier = modifier;
        return this;
    }

    public JkDependencySet getRuntimeDependencies() {
        return dependencySetModifier.apply(compilation.getDependencies());
    }


}
