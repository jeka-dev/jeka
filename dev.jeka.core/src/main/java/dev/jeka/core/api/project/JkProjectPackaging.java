/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.JkJarPacker;
import dev.jeka.core.api.java.JkJavadocProcessor;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.JkDoc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Responsible to create binary, Javadoc and Source jars.
 */
public class JkProjectPackaging {

    public enum JarType {

        @JkDoc("Jar containing only classes and resources of the project.")
        REGULAR,

        @JkDoc("Jar including classes and resources of dependencies. This Jar is not shaded.")
        FAT
    }

    /**
     * Consumer container for customizing the manifest that will bbe included in constructed Jar files.
     */
    public final JkConsumers<JkManifest> manifestCustomizer = JkConsumers.of();

    /**
     * Provides fluent interface for producing Javadoc.
     */
    public final JkJavadocProcessor javadocProcessor;

    private final JkProject project;

    private JkPathTreeSet fatJarExtraContent = JkPathTreeSet.ofEmpty();

    private final PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    String mainClass;

    private Supplier<String> mainClassFinder;

    private Function<JkDependencySet, JkDependencySet> dependencySetModifier = x -> x;

    private JkResolveResult cachedJkResolveResult;

    JkProjectPackaging(JkProject project) {
        this.project = project;
        this.javadocProcessor = JkJavadocProcessor.of();
        this.mainClassFinder = this::findMainClass;
    }

    public Path getJavadocDir() {
        return project.getOutputDir().resolve("javadoc");
    }

    public JkManifest getManifest() {
        JkManifest manifest = defaultManifest();
        manifestCustomizer.accept(manifest);
        return manifest;
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
    public Path createJavadocJar() {
        Path path = project.artifactLocator.getArtifactPath(JkArtifactId.JAVADOC_ARTIFACT_ID);
        createJavadocJar(path);
        return path;
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
    public Path createSourceJar() {
        Path path = project.artifactLocator.getArtifactPath(JkArtifactId.SOURCES_ARTIFACT_ID);
        createSourceJar(path);
        return path;
    }

    /**
     * Creates a binary jar (without dependencies) at the specified location.
     */
    public void createBinJar(Path target) {
        project.compilation.runIfNeeded();
        Path classDir = project.compilation.layout.resolveClassDir();
        if (!Files.exists(classDir)) {
            JkLog.warn("No class dir found : skip bin jar.");
            return;
        }
        JkLog.startTask("create-bin-jar");
        JkJarPacker.of(classDir)
                .withManifest(getManifest())
                .makeJar(target);
        JkLog.endTask("Jar created at " + friendlyPath(target));
    }

    /**
     * Creates a binary jar at conventional location.
     */
    public Path createBinJar() {
        Path path = project.artifactLocator.getArtifactPath(JkArtifactId.ofMainArtifact("jar"));
        createBinJar(friendlyPath(path));
        return path;
    }

    /**
     * Creates a fat jar (jar containing all dependencies) at the specified location.
     */
    public void createFatJar(Path target) {
        project.compilation.runIfNeeded();
        Iterable<Path> classpath = resolveRuntimeDependenciesAsFiles();
        JkLog.startTask("create-fat-jar");
        JkJarPacker.of(project.compilation.layout.resolveClassDir())
                .withManifest(getManifest())
                .withExtraFiles(getFatJarExtraContent())
                .makeFatJar(target, classpath, this.fatJarFilter);
        JkLog.endTask("Fat Jar created at " + friendlyPath(target));
    }

    /**
     * Creates a fat jar at conventional location.
     */
    public Path createFatJar() {
        JkLog.startTask("pack-fat-jar");
        Path path = project.artifactLocator.getArtifactPath(JkArtifactId.of("fat", "jar"));
        createFatJar(path);
        JkLog.endTask("Fat jar created at " + friendlyPath(path));
        return path;
    }

    private static Path friendlyPath(Path path) {
        if (path.toString().startsWith("..")) {
            return path.toAbsolutePath().normalize();
        }
        return path;
    }

    /**
     * Sets the main class name to use in #runXxx and for building docker images.
     * The value <code>"auto"</code> means that it will bbe auto-discovered, this value
     * can be referenced using constant {@link JkProject#AUTO_FIND_MAIN_CLASS}
     */
    public JkProjectPackaging setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /**
     * Sets the main class finder for this project. The main class finder is responsible for
     * providing the name of the main class to use in the project. This can be used for running
     * the project or building Docker images.
     */
    public JkProjectPackaging setMainClassFinder(Supplier<String> mainClassFinder) {
        this.mainClassFinder = mainClassFinder;
        return this;
    }

    /**
     * Returns the main class name of the project. This might be <code>null</code> for
     * library projects.
     */
    public String getMainClass() {
        if (JkProject.AUTO_FIND_MAIN_CLASS.equals(mainClass)) {
            return mainClassFinder.get();
        }
        return this.mainClass;
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
     *
     * @param modifier A function that define the runtime dependencies from the compilation ones.
     */
    public JkProjectPackaging customizeRuntimeDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        this.dependencySetModifier = modifier;
        return this;
    }

    /**
     * Returns the runtime dependencies computed from 'compile' dependencies and modified
     * through {@link #customizeRuntimeDependencies(Function)}
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
     * Returns the resolved (and cached) dependencies needed at runtime.
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
     * Retrieves the runtime dependencies as a sequence of files.
     */
    public List<Path> resolveRuntimeDependenciesAsFiles() {
        return project.dependencyResolver.resolveFiles(getRuntimeDependencies());
    }

    /**
     * Copies the manifest file to the class directory.
     */
    public void copyManifestInClassDir() {
        project.compilation.runIfNeeded();
        project.testing.runIfNeeded();
        Path classDir = project.compilation.layout.resolveClassDir();
        if (!Files.exists(classDir)) {
            JkLog.warn("No class dir found.");
        }
        getManifest().writeToStandardLocation(project.compilation.layout.resolveClassDir());
    }

    String declaredMainClass() {
        return mainClass;
    }

    private String findMainClass() {
        JkUrlClassLoader ucl = JkUrlClassLoader.of(project.compilation.layout.resolveClassDir(),
                ClassLoader.getSystemClassLoader());
        return ucl.toJkClassLoader().findClassesHavingMainMethod().stream()
                .findFirst().orElse(null);
    }

    private JkPathTreeSet getFatJarExtraContent() {
        return this.fatJarExtraContent;
    }

    private JkManifest defaultManifest() {
        return JkManifest.of()
                .addImplementationInfo(project.getModuleId(), project.getVersion())
                .addMainClass(this.getMainClass())
                .addBuildInfo();
    }

    /*
     * Generates javadoc files (files + zip)
     */
    private boolean createJavadocFiles() {
        JkProjectCompilation compilation = project.compilation;
        Iterable<Path> classpath = project.dependencyResolver
                .resolve(compilation.getDependencies().normalised(project.getDuplicateConflictStrategy())).getFiles();
        JkPathTreeSet sources = compilation.layout.resolveSources();
        if (!sources.containFiles()) {
            return false;
        }
        javadocProcessor.make(classpath, sources, getJavadocDir());
        return true;
    }

}
