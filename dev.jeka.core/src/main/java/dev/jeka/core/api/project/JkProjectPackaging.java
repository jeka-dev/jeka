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
 * Responsible to create binary, Javadoc and Source jars.
 */
public class JkProjectPackaging {

    public enum JarType {
        REGULAR, FAT
    }

    private final JkProject project;

    public final JkManifest manifest;

    private JkPathTreeSet fatJarExtraContent = JkPathTreeSet.ofEmpty();

    private final PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    public final JkJavadocProcessor javadocProcessor;

    private Function<JkDependencySet, JkDependencySet> dependencySetModifier = x -> x;

    // relative to output path
    private String javadocDir = "javadoc";

    private JkResolveResult cachedJkResolveResult;

     JkProjectPackaging(JkProject project) {
        this.project = project;
        this.manifest = JkManifest.of();
        this.javadocProcessor = JkJavadocProcessor.of();
    }

    public Path getJavadocDir() {
        return project.getOutputDir().resolve(javadocDir);
    }

    /**
     * Sets the directory, relative to 'jeka/output', where Javadoc will be generated. Initial value is 'javadoc'.
     */
    public JkProjectPackaging setJavadocDir(String javadocDir) {
        this.javadocDir = javadocDir;
        return this;
    }

    /**
     * Creates a Javadoc jar at the specified location.
     */
    public void createJavadocJar(Path target) {
        boolean created = createJavadocFiles();
        if (!created) {
            JkLog.warn("No javadoc files generated : skip javadoc jar.");
            return;
        }
        Path javadocDir = getJavadocDir();
        JkPathTree.of(javadocDir).zipTo(target);
    }

    /**
     * Creates a Javadoc jar at conventional location.
     */
    public void createJavadocJar() {
        createJavadocJar(project.artifactProducer.getArtifactPath(JkArtifactId.JAVADOC_ARTIFACT_ID));
    }

    /**
     * Creates a source jar at specified location.
     */
    public void createSourceJar(Path target) {
        JkProjectCompilation compilation = project.compilation;
        JkPathTreeSet allSources = compilation.layout.resolveSources().and(compilation
                .layout.resolveGeneratedSourceDir());
        if (!allSources.containFiles()) {
            JkLog.warn("No sources found : skip sources jar.");
            return;
        }
        compilation.layout.resolveSources().and(compilation
                .layout.resolveGeneratedSourceDir()).zipTo(target);
    }

    /**
     * Creates a source jar at the specified location.
     */
    public void createSourceJar() {
        createSourceJar(project.artifactProducer.getArtifactPath(JkArtifactId.SOURCES_ARTIFACT_ID));
    }

    /**
     * Creates a binary jar (without dependencies) at the specified location.
     */
    public void createBinJar(Path target) {
        project.compilation.runIfNeeded();
        project.testing.runIfNeeded();
        Path classDir = project.compilation.layout.resolveClassDir();
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

    /**
     * Creates a binary jar at conventional location.
     */
    public void createBinJar() {
        createBinJar(project.artifactProducer.getArtifactPath(JkArtifactId.ofMainArtifact("jar")));
    }

    /**
     * Creates a fat jar (jar containing all dependencies) at the specified location.
     */
    public void createFatJar(Path target) {
        project.compilation.runIfNeeded();
        project.testing.runIfNeeded();
        JkLog.startTask("Packing fat jar...");
        Iterable<Path> classpath = resolveRuntimeDependencies().getFiles();
        addManifestDefaults();
        JkJarPacker.of(project.compilation.layout.resolveClassDir())
                .withManifest(manifest)
                .withExtraFiles(getFatJarExtraContent())
                .makeFatJar(target, classpath, this.fatJarFilter);
        JkLog.endTask();
    }

    /**
     * Creates a fat jar at conventional location.
     */
    public void createFatJar() {
        createFatJar(project.artifactProducer.getArtifactPath(JkArtifactId.of("fat", "jar")));
    }

    /**
     * Allows customizing thz content of produced fat jar.
     */
    public JkProjectPackaging customizeFatJarContent(Function<JkPathTreeSet, JkPathTreeSet> customizer) {
        this.fatJarExtraContent = customizer.apply(fatJarExtraContent);
        return this;
    }

    /**
     * Specifies the dependencies to add or remove from the production compilation dependencies to
     * get the runtime dependencies.
     * @param modifier A function that define the runtime dependencies from the compilation ones.
     */
    public JkProjectPackaging configureRuntimeDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependencySetModifier = modifier;
        return this;
    }

    /**
     * Returns the runtime dependencies computed from 'compile' dependencies and modified
     * through {@link #configureRuntimeDependencies(Function)}
     */
    public JkDependencySet getRuntimeDependencies() {
        JkDependencySet baseDependencies = project.compilation.getDependencies();
        if (project.isIncludeTextAndLocalDependencies()) {
            baseDependencies = baseDependencies
                    .minus(project.textAndLocalDeps().getCompile().getEntries())
                    .and(project.textAndLocalDeps().getRuntime());
        }
        return dependencySetModifier.apply(baseDependencies);
    }

    /**
     * Returns the resolved (and cached) dependencies for the runtime.
     */
    public JkResolveResult resolveRuntimeDependencies() {
        if (cachedJkResolveResult != null) {
            return cachedJkResolveResult;
        }
        cachedJkResolveResult = project.dependencyResolver.resolve(getRuntimeDependencies()
                .normalised(project.getDuplicateConflictStrategy()));
        return cachedJkResolveResult;
    }

    /**
     * This method is meant to be consumed by an artifact producer that does not
     * produce artifact file itself (that's why the <code>target</code> parameter is not used.
     * It Just copies the manifest in the classDir director.
     *
     * @param target Not used. Just here co be used as a {@link Consumer<Path>}
     */
    public void includeManifestInClassDir(Path target) {
        project.compilation.runIfNeeded();
        project.testing.runIfNeeded();
        Path classDir = project.compilation.layout.resolveClassDir();
        if (!Files.exists(classDir)) {
            JkLog.warn("No class dir found.");
        }
        addManifestDefaults();
        manifest.writeToStandardLocation(project.compilation.layout.resolveClassDir());
    }

    private JkPathTreeSet getFatJarExtraContent() {
        return this.fatJarExtraContent;
    }

    private void addManifestDefaults() {
        JkModuleId jkModuleId = project.getModuleId();
        String version = project.getVersion().getValue();
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

    /*
     * Generates javadoc files (files + zip)
     */
    private boolean createJavadocFiles() {
        JkProjectCompilation compilation = project.compilation;
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

}
