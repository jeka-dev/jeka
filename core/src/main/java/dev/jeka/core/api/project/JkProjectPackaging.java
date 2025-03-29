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
import dev.jeka.core.api.depmanagement.JkDependencySetModifier;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJarPacker;
import dev.jeka.core.api.java.JkJavadocProcessor;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkDoc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Responsible to create binary, Javadoc and Source jars.
 */
public class JkProjectPackaging {

    /**
     * Represents the identifier for an action to create a JAR file in the project lifecycle.
     * Used within the context of project execution to trigger the corresponding task or operation
     * that handles the creation of the Java Archive (JAR) artifact.
     */
    public static final String CREATE_JAR_ACTION = "create-jar";

    public enum JarType {

        @JkDoc("Jar containing only classes and resources of the project.")
        REGULAR,

        @JkDoc("Jar including classes and resources of dependencies. This Jar is not shaded.")
        FAT,

        @JkDoc("Jar including classes and resources of dependencies, by relocating packages of dependencies for avoiding classpath collisions.")
        SHADE
    }

    /**
     * Actions to execute when {@link JkProject#pack()} is invoked.<p>
     * By default, the build action creates a regular binary jar. It can be
     * replaced by an action creating other jars/artifacts or doing special
     * action as publishing a Docker image, for example.
     * <p>
     * To insert before the JAR action, use {@link JkRunnables#insertBefore(String, String, Runnable)}
     * by specifying the {@link #CREATE_JAR_ACTION} as action to insert before.
     * </p>
     */
    public final JkRunnables actions = JkRunnables.of().setLogTasks(true);

    /**
     * Consumer container for customizing the manifest that will bbe included in constructed Jar files.
     */
    public final JkConsumers<JkManifest> manifestCustomizer = JkConsumers.of();

    /**
     * Provides fluent interface for producing Javadoc.
     */
    public final JkJavadocProcessor javadocProcessor;

    public final JkDependencySetModifier runtimeDependencies = JkDependencySetModifier.of()
            .modify(deps -> baseDependencies());

    private final JkProject project;

    private Consumer<Path> jarMaker = this::createBinJar;;

    private JkPathTreeSet fatJarExtraContent = JkPathTreeSet.ofEmpty();

    private final PathMatcher fatJarFilter = JkPathMatcher.of(); // take all

    String mainClass;

    private boolean detectMainClass;

    private Supplier<String> mainClassFinder;

    private JkResolveResult cachedJkResolveResult;

    JkProjectPackaging(JkProject project) {
        this.project = project;
        this.javadocProcessor = JkJavadocProcessor.of();
        this.mainClassFinder = this::findMainClass;
        actions.append(CREATE_JAR_ACTION,
                () -> jarMaker.accept(project.artifactLocator.getMainArtifactPath()));
    }

    /**
     * Executes the packing process for this project, which includes compiling, testing, and creating JAR files.
     *
     * @see #actions
     */
    public void run() {
        this.project.compilation.runIfNeeded();  // Better to launch it first explicitly for log clarity
        this.actions.run();
    }

    /**
     * Sets a {@link Runnable} to create the JAR used by {@link JkProject#prepareRunJar(JkProject.RuntimeDeps)}
     */
    public JkProjectPackaging setJarMaker(Consumer<Path> jarMaker) {
        this.jarMaker = jarMaker;
        return this;
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
        boolean done = createBinJarQuiet(target);
        if (done) {
            JkLog.info("Jar created at: " + friendlyPath(target));
        }
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
        List<Path> classpath = resolveRuntimeDependenciesAsFiles();
        JkJarPacker.of(project.compilation.layout.resolveClassDir())
                .withManifest(getManifest())
                .withExtraFiles(getFatJarExtraContent())
                .makeFatJar(target, classpath, this.fatJarFilter);
        JkLog.info("Fat Jar created at: " + friendlyPath(target));
    }

    /**
     * Same as createFatJar but relocate packages of the dependency jars in
     * order to not collide with dependencies in classpath.
     */
    public void createShadeJar(Path target) {
        Path mainJar = JkUtilsPath.createTempFile("jk_original-shade-", ".jar");
        JkUtilsPath.deleteIfExists(mainJar);
        createBinJarQuiet(mainJar);
        Iterable<Path> classpath = resolveRuntimeDependenciesAsFiles();
        JkRepoSet repos = project.dependencyResolver.getRepos();
        JkJarPacker.makeShadeJar(repos, mainJar, classpath, target);
        JkUtilsPath.deleteIfExists(mainJar);
        JkLog.info("Shade jar created at: " + friendlyPath(target));
    }

    /**
     * Creates a fat jar at conventional location.
     */
    public Path createFatJar() {
        Path path = project.artifactLocator.getArtifactPath(JkArtifactId.of("fat", "jar"));
        createFatJar(path);
        return path;
    }

    private static Path friendlyPath(Path path) {
        if (path.toString().startsWith("..")) {
            return path.toAbsolutePath().normalize();
        }
        return path;
    }

    private boolean createBinJarQuiet(Path target) {
        project.compilation.runIfNeeded();
        Path classDir = project.compilation.layout.resolveClassDir();
        if (!Files.exists(classDir)) {
            JkLog.warn("No class dir found : skip bin jar.");
            return false;
        }
        JkJarPacker.of(classDir)
                .withManifest(getManifest())
                .makeJar(target);
        return true;
    }

    /**
     * Sets whether to detect the main class when running or building the project.
     */
    public JkProjectPackaging setDetectMainClass(boolean detectMainClass) {
        this.detectMainClass = detectMainClass;
        return this;
    }

    public boolean isDetectMainClass() {
        return detectMainClass;
    }

    /**
     * Sets the main class name to use in #runXxx and for building docker images.
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
        if (JkUtilsString.isBlank(mainClass) && detectMainClass) {
            return mainClassFinder.get();
        }
        return this.mainClass;
    }

    /**
     * Retrieves the main class name of the project. If the main class name is not provided,
     * it will try to discover it.
     */
    public String getOrFindMainClass() {
        if (!JkUtilsString.isBlank(mainClass)) {
            return mainClass;
        }
        return this.mainClassFinder.get();
    }

    /**
     * Allows customizing thz content of produced fat jar.
     */
    public JkProjectPackaging customizeFatJarContent(Function<JkPathTreeSet, JkPathTreeSet> customizer) {
        this.fatJarExtraContent = customizer.apply(fatJarExtraContent);
        return this;
    }

    /**
     * Returns the resolved (and cached) dependencies needed at runtime.
     */
    public JkResolveResult resolveRuntimeDependencies() {
        if (cachedJkResolveResult != null) {
            return cachedJkResolveResult;
        }
        cachedJkResolveResult = project.dependencyResolver.resolve(runtimeDependencies.get()
                .normalised(project.getDuplicateConflictStrategy()));
        return cachedJkResolveResult;
    }

    /**
     * Retrieves the runtime dependencies as a sequence of files.
     */
    public List<Path> resolveRuntimeDependenciesAsFiles() {
        return project.dependencyResolver.resolveFiles(runtimeDependencies.get());
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

    /*
     * Returns the base dependencies upon which to construct runtime deps.
     * It includes compile dependencies + dependencies declared as runtime
     * in both dependencies.txt and file located in conventional dir.
     */
    private JkDependencySet baseDependencies() {
        JkDependencySet baseDependencies = project.compilation.dependencies.get();
        if (project.isIncludeTextAndLocalDependencies()) {
            baseDependencies = baseDependencies
                    .minus(project.textAndLocalDeps().getCompile().getEntries())
                    .and(project.textAndLocalDeps().getRuntime());
        }
        return baseDependencies;
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
                .resolve(compilation.dependencies.get().normalised(project.getDuplicateConflictStrategy())).getFiles();
        JkPathTreeSet sources = compilation.layout.resolveSources();
        if (!sources.containFiles()) {
            return false;
        }
        javadocProcessor.make(classpath, sources, getJavadocDir());
        return true;
    }

}
