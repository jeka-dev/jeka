package org.jerkar.api.java.project;

import java.io.File;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.utils.JkUtilsFile;


@Deprecated // Experimental !!!!
public class JkJavaProjectStructure implements Cloneable {

    public static final JkPathFilter RESOURCE_FILTER = JkPathFilter.exclude("**/*.java")
            .andExclude("**/package.html").andExclude("**/doc-files");

    /**
     * Creates a classic Java project structure according specified base and output directory.
     * All others locations are inferred from these two values.
     */
    public static JkJavaProjectStructure classic(File baseDir, File outputDir) {
        return new JkJavaProjectStructure(baseDir, outputDir);
    }

    /**
     * Creates a classic Java project structure according specified base and output related path to the base directory.
     * All others locations are inferred from these two values.
     */
    public static JkJavaProjectStructure classic(File baseDir, String outputDirRelatedPath) {
        return classic(baseDir, new File(baseDir, outputDirRelatedPath));
    }

    /**
     * Creates a classic Java project structure according specified base directory, the output directory relative path to
     * base directory is 'build/output".
     * All others locations are inferred from this value.
     */
    public static JkJavaProjectStructure classic(File baseDir) {
        return classic(baseDir, "build/output");
    }

    private File baseDir;

    private File outputDir;

    /**
     * Returns the location of production source code that has been edited
     * manually (not generated).
     */
    private JkFileTreeSet editedSources;

    /**
     * Returns the location of unit test source code that has been edited
     * manually (not generated).
     */
    private JkFileTreeSet testSources;

    /**
     * Returns the location of production resources that has been edited
     * manually (not generated).
     */
    private JkFileTreeSet editedResources;

    private File generatedSourceDir;

    /**
     * Returns location of generated resources.
     */
    private File generatedResourceDir;

    private File generatedTestResourceDir;

    /**
     * Returns location of edited resources for tests.
     */
    private JkFileTreeSet editedTestResources;

    /**
     * Returns location where the java production classes are compiled.
     */
    private File classDir;

    /**
     * Returns location where the test reports are written.
     */
    private File testReportDir;

    /**
     * Returns location where the java production classes are compiled.
     */
    private File testClassDir;


    private JkJavaProjectStructure(File baseDir, File outputDir) {

        this.baseDir = baseDir;
        this.outputDir = outputDir;

        this.editedSources = JkFileTreeSet.of(new File(baseDir,"src/main/java"));
        this.editedResources= JkFileTreeSet.of(new File(baseDir, "src/main/resources"));
        this.generatedSourceDir = new File(outputDir, "generated-sources/java");
        this.generatedResourceDir = new File(outputDir, "generated-resources");
        this.classDir = new File(outputDir, "classes");

        this.testSources = JkFileTreeSet.of(new File(baseDir,"src/test/java"));
        this.editedTestResources = JkFileTreeSet.of(new File(baseDir,"src/test/resources"));
        this.generatedTestResourceDir = new File(outputDir, "generated-unitTest-resources");
        this.testClassDir = new File(outputDir, "test-classes");
        this.testReportDir = new File(outputDir, "test-reports");
    }

    /**
     * Re-localise all locations defined under the base directory to the specified new base directory keeping the same relative path.
     */
    public JkJavaProjectStructure relocaliseBaseDir(File newBaseDir) {
        final File originalBase = this.baseDir;
        final File newOutputDir = move(outputDir, originalBase, newBaseDir);
        this.baseDir = newBaseDir;
        this.editedResources = move(editedResources, originalBase, newBaseDir);
        this.editedSources = move(editedSources, originalBase, newBaseDir);
        this.editedTestResources = move(editedTestResources, originalBase, newBaseDir);
        this.testSources = move(editedTestResources, originalBase, newBaseDir);
        relocaliseOutputDir(newOutputDir);
        return this;
    }

    /**
     * Re-localise output locations defined under the output directory keeping the same relative path.
     */
    public JkJavaProjectStructure relocaliseOutputDir(File newOutputDir) {
        final File originalOut = this.outputDir;
        this.outputDir = newOutputDir;
        this.classDir = move(classDir, originalOut, newOutputDir);
        this.testClassDir = move(testClassDir, originalOut, newOutputDir);
        this.testReportDir = move(testReportDir, originalOut, newOutputDir);
        this.generatedResourceDir = move(generatedResourceDir, originalOut, newOutputDir);
        this.generatedSourceDir = move(generatedSourceDir, originalOut, newOutputDir);
        this.generatedTestResourceDir = move(generatedTestResourceDir, originalOut, newOutputDir);
        return this;
    }

    /**
     * Same as {@link #relocaliseOutputDir(String)} but expressed with relative path to base dir.
     */
    public JkJavaProjectStructure relocaliseOutputDir(String relativePath) {
        return relocaliseOutputDir(new File(this.baseDir(), relativePath));
    }

    /**
     * Delete dirs all directories involved in outpout production (classes, test classes, test reports, generated sources/resources)
     * but not the outputDir.
     */
    public void deleteOutputDirs() {
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

    private static JkFileTreeSet move(JkFileTreeSet original, File originalBase, File newBase) {
        JkFileTreeSet result = JkFileTreeSet.empty();
        for (final JkFileTree fileTree : original.fileTrees()) {
            if (!JkUtilsFile.isAncestor(originalBase, fileTree.root())) {
                result = result.and(fileTree);
            } else {
                final String relPath = JkUtilsFile.getRelativePath(originalBase, fileTree.root());
                final File root = new File(newBase, relPath);
                final JkFileTree movedTree = JkFileTree.of(root).andFilter(fileTree.filter());
                result = result.and(movedTree);
            }
        }
        return result;
    }


    // --------------------------- Views ---------------------------------

    /**
     * Returns location of production source code (containing edited + generated
     * sources).
     */
    public final JkFileTreeSet sources() {
        return editedSources.and(generatedSourceDir);
    }

    /**
     * Returns location of production resources.
     */
    public final JkFileTreeSet resources() {
        return sources().andFilter(RESOURCE_FILTER).and(editedResources)
                .and(generatedResourceDir);
    }

    /**
     * Returns location of test source code (containing edited + generated
     * sources).
     */
    public final JkFileTreeSet testSources() {
        return testSources;
    }

    /**
     * Returns location of test resources.
     */
    public final JkFileTreeSet testResources() {
        return testSources.andFilter(RESOURCE_FILTER).and(editedTestResources)
                .and(generatedTestResourceDir);
    }

    public final File outputDir() {
        return outputDir;
    }

    public File baseDir() {
        return baseDir;
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
    public File getGeneratedSourceDir() {
        return generatedSourceDir;
    }

    /**
     * Returns location of generated resources.
     */
    public File getGeneratedResourceDir() {
        return generatedResourceDir;
    }

    /**
     * Returns location of generated resources for tests.
     */
    public File getGeneratedTestResourceDir() {
        return generatedTestResourceDir;
    }

    // ------------------------ Setters --------------------------------------------

    /**
     * Sets location of production source code edited manually (not generated).
     */
    public JkJavaProjectStructure setEditedSources(JkFileTreeSet editedSources) {
        this.editedSources = editedSources;
        return this;
    }

    /**
     * @see #setEditedSources(String...)
     */
    public JkJavaProjectStructure setEditedSources(String ... relativePaths) {
        return this.setEditedSources(toTrees(relativePaths));
    }

    /**
     * Sets location of test source code.
     */
    public JkJavaProjectStructure setTestSources(JkFileTreeSet testSources) {
        this.testSources = testSources;
        return this;
    }

    /**
     * @see #setTestSources(JkFileTreeSet)
     */
    public JkJavaProjectStructure setTestSources(String ... relativePaths) {
        return this.setTestSources(toTrees(relativePaths));
    }

    /**
     * Sets location of production resources edited manually (not generated).
     */
    public JkJavaProjectStructure setEditedResources(JkFileTreeSet editedResources) {
        this.editedResources = editedResources;
        return this;
    }

    /**
     * @see #setEditedResources(JkFileTreeSet)
     */
    public JkJavaProjectStructure setEditedResources(String ... relativePaths) {
        return this.setEditedResources(toTrees(relativePaths));
    }

    /**
     * Sets location of edited resources for tests.
     */
    public JkJavaProjectStructure setEditedTestResources(JkFileTreeSet editedTestResources) {
        this.editedTestResources = editedTestResources;
        return this;
    }

    /**
     * @see #setEditedTestResources(String...)
     */
    public JkJavaProjectStructure setEditedTestResources(String ... relativePaths) {
        return this.setEditedTestResources(toTrees(relativePaths));
    }

    /*
     * Sets location of generated sources.
     */
    public JkJavaProjectStructure setGeneratedSourceDir(File generatedSourceDir) {
        this.generatedSourceDir = generatedSourceDir;
        return this;
    }

    /**
     * Sets location of generated resources.
     */
    public JkJavaProjectStructure setGeneratedResourceDir(File generatedResourceDir) {
        this.generatedResourceDir = generatedResourceDir;
        return this;
    }

    /**
     * Sets location of generated resources for tests.
     */
    public JkJavaProjectStructure setGeneratedTestResourceDir(File generatedTestResourceDir) {
        this.generatedTestResourceDir = generatedTestResourceDir;
        return this;
    }

    /**
     * Returns location where the java production classes are compiled.
     */
    public JkJavaProjectStructure setClassDir(File classDir) {
        this.classDir = classDir;
        return this;
    }

    /**
     * Returns location where the java production classes are compiled related to the output directory.
     */
    public JkJavaProjectStructure setClassDir(String relativePath) {
        return this.setClassDir(new File(outputDir, relativePath));
    }

    /**
     * Sets location where the test reports are written.
     */
    public JkJavaProjectStructure setTestReportDir(File testReportDir) {
        this.testReportDir = testReportDir;
        return this;
    }

    /**
     * Sets location where the test reports are written related to the output directory.
     */
    public JkJavaProjectStructure setTestReportDir(String relativePath) {
        return this.setTestReportDir(new File(outputDir, relativePath));
    }

    /**
     * Sets location where the java production classes are compiled.
     */
    public JkJavaProjectStructure setTestClassDir(File testClassDir) {
        this.testClassDir = testClassDir;
        return this;
    }

    /**
     * Sets location where the java production classes are compiled related to the output directory
     */
    public JkJavaProjectStructure setTestClassDir(String relativePath) {
        return this.setTestClassDir(new File(outputDir, relativePath));
    }

    private JkFileTreeSet toTrees(String ... paths) {
        JkFileTreeSet result = JkFileTreeSet.empty();
        for (final String path : paths) {
            result = result.and(JkFileTree.of(new File(baseDir, path)));
        }
        return result;
    }

    @Override
    public JkJavaProjectStructure clone()  {
        try {
            return (JkJavaProjectStructure) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
