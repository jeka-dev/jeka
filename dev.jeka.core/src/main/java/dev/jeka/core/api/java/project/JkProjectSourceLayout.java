package dev.jeka.core.api.java.project;

import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.tool.JkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Describes a project layout about the source parts. Generated sources/resources are not
 * considered as source part since it is generated (so part of the output).
 */
public class JkProjectSourceLayout {

    public static final String GENERATED_SOURCE_PATH = "generated_sources/java";

    public static final String GENERATED_RESOURCE_PATH = "generated_resources";

    /**
     * Filter to consider as resources everything but java source stuff.
     */
    public static final PathMatcher JAVA_RESOURCE_MATCHER = JkPathMatcher.of(false, "**/*.java", "*.java",
            "**/package.html", "package.html", "**/doc-files", "doc-files");

    /**
     * Creates a Java project source structure according Maven conventions. It differs from Maven in that
     * non-java files located under src/main/java and src/test/java are also considered as resources.
     */
    public static JkProjectSourceLayout ofMavenStyle() {
        final Path baseDir = Paths.get(".");
        JkPathTreeSet sources = JkPathTreeSet.of(baseDir.resolve("src/main/java").normalize());
        JkPathTreeSet resources = JkPathTreeSet.of(baseDir.resolve("src/main/resources").normalize())
                .and(sources.andMatcher(JAVA_RESOURCE_MATCHER));
        final JkPathTreeSet tests = JkPathTreeSet.of(baseDir.resolve("src/test/java").normalize());
        final JkPathTreeSet testResources = JkPathTreeSet.of(baseDir.resolve("src/test/resources").normalize())
                .and(tests.andMatcher(JAVA_RESOURCE_MATCHER));
        final Path generatedSources = baseDir.resolve(JkConstants.OUTPUT_PATH).resolve(GENERATED_SOURCE_PATH);
        if (Files.exists(generatedSources)) {
            sources = sources.and(generatedSources);
        }
        final Path generatedResources = baseDir.resolve(JkConstants.OUTPUT_PATH).resolve(GENERATED_RESOURCE_PATH);
        if (Files.exists(generatedResources)) {
            resources = resources.and(generatedSources);
        }
        return new JkProjectSourceLayout(baseDir, sources, resources, tests, testResources);
    }

    /**
     * Creates a simple Java project structure. Production sources and resources are located under src. Test sources
     * and resources are located in test.
     */
    public static JkProjectSourceLayout ofSimpleStyle() {
        final Path baseDir = Paths.get(".");
        final JkPathTreeSet sources = JkPathTreeSet.of(baseDir.resolve("src").normalize());
        final JkPathTreeSet resources = sources.andMatcher(JAVA_RESOURCE_MATCHER);
        final JkPathTreeSet tests = JkPathTreeSet.of(baseDir.resolve("test").normalize());
        final JkPathTreeSet testResources = tests.andMatcher(JAVA_RESOURCE_MATCHER);
        return new JkProjectSourceLayout(baseDir, sources, resources, tests, testResources);
    }

    private final Path baseDir;

    /**
     * Returns the location of production source code that has been edited
     * manually (not generated).
     */
    private final JkPathTreeSet sources;

    /**
     * Returns the location of unit test source code that has been edited
     * manually (not generated).
     */
    private final JkPathTreeSet tests;

    /**
     * Returns the location of production resources that has been edited
     * manually (not generated).
     */
    private final JkPathTreeSet resources;

    /**
     * Returns location of edited resources for tests.
     */
    private final JkPathTreeSet testResources;

    private JkProjectSourceLayout(Path baseDir, JkPathTreeSet sources, JkPathTreeSet resources,
            JkPathTreeSet tests, JkPathTreeSet testResources) {
        super();
        this.baseDir = baseDir.normalize().toAbsolutePath();
        this.sources = sources;
        this.tests = tests;
        this.resources = resources;
        this.testResources = testResources;
    }

    /**
     * Re-localise all locations defined under the base directory to the specified new of directory keeping the same relative path.
     */
    public JkProjectSourceLayout withBaseDir(Path newBaseDir) {
        final Path path = newBaseDir.toAbsolutePath().normalize();
        return new JkProjectSourceLayout(path,
                relocalize(path,sources), relocalize(path, resources),
                relocalize(path, tests), relocalize(path, testResources));
    }

    public JkProjectSourceLayout withSources(JkPathTreeSet sources) {
        return new JkProjectSourceLayout(this.baseDir, sources, this.resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withSources(String ... relativePath) {
        return new JkProjectSourceLayout(this.baseDir, toSet(relativePath), this.resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withResources(JkPathTreeSet resources) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withResources(String ... relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, toSet(relativePath), this.tests, this.testResources);
    }

    public JkProjectSourceLayout withTests(JkPathTreeSet tests) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, tests, this.testResources);
    }

    public JkProjectSourceLayout withTests(String ... relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, toSet(relativePath), this.testResources);
    }

    public JkProjectSourceLayout withTestResources(JkPathTreeSet testResources) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, this.tests, testResources);
    }

    public JkProjectSourceLayout withTestResources(String ... relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, this.tests, toSet(relativePath));
    }


    // --------------------------- Views ---------------------------------

    /**
     * Returns location of production source code (containing only edited sources, not generated sources).
     */
    public final JkPathTreeSet getSources() {
        return sources.resolve(this.baseDir);
    }

    /**
     * Returns location of production resources.
     */
    public final JkPathTreeSet getResources() {
        return resources.resolve(this.baseDir);
    }

    /**
     * Returns location of test source code (containing edited + generated
     * sources).
     */
    public final JkPathTreeSet getTests() {
        return tests.resolve(this.baseDir);
    }

    /**
     * Returns location of test resources.
     */
    public final JkPathTreeSet getTestResources() {
        return testResources.resolve(this.baseDir);
    }

    public Path getBaseDir() {
        return baseDir;
    }


    /**
     * Returns base directory as a {@link JkPathTree}.
     */
    public JkPathTree getBaseTree() {
        return JkPathTree.of(baseDir);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JkProjectSourceLayout that = (JkProjectSourceLayout) o;

        if (!baseDir.equals(that.baseDir)) {
            return false;
        }
        if (!sources.equals(that.sources)) {
            return false;
        }
        if (!tests.equals(that.tests)) {
            return false;
        }
        if (!resources.equals(that.resources)) {
            return false;
        }
        return testResources.equals(that.testResources);
    }

    public String getInfo() {
        return new StringBuffer("Sources : " + this.sources + "\n")
                .append("Resources : " + this.resources + "\n")
                .append("Tests : " + this.tests + "\n")
                .append("Tests resources : " + this.testResources)
                .toString();
    }

    @Override
    public int hashCode() {
        int result = baseDir.hashCode();
        result = 31 * result + sources.hashCode();
        result = 31 * result + tests.hashCode();
        result = 31 * result + resources.hashCode();
        result = 31 * result + testResources.hashCode();
        return result;
    }

    private JkPathTreeSet toSet(String ... relativePaths) {
        final List<JkPathTree> trees = new LinkedList<>();
        for (final String relativePath : relativePaths) {
            trees.add(getBaseTree().goTo(relativePath));
        }
        return JkPathTreeSet.of(trees);
    }

    private Path relocalize(Path newBase, Path path) {
        if (!path.isAbsolute()) {
            return newBase.resolve(path);
        }
        if (!path.startsWith(baseDir)) {
            return path;
        }
        final Path relPath = baseDir.relativize(path);
        return newBase.resolve(relPath);
    }

    private JkPathTreeSet relocalize(Path newBase, JkPathTreeSet pathTreeSet) {
        JkPathTreeSet result = JkPathTreeSet.ofEmpty();
        for (final JkPathTree tree : pathTreeSet.getPathTrees()) {
            result = result.and(JkPathTree.of(relocalize(newBase, tree.getRoot())).withMatcher(tree.getMatcher()));
        }
        return result;
    }



}
