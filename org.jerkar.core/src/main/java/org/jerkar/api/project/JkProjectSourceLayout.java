package org.jerkar.api.project;

import java.io.File;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.utils.JkUtilsFile;

@Deprecated // Experimental !!!!
public class JkProjectSourceLayout {

    public static final JkPathFilter RESOURCE_FILTER = JkPathFilter.exclude("**/*.java")
            .andExclude("**/package.html").andExclude("**/doc-files");


    /**
     * Creates a Java project source structure according Maven conventions. It differs from Maven in that
     * non-java files located under src/main/java and src/test/java are also considered as resources.
     */
    public static JkProjectSourceLayout mavenJava() {
        final File baseDir = new File(".");
        final JkFileTreeSet sources = JkFileTreeSet.of(new File(baseDir,"src/main/java"));
        final JkFileTreeSet resources = JkFileTreeSet.of(new File(baseDir, "src/main/resources")).and(sources.andFilter(RESOURCE_FILTER));
        final JkFileTreeSet tests = JkFileTreeSet.of(new File(baseDir,"src/test/java"));
        final JkFileTreeSet testResources = JkFileTreeSet.of(new File(baseDir, "src/test/resources")).and(tests.andFilter(RESOURCE_FILTER));
        return new JkProjectSourceLayout(baseDir, sources, resources, tests, testResources);
    }

    /**
     * Creates a simple Java project structure. Production sources and resources are located under src. Test sources
     * and resources are located in test.
     */
    public static JkProjectSourceLayout simple() {
        final File baseDir = new File(".");
        final JkFileTreeSet sources = JkFileTreeSet.of(new File(baseDir,"src"));
        final JkFileTreeSet resources = sources.andFilter(RESOURCE_FILTER);
        final JkFileTreeSet tests = JkFileTreeSet.of(new File(baseDir,"test"));
        final JkFileTreeSet testResources = tests.andFilter(RESOURCE_FILTER);
        return new JkProjectSourceLayout(baseDir, sources, resources, tests, testResources);
    }


    private final File baseDir;

    /**
     * Returns the location of production source code that has been edited
     * manually (not generated).
     */
    private final JkFileTreeSet sources;

    /**
     * Returns the location of unit test source code that has been edited
     * manually (not generated).
     */
    private final JkFileTreeSet tests;

    /**
     * Returns the location of production resources that has been edited
     * manually (not generated).
     */
    private final JkFileTreeSet resources;

    /**
     * Returns location of edited resources for tests.
     */
    private final JkFileTreeSet testResources;


    private JkProjectSourceLayout(File baseDir, JkFileTreeSet sources, JkFileTreeSet resources, JkFileTreeSet tests, JkFileTreeSet testResources) {
        super();
        this.baseDir = baseDir;
        this.sources = sources;
        this.tests = tests;
        this.resources = resources;
        this.testResources = testResources;
    }


    /**
     * Re-localise all locations defined under the base directory to the specified new base directory keeping the same relative path.
     */
    public JkProjectSourceLayout withBaseDir(File newBaseDir) {
        return new JkProjectSourceLayout(newBaseDir,
                move(sources, baseDir, newBaseDir),
                move(resources, baseDir, newBaseDir),
                move(tests, baseDir, newBaseDir),
                move(testResources, baseDir, newBaseDir));
    }

    public JkProjectSourceLayout withSources(JkFileTreeSet sources) {
        return new JkProjectSourceLayout(this.baseDir, sources, this.resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withSources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, root().go(relativePath).asSet(), this.resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withResources(JkFileTreeSet resources) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withResources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, root().go(relativePath).asSet(), this.tests, this.testResources);
    }

    public JkProjectSourceLayout withTests(JkFileTreeSet tests) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, tests, this.testResources);
    }

    public JkProjectSourceLayout withTests(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, root().go(relativePath).asSet(), this.testResources);
    }

    public JkProjectSourceLayout withTestResources(JkFileTreeSet testResources) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, this.tests, testResources);
    }

    public JkProjectSourceLayout withTestResources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, this.tests, root().go(relativePath).asSet());
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
        return sources;
    }

    /**
     * Returns location of production resources.
     */
    public final JkFileTreeSet resources() {
        return resources;
    }

    /**
     * Returns location of test source code (containing edited + generated
     * sources).
     */
    public final JkFileTreeSet tests() {
        return tests;
    }

    /**
     * Returns location of test resources.
     */
    public final JkFileTreeSet testResources() {
        return testResources;
    }

    public File baseDir() {
        return baseDir;
    }

    /**
     * Returns base directory as a {@link JkFileTree}.
     */
    public JkFileTree root() {
        return JkFileTree.of(baseDir);
    }

}
