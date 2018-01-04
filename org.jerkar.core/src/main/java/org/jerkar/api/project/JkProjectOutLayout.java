package org.jerkar.api.project;

import org.jerkar.api.file.JkPathTree;

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Folder layout for a project output.
 */
public class JkProjectOutLayout {

    public static JkProjectOutLayout classicJava() {
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

    private Path outputDir;

    private Path generatedSourceDir;

    /**
     * Returns location of generated resources.
     */
    private Path generatedResourceDir;

    private Path generatedTestResourceDir;

    /**
     * Returns location where the java production classes are compiled.
     */
    private Path classDir;

    /**
     * Returns location where the java production classes are compiled.
     */
    private Path testClassDir;

    /**
     * Returns location where the test reports are written.
     */
    private Path testReportDir;

    private Path javadocDir;

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

    public JkProjectOutLayout setOutputDir(String newOutputDirPath) {
        return setOutputDir(Paths.get(newOutputDirPath));
    }

    public JkProjectOutLayout setOutputDir(Path newOutputDir) {
        this.outputDir = newOutputDir;
        return this;
    }

    public JkProjectOutLayout setGeneratedSourceDir(String path) {
        return setGeneratedSourceDir(Paths.get(path));
    }

    public JkProjectOutLayout setGeneratedSourceDir(Path path) {
        this.generatedSourceDir = path;
        return this;
    }

    public JkProjectOutLayout setGeneratedResourceDir(Path path) {
        this.generatedResourceDir = path;
        return this;
    }

    public JkProjectOutLayout setGeneratedTestResourceDir(Path path) {
        this.generatedTestResourceDir = path;
        return this;
    }

    public JkProjectOutLayout setGeneratedTestResourceDir(String path) {
        return setGeneratedTestResourceDir(Paths.get(path));
    }

    public JkProjectOutLayout setClassDir(Path path) {
        this.classDir = path;
        return this;
    }

    public JkProjectOutLayout setClassDir(String path) {
        return this.setClassDir(Paths.get(path));
    }

    public JkProjectOutLayout setTestClassDir(Path path) {
        this.testClassDir = path;
        return this;
    }

    public JkProjectOutLayout setTestClassDir(String path) {
        return this.setTestClassDir(Paths.get(path));
    }

    public JkProjectOutLayout setTestReportDir(Path path) {
        this.testReportDir = path;
        return this;
    }

    public JkProjectOutLayout setTestReportDir(String path) {
        return this.setTestReportDir(Paths.get(path));
    }

    /**
     * Delete dirs all directories involved in output production (classes, test classes, test reports, generated sources/resources)
     * but not the outputDir.
     */
    public void deleteDirs() {
        JkPathTree.of(this.getClassDir()).deleteContent();
        JkPathTree.of(this.getTestClassDir()).deleteContent();
        JkPathTree.of(this.getTestReportDir()).deleteContent();
        JkPathTree.of(this.getGeneratedResourceDir()).deleteContent();
        JkPathTree.of(this.getGeneratedSourceDir()).deleteContent();
        JkPathTree.of(this.getGeneratedTestResourceDir()).deleteContent();
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
