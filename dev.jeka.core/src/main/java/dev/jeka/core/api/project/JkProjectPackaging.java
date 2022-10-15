package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJarPacker;
import dev.jeka.core.api.java.JkJavadocProcessor;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Responsible to create Javadoc and Source jar.
 */
public class JkProjectPackaging {

    private final JkProject project;

    private final JkManifest<JkProjectPackaging> manifest;

    private JkPathTreeSet fatJarContentCustomizer = JkPathTreeSet.ofEmpty();

    private PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    private final JkJavadocProcessor<JkProjectPackaging> javadocProcessor;

    private Function<JkDependencySet, JkDependencySet> dependencySetModifier = x -> x;

    // relative to output path
    private String javadocDir = "javadoc";

    /**
     * For parent chaining
     */
    public final JkProject __;

     JkProjectPackaging(JkProject project) {
        this.project = project;
        this.manifest = JkManifest.ofParent(this);
        this.__ = project;
        javadocProcessor = JkJavadocProcessor.ofParent(this);
    }

    public JkProjectPackaging apply(Consumer<JkProjectPackaging> consumer) {
         consumer.accept(this);
         return this;
    }

    public JkJavadocProcessor<JkProjectPackaging> getJavadocProcessor() {
        return javadocProcessor;
    }

    /**
     * Generates javadoc files (files + zip)
     */
    private void createJavadocFiles() {
        JkProjectCompilation compilation = project.getCompilation();
        Iterable<Path> classpath = project.getDependencyResolver()
                .resolve(compilation.getDependencies().normalised(project.getDuplicateConflictStrategy())).getFiles();
        Path dir = project.getOutputDir().resolve(javadocDir);
        JkPathTreeSet sources = compilation.getLayout().resolveSources();
        javadocProcessor.make(classpath, sources, dir);
    }

    public Path getJavadocDir() {
        return project.getOutputDir().resolve(javadocDir);
    }

    public JkProjectPackaging setJavadocDir(String javadocDir) {
        this.javadocDir = javadocDir;
        return this;
    }

    public JkManifest<JkProjectPackaging> getManifest() {
        return manifest;
    }

    public void createJavadocJar(Path target) {
        createJavadocFiles();
        Path javadocDir = getJavadocDir();
        JkPathTree.of(javadocDir).zipTo(target);
    }

    public void createJavadocJar() {
        createJavadocJar(project.getArtifactProducer().getArtifactPath(JkProject.JAVADOC_ARTIFACT_ID));
    }

    public void createSourceJar(Path target) {
        JkProjectCompilation compilation = project.getCompilation();
        compilation.getLayout().resolveSources().and(compilation
                .getLayout().resolveGeneratedSourceDir()).zipTo(target);
    }

    public void createBinJar() {
        createBinJar(project.getArtifactProducer().getArtifactPath(JkArtifactId.ofMainArtifact("jar")));
    }

    public JkPathTreeSet getExtraFilesToIncludeInJar() {
        return this.fatJarContentCustomizer;
    }

    /**
     * Allows customizing thz content of produced fat jar.
     */
    public JkProjectPackaging customizeFatJarContent(Function<JkPathTreeSet, JkPathTreeSet> customizer) {
        this.fatJarContentCustomizer = customizer.apply(fatJarContentCustomizer);
        return this;
    }

    /**
     * Specify the dependencies to add or remove from the production compilation dependencies to
     * get the runtime dependencies.
     * @param modifier A function that define the runtime dependencies from the compilation ones.
     */
    public JkProjectPackaging configureRuntimeDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependencySetModifier = modifier;
        return this;
    }

    public JkDependencySet getRuntimeDependencies() {
        JkDependencySet baseDependencies = project.getCompilation().getDependencies();
        if (project.isIncludeTextAndLocalDependencies()) {
            baseDependencies = baseDependencies
                    .minus(project.textAndLocalDeps().getCompileOnly().getEntries())
                    .and(project.textAndLocalDeps().getRuntimeOnly());
        }
        return dependencySetModifier.apply(baseDependencies);
    }

    public JkResolveResult resolveRuntimeDependencies() {
        return project.getDependencyResolver().resolve(getRuntimeDependencies()
                .normalised(project.getDuplicateConflictStrategy()));
    }

    public void createSourceJar() {
        createSourceJar(project.getArtifactProducer().getArtifactPath(JkProject.SOURCES_ARTIFACT_ID));
    }

    public void createBinJar(Path target) {
        project.getCompilation().runIfNeeded();
        project.getTesting().runIfNeeded();
        addManifestDefaults();
        JkJarPacker.of(project.getCompilation().getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeJar(target);
    }

    public void createFatJar(Path target) {
        project.getCompilation().runIfNeeded();
        project.getTesting().runIfNeeded();
        JkLog.startTask("Packing fat jar...");
        Iterable<Path> classpath = resolveRuntimeDependencies().getFiles();
        addManifestDefaults();
        JkJarPacker.of(project.getCompilation().getLayout().resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getExtraFilesToIncludeInJar())
                .makeFatJar(target, classpath, this.fatJarFilter);
        JkLog.endTask();
    }

    public void createFatJar() {
        createFatJar(project.getArtifactProducer().getArtifactPath(JkArtifactId.of("fat", "jar")));
    }

    private void addManifestDefaults() {
        JkModuleId jkModuleId = project.getPublication().getModuleId();
        String version = project.getPublication().getVersion().getValue();
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_TITLE) == null && jkModuleId != null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_TITLE, jkModuleId.getName());
        }
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_VENDOR) == null && jkModuleId != null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_VENDOR, jkModuleId.getGroup());
        }
        if (manifest.getMainAttribute(JkManifest.IMPLEMENTATION_VERSION) == null && version != null) {
            manifest.addMainAttribute(JkManifest.IMPLEMENTATION_VERSION, version);
        }
    }

}
