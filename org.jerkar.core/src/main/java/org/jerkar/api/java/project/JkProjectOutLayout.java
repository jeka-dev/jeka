package org.jerkar.api.java.project;

import java.io.File;

import org.jerkar.api.utils.JkUtilsFile;


@Deprecated // Experimental !!!!
public class JkProjectOutLayout {

    public static JkProjectOutLayout classic() {
        final File outputDir = new File(".");
        final File generatedSourceDir = new File(outputDir, "generated-sources/java");
        final File generatedResourceDir = new File(outputDir, "generated-resources");
        final File classDir = new File(outputDir, "classes");
        final File generatedTestResourceDir = new File(outputDir, "generated-test-resources");
        final File testClassDir = new File(outputDir, "test-classes");
        final File testReportDir = new File(outputDir, "test-reports");
        return new JkProjectOutLayout(outputDir, generatedSourceDir, generatedResourceDir,
                generatedTestResourceDir, classDir, testClassDir, testReportDir);
    }


    private final File outputDir;

    private final File generatedSourceDir;

    /**
     * Returns location of generated resources.
     */
    private final File generatedResourceDir;

    private final File generatedTestResourceDir;

    /**
     * Returns location where the java production classes are compiled.
     */
    private final File classDir;

    /**
     * Returns location where the java production classes are compiled.
     */
    private final File testClassDir;

    /**
     * Returns location where the test reports are written.
     */
    private final File testReportDir;

    private JkProjectOutLayout(File outputDir, File generatedSourceDir, File generatedResourceDir,
            File generatedTestResourceDir, File classDir, File testClassDir, File testReportDir) {
        super();
        this.outputDir = outputDir;
        this.generatedSourceDir = generatedSourceDir;
        this.generatedResourceDir = generatedResourceDir;
        this.generatedTestResourceDir = generatedTestResourceDir;
        this.classDir = classDir;
        this.testClassDir = testClassDir;
        this.testReportDir = testReportDir;
    }

    public JkProjectOutLayout withOutputBaseDir(File newOutputDir) {
        final File originalOut = this.outputDir;
        final File outputDir = newOutputDir;
        final File classDir = move(this.classDir, originalOut, newOutputDir);
        final File testClassDir = move(this.testClassDir, originalOut, newOutputDir);
        final File testReportDir = move(this.testReportDir, originalOut, newOutputDir);
        final File generatedResourceDir = move(this.generatedResourceDir, originalOut, newOutputDir);
        final File generatedSourceDir = move(this.generatedSourceDir, originalOut, newOutputDir);
        final File generatedTestResourceDir = move(this.generatedTestResourceDir, originalOut, newOutputDir);
        return new JkProjectOutLayout(outputDir, generatedSourceDir, generatedResourceDir, generatedTestResourceDir, classDir, testClassDir, testReportDir);
    }

    public JkProjectOutLayout withGeneratedSourceDir(String path) {
        return new JkProjectOutLayout(this.outputDir, new File(outputDir, path), this.generatedResourceDir, this.generatedTestResourceDir,
                this.classDir, this.testClassDir, this.testReportDir);
    }

    public JkProjectOutLayout withGeneratedResourceDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, new File(this.outputDir, path), this.generatedTestResourceDir,
                this.classDir, this.testClassDir, this.testReportDir);
    }

    public JkProjectOutLayout withGeneratedTestResourceDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, new File(this.outputDir, path),
                this.classDir, this.testClassDir, this.testReportDir);
    }

    public JkProjectOutLayout withClassDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                new File(this.outputDir, path), this.testClassDir, this.testReportDir);
    }

    public JkProjectOutLayout withTestClassDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                this.classDir, new File(this.outputDir, path), this.testReportDir);
    }

    public JkProjectOutLayout withTestReportDir(String path) {
        return new JkProjectOutLayout(this.outputDir, this.generatedSourceDir, this.generatedResourceDir, this.generatedResourceDir,
                this.classDir, this.testClassDir, new File(this.outputDir, path));
    }

    /**
     * Delete dirs all directories involved in outpout production (classes, test classes, test reports, generated sources/resources)
     * but not the outputDir.
     */
    public void deleteDirs() {
        JkUtilsFile.deleteIfExist(this.classDir);
        JkUtilsFile.deleteIfExist(this.testClassDir);
        JkUtilsFile.deleteIfExist(this.testReportDir);
        JkUtilsFile.deleteIfExist(this.generatedResourceDir);
        JkUtilsFile.deleteIfExist(this.generatedSourceDir);
        JkUtilsFile.deleteIfExist(this.generatedTestResourceDir);
    }

    private static File move(File original, File originalBaseDir, File newBaseDir) {
        if (!JkUtilsFile.isAncestor(originalBaseDir, original)) {
            return original;
        }
        final String relPath = JkUtilsFile.getRelativePath(originalBaseDir, original);
        return new File(newBaseDir, relPath);
    }




    // --------------------------- Views ---------------------------------



    public final File outputDir() {
        return outputDir;
    }

    public File classDir() {
        return classDir;
    }

    public File testReportDir() {
        return testReportDir;
    }

    public File testClassDir() {
        return testClassDir;
    }

    /**
     * Returns location of generated sources.
     */
    public File generatedSourceDir() {
        return generatedSourceDir;
    }

    /**
     * Returns location of generated resources.
     */
    public File generatedResourceDir() {
        return generatedResourceDir;
    }

    /**
     * Returns location of generated resources for tests.
     */
    public File generatedTestResourceDir() {
        return generatedTestResourceDir;
    }

}
