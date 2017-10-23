package org.jerkar.api.project;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
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
    public static final JkPathFilter JAVA_RESOURCE_FILTER = JkPathFilter.exclude("**/*.java")
            .andExclude("**/package.html").andExclude("**/doc-files");

    /**
     * Filter to consider as resources everything but java source stuff.
     */
    public static final PathMatcher JAVA_RESOURCE_MATCHER = JkPathMatcher.notIn("**/*.java")
            .andNot("**/package.html").andNot("**/doc-files");

    /**
     * Creates a Java project source structure according Maven conventions. It differs from Maven in that
     * non-java files located under src/main/java and src/test/java are also considered as resources.
     */
    public static JkProjectSourceLayout mavenJava() {
        final Path baseDir = Paths.get(".");
        final JkFileTreeSet sources = JkFileTreeSet.of(baseDir.resolve("src/main/java"));
        final JkFileTreeSet resources = JkFileTreeSet.of(baseDir.resolve("src/main/resources"))
                .and(sources.andFilter(JAVA_RESOURCE_MATCHER));
        final JkFileTreeSet tests = JkFileTreeSet.of(baseDir.resolve("src/test/java"));
        final JkFileTreeSet testResources = JkFileTreeSet.of(baseDir.resolve("src/test/resources")).and(tests.andFilter(JAVA_RESOURCE_MATCHER));
        return new JkProjectSourceLayout(baseDir, sources, resources, tests, testResources);
    }

    /**
     * Creates a simple Java project structure. Production sources and resources are located under src. Test sources
     * and resources are located in test.
     */
    public static JkProjectSourceLayout simple() {
        final Path baseDir = Paths.get(".");
        final JkFileTreeSet sources = JkFileTreeSet.of(baseDir.resolve("src"));
        final JkFileTreeSet resources = sources.andFilter(JAVA_RESOURCE_MATCHER);
        final JkFileTreeSet tests = JkFileTreeSet.of(baseDir.resolve("test"));
        final JkFileTreeSet testResources = tests.andFilter(JAVA_RESOURCE_MATCHER);
        return new JkProjectSourceLayout(baseDir, sources, resources, tests, testResources);
    }


    private final Path baseDir;

    /**
     * Returns the location ofMany production source code that has been edited
     * manually (not generated).
     */
    private final JkFileTreeSet sources;

    /**
     * Returns the location ofMany unit test source code that has been edited
     * manually (not generated).
     */
    private final JkFileTreeSet tests;

    /**
     * Returns the location ofMany production resources that has been edited
     * manually (not generated).
     */
    private final JkFileTreeSet resources;

    /**
     * Returns location ofMany edited resources for tests.
     */
    private final JkFileTreeSet testResources;

    private JkProjectSourceLayout(Path baseDir, JkFileTreeSet sources, JkFileTreeSet resources,
                                  JkFileTreeSet tests, JkFileTreeSet testResources) {
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

    public JkProjectSourceLayout withSources(JkFileTreeSet sources) {
        return new JkProjectSourceLayout(this.baseDir, sources, this.resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withSources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, baseTree().go(relativePath).asSet(), this.resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withResources(JkFileTreeSet resources) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, resources, this.tests, this.testResources);
    }

    public JkProjectSourceLayout withResources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, baseTree().go(relativePath).asSet(), this.tests, this.testResources);
    }

    public JkProjectSourceLayout withTests(JkFileTreeSet tests) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, tests, this.testResources);
    }

    public JkProjectSourceLayout withTests(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, baseTree().go(relativePath).asSet(), this.testResources);
    }

    public JkProjectSourceLayout withTestResources(JkFileTreeSet testResources) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, this.tests, testResources);
    }

    public JkProjectSourceLayout withTestResources(String relativePath) {
        return new JkProjectSourceLayout(this.baseDir, this.sources, this.resources, this.tests, baseTree().go(relativePath).asSet());
    }


    // --------------------------- Views ---------------------------------

    /**
     * Returns location ofMany production source code (containing only edited sources, not generated sources).
     */
    public final JkFileTreeSet sources() {
        return sources.resolve(this.baseDir);
    }

    /**
     * Returns location ofMany production resources.
     */
    public final JkFileTreeSet resources() {
        return resources.resolve(this.baseDir);
    }

    /**
     * Returns location ofMany test source code (containing edited + generated
     * sources).
     */
    public final JkFileTreeSet tests() {
        return tests.resolve(this.baseDir);
    }

    /**
     * Returns location ofMany test resources.
     */
    public final JkFileTreeSet testResources() {
        return testResources.resolve(this.baseDir);
    }

    public Path baseDir() {
        return baseDir;
    }


    /**
     * Returns base directory as a {@link JkFileTree}.
     */
    public JkFileTree baseTree() {
        return JkFileTree.of(baseDir);
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
