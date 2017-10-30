package org.jerkar.api.project;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.file.JkPathMatcher;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

/**
 * Describes a project layout about the source parts. Generated sources/resources are not
 * considered as source part since it is generated (so part ofMany the output).
 */
public class JkProjectSourceLayout {

    /**
     * Filter to consider as resources everything but java source stuff.
     */
    public static final PathMatcher JAVA_RESOURCE_MATCHER = JkPathMatcher.refuse("**/*.java", "*.java")
            .andRefuse("**/package.html", "package.html").andRefuse("**/doc-files", "doc-files");

    /**
     * Creates a Java project source structure according Maven conventions. It differs from Maven in that
     * non-java files located under src/main/java and src/test/java are also considered as resources.
     */
    public static JkProjectSourceLayout mavenJava() {
        final Path baseDir = Paths.get(".");
        final JkPathTreeSet sources = JkPathTreeSet.of(baseDir.resolve("src/main/java"));
        final JkPathTreeSet resources = JkPathTreeSet.of(baseDir.resolve("src/main/resources"))
                .and(sources.andMatcher(JAVA_RESOURCE_MATCHER));
        final JkPathTreeSet tests = JkPathTreeSet.of(baseDir.resolve("src/test/java"));
        final JkPathTreeSet testResources = JkPathTreeSet.of(baseDir.resolve("src/test/resources")).and(tests.andMatcher(JAVA_RESOURCE_MATCHER));
        return new JkProjectSourceLayout(baseDir, sources, resources, tests, testResources);
    }

    /**
     * Creates a simple Java project structure. Production sources and resources are located under src. Test sources
     * and resources are located in test.
     */
    public static JkProjectSourceLayout simple() {
        final Path baseDir = Paths.get(".");
        final JkPathTreeSet sources = JkPathTreeSet.of(baseDir.resolve("src"));
        final JkPathTreeSet resources = sources.andMatcher(JAVA_RESOURCE_MATCHER);
        final JkPathTreeSet tests = JkPathTreeSet.of(baseDir.resolve("test"));
        final JkPathTreeSet testResources = tests.andMatcher(JAVA_RESOURCE_MATCHER);
        return new JkProjectSourceLayout(baseDir, sources, resources, tests, testResources);
    }


    private final Path baseDir;

    /**
     * Returns the location ofMany production source code that has been edited
     * manually (not generated).
     */
    private final JkPathTreeSet sources;

    /**
     * Returns the location ofMany unit test source code that has been edited
     * manually (not generated).
     */
    private final JkPathTreeSet tests;

    /**
     * Returns the location ofMany production resources that has been edited
     * manually (not generated).
     */
    private final JkPathTreeSet resources;

    /**
     * Returns location ofMany edited resources for tests.
     */
    private final JkPathTreeSet testResources;

    private JkProjectSourceLayout(Path baseDir, JkPathTreeSet sources, JkPathTreeSet resources,
                                  JkPathTreeSet tests, JkPathTreeSet testResources) {
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
    public JkProjectSourceLayout withBaseDir(Path newBaseDir) {
        return new JkProjectSourceLayout(newBaseDir,
                sources, resources, tests, testResources);
    }

    public JkProjectSourceLayout withSources(JkPathTreeSet sources) {
        return new JkProjectSourceLayout(this.baseDir, sources, this.resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withSources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, baseTree().goTo(relativePath).asSet(), this.resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withResources(JkPathTreeSet resources) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withResources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, baseTree().goTo(relativePath).asSet(), this.tests, this.testResources);
    }

    public JkProjectSourceLayout withTests(JkPathTreeSet tests) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, tests, this.testResources);
    }

    public JkProjectSourceLayout withTests(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, baseTree().goTo(relativePath).asSet(), this.testResources);
    }

    public JkProjectSourceLayout withTestResources(JkPathTreeSet testResources) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, this.tests, testResources);
    }

    public JkProjectSourceLayout withTestResources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, this.tests, baseTree().goTo(relativePath).asSet());
    }


    // --------------------------- Views ---------------------------------

    /**
     * Returns location ofMany production source code (containing only edited sources, not generated sources).
     */
    public final JkPathTreeSet sources() {
        return sources.resolve(this.baseDir);
    }

    /**
     * Returns location ofMany production resources.
     */
    public final JkPathTreeSet resources() {
        return resources.resolve(this.baseDir);
    }

    /**
     * Returns location ofMany test source code (containing edited + generated
     * sources).
     */
    public final JkPathTreeSet tests() {
        return tests.resolve(this.baseDir);
    }

    /**
     * Returns location ofMany test resources.
     */
    public final JkPathTreeSet testResources() {
        return testResources.resolve(this.baseDir);
    }

    public Path baseDir() {
        return baseDir;
    }


    /**
     * Returns base directory as a {@link JkPathTree}.
     */
    public JkPathTree baseTree() {
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

    @Override
    public int hashCode() {
        int result = baseDir.hashCode();
        result = 31 * result + sources.hashCode();
        result = 31 * result + tests.hashCode();
        result = 31 * result + resources.hashCode();
        result = 31 * result + testResources.hashCode();
        return result;
    }


}
