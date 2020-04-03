package dev.jeka.core.api.java.project;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Folder layout for a project output.
 */
public class JkCompileLayout<T> {

    public enum Style {
        MAVEN, SIMPLE;
    }

    public enum Concern {
        PROD, TEST
    }

    private static final String GENERATED_SOURCE_PATH = "generated_sources/java";

    private static final String GENERATED_RESOURCE_PATH = "generated_resources";

    private static final String GENERATED_TEST_SOURCE_PATH = "generated_test_sources/java";

    private static final String GENERATED_TEST_RESOURCE_PATH = "generated_test_resources";

    /**
     * Filter to consider as resources everything but java source stuff.
     */
    public static final PathMatcher JAVA_RESOURCE_MATCHER = JkPathMatcher.of(false, "**/*.java", "*.java",
            "**/package.html", "package.html", "**/doc-files", "doc-files");

    /**
     * Filter to consider only Java source
     */
    public static final PathMatcher JAVA_SOURCE_MATCHER = JkPathMatcher.of(true, "**/*.java", "*.java");

    /**
     * Parent chaining
     */
    public final T __;

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


    private JkCompileLayout(T parent) {
        this.__ = parent;
        baseDirSupplier = () -> Paths.get(".");
        outputDirSupplier = () -> baseDirSupplier.get().resolve("jeka/output");
        setStandardSource(Concern.PROD, Style.MAVEN);
    }

    public static <T> JkCompileLayout<T> ofParent(T parent) {
        return new JkCompileLayout<>(parent);
    }

    public static <T> JkCompileLayout<T> of() {
        return ofParent(null);
    }

    public JkCompileLayout<T> setStandardSource(Concern concern, Style style) {
        if (style == Style.MAVEN) {
            setSourceMavenStyle(concern);
        } else {
            setSourceSimpleStyle(concern);
        }
        return this;
    }

    private void setSourceMavenStyle(Concern concern) {
        if (concern == Concern.PROD) {
            sources = JkPathTreeSet.of(Paths.get("src/main/java")).andMatcher(JAVA_SOURCE_MATCHER);
            resources = JkPathTreeSet.of(Paths.get("src/main/resources")).andMatcher(JAVA_RESOURCE_MATCHER);
        } else {
            sources = JkPathTreeSet.of(Paths.get("src/test/java")).andMatcher(JAVA_SOURCE_MATCHER);
            resources = JkPathTreeSet.of(Paths.get("src/test/resources")).andMatcher(JAVA_RESOURCE_MATCHER);
        }
    }

    private void setSourceSimpleStyle(Concern concern) {
        if (concern == Concern.TEST) {
            sources = JkPathTreeSet.of(Paths.get("src")).andMatcher(JAVA_SOURCE_MATCHER);
            resources = JkPathTreeSet.of(Paths.get("src")).andMatcher(JAVA_RESOURCE_MATCHER);
        } else {
            this.sources = JkPathTreeSet.of(Paths.get("test")).andMatcher(JAVA_SOURCE_MATCHER);
            this.resources = JkPathTreeSet.of(Paths.get("test")).andMatcher(JAVA_RESOURCE_MATCHER);
        }
    }

    public JkCompileLayout<T> setSources(JkPathTreeSet sources) {
        this.sources = sources;
        return this;
    }

    public JkCompileLayout<T> addSource(JkPathTree source) {
        return setSources(sources.and(source));
    }

    public JkCompileLayout<T> addSource(String relativeDir) {
        return addSource(JkPathTree.of(Paths.get(relativeDir)).andMatcher(JAVA_SOURCE_MATCHER));
    }

    public JkCompileLayout<T> removeSources() {
        return setSources(JkPathTreeSet.ofEmpty());
    }

    public JkCompileLayout<T> setResources(JkPathTreeSet resources) {
        this.resources = resources;
        return this;
    }

    public JkCompileLayout<T> addResource(JkPathTree resource) {
        return setResources(resources.and(resource));
    }

    public JkCompileLayout<T> addResource(String relativeDir) {
        return addResource(JkPathTree.of(Paths.get(relativeDir)).andMatcher(JAVA_RESOURCE_MATCHER));
    }

    public JkCompileLayout<T> removeResources() {
        return setResources(JkPathTreeSet.ofEmpty());
    }

    public JkCompileLayout<T> includeSourceDirsInResources() {
        return setResources(resources.and(sources.withMatcher(JAVA_RESOURCE_MATCHER)));
    }

    public JkCompileLayout<T> setBaseDirSupplier(Supplier<Path> supplier) {
        this.baseDirSupplier = supplier;
        return this;
    }

    public JkCompileLayout<T> setBaseDir(Path path) {
        return setBaseDirSupplier(() -> path);
    }

    public JkCompileLayout<T> setOutputDirSupplier(Supplier<Path> supplier) {
        this.outputDirSupplier = supplier;
        return this;
    }

    public JkCompileLayout<T> setOutputDir(Path path) {
        return setOutputDirSupplier(() -> path);
    }

    public JkCompileLayout<T> setGeneratedSourceDir(String path) {
        this.generatedSourceDir = path;
        return this;
    }

    public JkCompileLayout<T> setGeneratedResourceDir(String path) {
        this.generatedResourceDir = path;
        return this;
    }

    public JkCompileLayout<T> setClassDir(String path) {
        this.classDir = path;
        return this;
    }

    /**
     * Delete all directories involved in output production (classes, test classes, test reports, generated sources/resources)
     * but not the outputDir.
     */
    public void deleteOutputDirs() {
        JkPathTree.of(getClassDir()).deleteContent();
        JkPathTree.of(getGeneratedSourceDir()).deleteContent();
        JkPathTree.of(getGeneratedResourceDir()).deleteContent();
    }

    // --------------------------- Views ---------------------------------

    public Path getBaseDir() {
        return baseDirSupplier.get();
    }

    public Path getOutputDir() {
        return outputDirSupplier.get();
    }

    public Path getClassDir() {
        return outputDirSupplier.get().resolve(classDir);
    }

    /**
     * Returns location of generated sources.
     */
    public Path getGeneratedSourceDir() {
        return outputDirSupplier.get().resolve(generatedSourceDir);
    }

    /**
     * Returns location of generated resources.
     */
    public Path getGeneratedResourceDir() {
        return outputDirSupplier.get().resolve(generatedResourceDir);
    }

    public JkPathTreeSet getSources() {
        return sources.resolvedTo(baseDirSupplier.get());
    }

    public JkPathTreeSet getResources() {
        return resources.resolvedTo(outputDirSupplier.get());
    }

    public String getInfo() {
        return new StringBuffer("Sources : " + this.sources + "\n")
                .append("Resources : " + this.resources + "\n")
                .toString();
    }
}