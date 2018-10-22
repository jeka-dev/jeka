package org.jerkar.api.java.project;

import org.jerkar.api.file.JkPathTree;

import java.nio.file.Path;
import java.nio.file.Paths;


// Experimental !!!!

/**
 * Folder layout for a project output.
 */
public class JkProjectOutLayout {

    public static JkProjectOutLayout ofClassicJava() {
        final Path outputDir = Paths.get("");
        final Path generatedSourceDir = Paths.get( "generated-sources/java");
        final Path generatedResourceDir = Paths.get("generated-resources");
        final Path classDir = Paths.get( "classes");
        final Path generatedTestResourceDir = Paths.get( "generated-test-resources");
        final Path testClassDir = Paths.get( "test-classes");
        final Path testReportDir = Paths.get( "test-reports");
        final Path javadocDir = Paths.get( "javadoc");
        return new JkProjectOutLayout(outputDir, generatedSourceDir, generatedResourceDir,
                generatedTestResourceDir, classDir, testClassDir, testReportDir, javadocDir);
    }

    private final Path outputDir;

    private final Path generatedSourceDir;

    /**
     * Returns location of generated resources.
     */
    private final Path generatedResourceDir;

    private final Path generatedTestResourceDir;

    /**
     * Returns location where the java production classes are compiled.
     */
    private final Path classDir;

    /**
     * Returns location where the java production classes are compiled.
     */
    private final Path testClassDir;

    /**
     * Returns location where the test reports are written.
     */
    private final Path testReportDir;

    private final Path javadocDir;

    private JkProjectOutLayout(Path outputDir, Path generatedSourceDir, Path generatedResourceDir,
                               Path generatedTestResourceDir, Path classDir, Path testClassDir, Path testReportDir,
                               Path javadocDir) {
        super();
        this.outputDir = outputDir;
        this.generatedSourceDir = generatedSourceDir;
        this.generatedResourceDir = generatedResourceDir;
        this.generatedTestResourceDir = generatedTestResourceDir;
        this.classDir = classDir;
        this.testClassDir = testClassDir;
        this.testReportDir = testReportDir;
        this.javadocDir = javadocDir;
    }

    public JkProjectOutLayout withOutputDir(String newOutputDirPath) {
        return withOutputDir(Paths.get(newOutputDirPath));
    }

    public JkProjectOutLayout withOutputDir(Path newOutputDir) {
        return new JkProjectOutLayout(newOutputDir, generatedSourceDir, generatedResourceDir, generatedTestResourceDir,
                classDir, testClassDir, testReportDir, javadocDir);
    }

    public JkProjectOutLayout withGeneratedSourceDir(String path) {
        return new JkProjectOutLayout(this.outputDir, Paths.get(path), this.generatedResourceDir, this.generatedTestResourceDir,
                this.classDir, this.testClassDir, this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayout withGeneratedResourceDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, Paths.get( path), this.generatedTestResourceDir,
                this.classDir, this.testClassDir, this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayout withGeneratedTestResourceDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, Paths.get( path),
                this.classDir, this.testClassDir, this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayout withClassDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                Paths.get( path), this.testClassDir, this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayout withTestClassDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                this.classDir, Paths.get( path), this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayout withTestReportDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                this.classDir, this.testClassDir, Paths.get( path), this.javadocDir);
    }

    /**
     * Delete dirs all directories involved in output production (classes, test classes, test reports, generated sources/resources)
     * but not the outputDir.
     */
    public void deleteDirs() {
        JkPathTree.of(this.classDir).deleteContent();
        JkPathTree.of(this.testClassDir).deleteContent();
        JkPathTree.of(this.testReportDir).deleteContent();
        JkPathTree.of(this.generatedResourceDir).deleteContent();
        JkPathTree.of(this.generatedSourceDir).deleteContent();
        JkPathTree.of(this.generatedTestResourceDir).deleteContent();
    }



    // --------------------------- Views ---------------------------------


    public final Path getOutputPath() {
        return outputDir;
    }

    public Path getOutputPath(String relativePath) {
        return outputDir.resolve(relativePath);
    }

    public Path getClassDir() {
        return outputDir.resolve(classDir);
    }

    public Path getTestReportDir() {
        return outputDir.resolve(testReportDir);
    }

    public Path getTestClassDir() {
        return outputDir.resolve(testClassDir);
    }

    /**
     * Returns location of generated sources.
     */
    public Path getGeneratedSourceDir() {
        return outputDir.resolve(generatedSourceDir);
    }

    /**
     * Returns location of generated resources.
     */
    public Path getGeneratedResourceDir() {
        return outputDir.resolve(generatedResourceDir);
    }

    /**
     * Returns location of generated resources for tests.
     */
    public Path getGeneratedTestResourceDir() {
        return outputDir.resolve(generatedTestResourceDir);
    }

    public Path getJavadocDir() {
        return outputDir.resolve(javadocDir);
    }

}