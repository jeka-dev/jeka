package org.jerkar.api.project;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsPath;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


// Experimental !!!!

/**
 * Folder layout for a project output.
 */
public class JkProjectOutLayoutPath {

    public static JkProjectOutLayoutPath classicJava() {
        final Path outputDir = Paths.get("");
        final Path generatedSourceDir = Paths.get( "generated-sources/java");
        final Path generatedResourceDir = Paths.get("generated-resources");
        final Path classDir = Paths.get( "classes");
        final Path generatedTestResourceDir = Paths.get( "generated-test-resources");
        final Path testClassDir = Paths.get( "test-classes");
        final Path testReportDir = Paths.get( "test-reports");
        final Path javadocDir = Paths.get( "javadoc");
        return new JkProjectOutLayoutPath(outputDir, generatedSourceDir, generatedResourceDir,
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

    private JkProjectOutLayoutPath(Path outputDir, Path generatedSourceDir, Path generatedResourceDir,
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

    public JkProjectOutLayoutPath withOutputDir(String newOutputDirPath) {
        return withOutputDir(Paths.get(newOutputDirPath));
    }

    public JkProjectOutLayoutPath withOutputDir(Path newOutputDir) {
        return new JkProjectOutLayoutPath(newOutputDir, generatedSourceDir, generatedResourceDir, generatedTestResourceDir,
                classDir, testClassDir, testReportDir, javadocDir);
    }

    public JkProjectOutLayoutPath withGeneratedSourceDir(String path) {
        return new JkProjectOutLayoutPath(this.outputDir, Paths.get(path), this.generatedResourceDir, this.generatedTestResourceDir,
                this.classDir, this.testClassDir, this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayoutPath withGeneratedResourceDir(String path) {
        return new JkProjectOutLayoutPath(this.outputDir, this.generatedSourceDir, Paths.get( path), this.generatedTestResourceDir,
                this.classDir, this.testClassDir, this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayoutPath withGeneratedTestResourceDir(String path) {
        return new JkProjectOutLayoutPath(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, Paths.get( path),
                this.classDir, this.testClassDir, this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayoutPath withClassDir(String path) {
        return new JkProjectOutLayoutPath(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                Paths.get( path), this.testClassDir, this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayoutPath withTestClassDir(String path) {
        return new JkProjectOutLayoutPath(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                this.classDir, Paths.get( path), this.testReportDir, this.javadocDir);
    }

    public JkProjectOutLayoutPath withTestReportDir(String path) {
        return new JkProjectOutLayoutPath(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                this.classDir, this.testClassDir, Paths.get( path), this.javadocDir);
    }

    /**
     * Delete dirs all directories involved in output production (classes, test classes, test reports, generated sources/resources)
     * but not the outputDir.
     */
    public void deleteDirs() {
        JkFileTree.of(this.classDir).deleteAll();
        JkFileTree.of(this.testClassDir).deleteAll();
        JkFileTree.of(this.testReportDir).deleteAll();
        JkFileTree.of(this.generatedResourceDir).deleteAll();
        JkFileTree.of(this.generatedSourceDir).deleteAll();
        JkFileTree.of(this.generatedTestResourceDir).deleteAll();
    }



    // --------------------------- Views ---------------------------------


    public final Path outputPath() {
        return outputDir;
    }

    public Path outputFile(String relativePath) {
        return outputDir.resolve(relativePath);
    }

    public Path classDir() {
        return outputDir.resolve(classDir());
    }

    public Path testReportDir() {
        return outputDir.resolve(testReportDir);
    }

    public Path testClassDir() {
        return outputDir.resolve(testClassDir);
    }

    /**
     * Returns location of generated sources.
     */
    public Path generatedSourceDir() {
        return outputDir.resolve(generatedSourceDir);
    }

    /**
     * Returns location of generated resources.
     */
    public Path generatedResourceDir() {
        return outputDir.resolve(generatedResourceDir);
    }

    /**
     * Returns location of generated resources for tests.
     */
    public Path generatedTestResourceDir() {
        return outputDir.resolve(generatedTestResourceDir);
    }

    public Path getJavadocDir() {
        return outputDir.resolve(javadocDir);
    }

}
