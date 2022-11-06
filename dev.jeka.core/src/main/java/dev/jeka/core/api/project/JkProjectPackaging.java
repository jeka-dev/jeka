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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Responsible to create Javadoc and Source jar.
 */
public class JkProjectPackaging {

    private final JkProject project;

    public final JkManifest<JkProjectPackaging> manifest;

    private JkPathTreeSet fatJarExtraContent = JkPathTreeSet.ofEmpty();

    private PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    public final JkJavadocProcessor<JkProjectPackaging> javadocProcessor;

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

    /**
     * Generates javadoc files (files + zip)
     */
    private boolean createJavadocFiles() {
        JkProjectCompilation compilation = project.prodCompilation;
        Iterable<Path> classpath = project.dependencyResolver
                .resolve(compilation.getDependencies().normalised(project.getDuplicateConflictStrategy())).getFiles();
        Path dir = project.getOutputDir().resolve(javadocDir);
        JkPathTreeSet sources = compilation.layout.resolveSources();
        if (!sources.containFiles()) {
            return false;
        }
        javadocProcessor.make(classpath, sources, dir);
        return true;
    }

    public Path getJavadocDir() {
        return project.getOutputDir().resolve(javadocDir);
    }

    public JkProjectPackaging setJavadocDir(String javadocDir) {
        this.javadocDir = javadocDir;
        return this;
    }

    public void createJavadocJar(Path target) {
        boolean created = createJavadocFiles();
        if (!created) {
            JkLog.warn("No javadoc files generated : skip javadoc jar.");
            return;
        }
        Path javadocDir = getJavadocDir();
        JkPathTree.of(javadocDir).zipTo(target);
    }

    public void createJavadocJar() {
        createJavadocJar(project.artifactProducer.getArtifactPath(JkProject.JAVADOC_ARTIFACT_ID));
    }

    public void createSourceJar(Path target) {
        JkProjectCompilation compilation = project.prodCompilation;
        JkPathTreeSet allSources = compilation.layout.resolveSources().and(compilation
                .layout.resolveGeneratedSourceDir());
        if (!allSources.containFiles()) {
            JkLog.warn("No sources found : skip sources jar.");
            return;
        }
        compilation.layout.resolveSources().and(compilation
                .layout.resolveGeneratedSourceDir()).zipTo(target);
    }

    public void createBinJar() {
        createBinJar(project.artifactProducer.getArtifactPath(JkArtifactId.ofMainArtifact("jar")));
    }

    public JkPathTreeSet getFatJarExtraContent() {
        return this.fatJarExtraContent;
    }

    /**
     * Allows customizing thz content of produced fat jar.
     */
    public JkProjectPackaging customizeFatJarContent(Function<JkPathTreeSet, JkPathTreeSet> customizer) {
        this.fatJarExtraContent = customizer.apply(fatJarExtraContent);
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
        JkDependencySet baseDependencies = project.prodCompilation.getDependencies();
        if (project.isIncludeTextAndLocalDependencies()) {
            baseDependencies = baseDependencies
                    .minus(project.textAndLocalDeps().getCompile().getEntries())
                    .and(project.textAndLocalDeps().getRuntime());
        }
        return dependencySetModifier.apply(baseDependencies);
    }

    public JkResolveResult resolveRuntimeDependencies() {
        return project.dependencyResolver.resolve(getRuntimeDependencies()
                .normalised(project.getDuplicateConflictStrategy()));
    }

    public void createSourceJar() {
        createSourceJar(project.artifactProducer.getArtifactPath(JkProject.SOURCES_ARTIFACT_ID));
    }

    public void createBinJar(Path target) {
        project.prodCompilation.runIfNeeded();
        project.testing.runIfNeeded();
        Path classDir = project.prodCompilation.layout.resolveClassDir();
        if (!Files.exists(classDir)) {
            JkLog.warn("No class dir found : skip bin jar.");
            return;
        }
        addManifestDefaults();
        JkJarPacker.of(classDir)
                .withManifest(manifest)
                .withExtraFiles(getFatJarExtraContent())
                .makeJar(target);
    }

    public void createFatJar(Path target) {
        project.prodCompilation.runIfNeeded();
        project.testing.runIfNeeded();
        JkLog.startTask("Packing fat jar...");
        Iterable<Path> classpath = resolveRuntimeDependencies().getFiles();
        addManifestDefaults();
        JkJarPacker.of(project.prodCompilation.layout.resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getFatJarExtraContent())
                .makeFatJar(target, classpath, this.fatJarFilter);
        JkLog.endTask();
    }

    public void createFatJar() {
        createFatJar(project.artifactProducer.getArtifactPath(JkArtifactId.of("fat", "jar")));
    }

    private void addManifestDefaults() {
        JkModuleId jkModuleId = project.publication.getModuleId();
        String version = project.publication.getVersion().getValue();
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
