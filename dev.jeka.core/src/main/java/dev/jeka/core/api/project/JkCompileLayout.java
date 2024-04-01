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

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Folder layout for a project output.
 */
public class JkCompileLayout {

    public enum Concern {
        PROD, TEST
    }

    public enum Style {SIMPLE, MAVEN}

    private static final String GENERATED_SOURCE_PATH = "generated_sources/java";

    private static final String GENERATED_RESOURCE_PATH = "generated_resources";

    private static final String GENERATED_TEST_SOURCE_PATH = "generated_test_sources/java";

    private static final String GENERATED_TEST_RESOURCE_PATH = "generated_test_resources";

    /**
     * Filter to consider as resources everything but java source stuff.
     */
    public static final PathMatcher JAVA_RESOURCE_MATCHER = JkPathMatcher.of(false,
            "**/*.java", "*.java", "**/package.html", "package.html", "**/doc-files", "doc-files");

    private Supplier<Path> baseDirSupplier;

    private Supplier<Path> outputDirSupplier;

    /*
     * Returns the location of production source code to compile.
     */
    private JkPathTreeSet sources;

    /**
     *  Returns the location of production resources to process.
     */
    private JkPathTreeSet resources;

    // Relative path from output dir
    private String generatedSourceDir;

    // Relative path from output dir
    private String generatedResourceDir;

    // Relative path from output dir
    private String classDir;

    private JkCompileLayout() {
        baseDirSupplier = () -> Paths.get("");
        outputDirSupplier = () -> baseDirSupplier.get().resolve(JkConstants.OUTPUT_PATH);
        setSourceMavenStyle(Concern.PROD);
        setStandardOutputDirs(Concern.PROD);
    }

    /**
     * Creates a default layout respecting Maven standard for sources. <p>
     * This means sources in <i>[baseDir]/src/main/java</i> and resources in  <i>[baseDir]/src/main/resources</i></li>
     * and using "." directory as base dir.
     */
    public static JkCompileLayout of() {
        return new JkCompileLayout();
    }

    /**
     * Creates a default layout respecting Maven standard for sources. This means :
     * <ul>
     *     <li>For production : sources in <i>[baseDir]/src/main/java</i> and resources in  <i>[baseDir]/src/main/resources</i></li>
     *     <li>For test : sources in <i>[baseDir]/src/test/java</i> and resources in  <i>[baseDir]/src/test/resources</i></li>
     * </ul>
     */
    public JkCompileLayout setSourceMavenStyle(Concern concern) {
        if (concern == Concern.PROD) {
            sources = JkPathTreeSet.ofRoots(Paths.get("src/main/java"));
            resources = JkPathTreeSet.ofRoots(Paths.get("src/main/resources"));
        } else {
            sources = JkPathTreeSet.ofRoots(Paths.get("src/test/java"));
            resources = JkPathTreeSet.ofRoots(Paths.get("src/test/resources"));
        }
        return this;
    }

    /**
     * Sets the <i>simple</i> standard layout on this {@link JkCompileLayout}. This means :
     * <ul>
     *     <li>For production code : sources and resources are located in the same directory [baseDir]/src</li>
     *     <li>For test code : sources and resources are located in the same directory [baseDir]/test</li>
     * </ul>
     */
    public JkCompileLayout setSourceSimpleStyle(Concern concern) {
        if (concern == Concern.PROD) {
            sources = JkPathTreeSet.ofRoots(Paths.get("src"));
            resources = JkPathTreeSet.ofRoots(Paths.get("src")).andMatcher(JAVA_RESOURCE_MATCHER);
        } else {
            this.sources = JkPathTreeSet.ofRoots(Paths.get("test"));
            this.resources = JkPathTreeSet.ofRoots(Paths.get("test")).andMatcher(JAVA_RESOURCE_MATCHER);
        }
        return this;
    }

    public JkCompileLayout setStandardOutputDirs(Concern concern) {
        if (concern == Concern.PROD) {
            this.classDir = "classes";
            this.generatedSourceDir = GENERATED_SOURCE_PATH;
            this.generatedResourceDir = GENERATED_RESOURCE_PATH;
        } else {
            this.classDir = "test-classes";
            this.generatedSourceDir = GENERATED_TEST_SOURCE_PATH;
            this.generatedResourceDir = GENERATED_TEST_RESOURCE_PATH;
        }
        return this;
    }

    public JkCompileLayout setSources(JkPathTreeSet sources) {
        this.sources = sources;
        return this;
    }

    public JkCompileLayout setSources(JkPathTree sources) {
        return setSources(sources.toSet());
    }

    public JkCompileLayout setSources(Function<JkPathTreeSet, JkPathTreeSet> sourceTransformer) {
        this.sources = sourceTransformer.apply(this.sources);
        return this;
    }

    public JkCompileLayout addSource(JkPathTree source) {
        return setSources(sources.and(source));
    }

    public JkCompileLayout addSource(JkPathTreeSet source) {
        return setSources(sources.and(source));
    }

    public JkCompileLayout setSources(String dir) {
        return setSources(JkPathTreeSet.ofRoots(Paths.get(dir)));
    }

    public JkCompileLayout addSource(Path dir) {
        return addSource(JkPathTree.of(dir));
    }

    public JkCompileLayout addSource(String path) {
        return addSource(Paths.get(path));
    }

    public JkCompileLayout emptySources() {
        return setSources(JkPathTreeSet.ofEmpty());
    }

    public JkCompileLayout setResources(JkPathTreeSet resources) {
        this.resources = resources;
        return this;
    }

    public JkCompileLayout addResource(JkPathTree resource) {
        return setResources(resources.and(resource));
    }

    public JkCompileLayout addResource(Path path) {
        return addResource(JkPathTree.of(path).andMatcher(JAVA_RESOURCE_MATCHER));
    }

    public JkCompileLayout addResource(String relativeDir) {
        return addResource(Paths.get(relativeDir));
    }

    public JkCompileLayout emptyResources() {
        return setResources(JkPathTreeSet.ofEmpty());
    }

    /**
     * All non .java files located in a source directory will be considered as a resource (copied in classes file)
     */
    public JkCompileLayout mixResourcesAndSources() {
        return setResources(sources.withMatcher(JAVA_RESOURCE_MATCHER));
    }

    public JkCompileLayout setBaseDirSupplier(Supplier<Path> supplier) {
        this.baseDirSupplier = supplier;
        return this;
    }

    public JkCompileLayout setBaseDir(Path path) {
        return setBaseDirSupplier(() -> path);
    }

    public JkCompileLayout setOutputDirSupplier(Supplier<Path> supplier) {
        this.outputDirSupplier = supplier;
        return this;
    }

    public JkCompileLayout setOutputDir(Path path) {
        return setOutputDirSupplier(() -> path);
    }

    public JkCompileLayout setGeneratedSourceDir(String path) {
        this.generatedSourceDir = path;
        return this;
    }

    public JkCompileLayout setGeneratedResourceDir(String path) {
        this.generatedResourceDir = path;
        return this;
    }

    public JkCompileLayout setClassDir(String path) {
        this.classDir = path;
        return this;
    }

    /**
     * Delete all directories involved in output production (classes, test classes, test reports, generated sources/resources)
     * but not the outputDir.
     */
    public void deleteOutputDirs() {
        JkPathTree.of(resolveClassDir()).deleteContent();
        JkPathTree.of(resolveGeneratedSourceDir()).deleteContent();
        JkPathTree.of(resolveGeneratedResourceDir()).deleteContent();
    }

    // --------------------------- Views ---------------------------------

    public Path getBaseDir() {
        return baseDirSupplier.get();
    }

    public Path getOutputDir() {
        return outputDirSupplier.get();
    }

    public JkPathTreeSet getSources() {
        return sources;
    }

    public JkPathTreeSet getResources() {
        return resources;
    }

    public String getGeneratedSourceDir() {
        return generatedSourceDir;
    }

    public String getGeneratedResourceDir() {
        return generatedResourceDir;
    }

    public String getClassDir() {
        return classDir;
    }

    public Path getClassDirPath() {
        return getOutputDir().resolve(classDir);
    }

    /**
     * Returns the class dir directory resolved against the current base dir.
     */
    public Path resolveClassDir() {
        return outputDirSupplier.get().resolve(classDir);
    }

    /**
     * Returns location of generated sources.
     */
    public Path resolveGeneratedSourceDir() {
        return outputDirSupplier.get().resolve(generatedSourceDir);
    }

    /**
     * Returns location of generated resources.
     */
    public Path resolveGeneratedResourceDir() {
        return outputDirSupplier.get().resolve(generatedResourceDir);
    }

    public JkPathTreeSet resolveSources() {
        return sources.resolvedTo(baseDirSupplier.get());
    }

    public JkPathTreeSet resolveResources() {
        return resources.resolvedTo(baseDirSupplier.get());
    }

    public String getInfo() {
        return "Sources =" + this.sources + ", Resources = " + this.resources;
    }
}