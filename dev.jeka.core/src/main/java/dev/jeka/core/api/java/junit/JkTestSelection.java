package dev.jeka.core.api.java.junit;

import dev.jeka.core.api.file.JkPathSequence;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

/**
 * Mutable object to specify a set of test to run according class root dirs, file patterns and tags.
 */
public final class JkTestSelection implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STANDARD_INCLUDE_PATTERN = "^(Test.*|.+[.$]Test.*|.*Tests?)$";

    private JkPathSequence testClassRoots;

    private final Set<String> includePatterns = new LinkedHashSet<>();

    private final Set<String> excludePatterns = new LinkedHashSet<>();

    private final Set<String> includeTags = new LinkedHashSet<>();

    private final Set<String> excludeTags = new LinkedHashSet<>();

    private JkTestSelection(JkPathSequence testClassRoots) {
        this.testClassRoots = testClassRoots;
    }

    /**
     * Creates a <i>testSet</t> with the specified test class dir roots.  <p>
     * The created instance does not include any include filter so no test will be included out of the box.
     */
    public static JkTestSelection of(JkPathSequence testClassRoots) {
        return new JkTestSelection(testClassRoots);
    }

    /**
     * @see #of(JkPathSequence)
     */
    public static JkTestSelection of(Path ... paths) {
        return of(JkPathSequence.of(Arrays.asList(paths)));
    }

    /**
     * Same as {@link #of(JkPathSequence)} but already including the standard include pattern.
     */
    public static JkTestSelection ofStandard(JkPathSequence testClassRoots) {
        JkTestSelection result =  JkTestSelection.of(testClassRoots);
        result.includePatterns.add(STANDARD_INCLUDE_PATTERN);
        return result;
    }

    /**
     * @see #ofStandard(JkPathSequence)
     */
    public static JkTestSelection ofStandard(Path ... paths) {
        return JkTestSelection.ofStandard(JkPathSequence.of(Arrays.asList(paths)));
    }

    /**
     * Returns the test class rot dirs to discover the tests from.
     */
    public JkPathSequence getTestClassRoots() {
        return testClassRoots;
    }

    /**
     * Returns a modifiable set of include classname patterns (ex : ".*IT")
     */
    public Set<String> getIncludePatterns() {
        return includePatterns;
    }

    /**
     * Returns a modifiable set of exclude classname patterns.
     * @see #includePatterns
     */
    public Set<String> getExcludePatterns() {
        return excludePatterns;
    }

    public Set<String> getIncludeTags() {
        return includeTags;
    }

    public Set<String> getExcludeTags() {
        return excludeTags;
    }

}
