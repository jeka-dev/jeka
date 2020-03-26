package dev.jeka.core.api.java.junit;

import dev.jeka.core.api.file.JkPathSequence;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Specifies a set of test to run according class root dirs, file patterns and tags.
 */
public class JkUnit5TestSelection implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STANDARD_INCLUDE_PATTERN = "^(Test.*|.+[.$]Test.*|.*Tests?)$";

    private final JkPathSequence testClassRoots;

    private final Set<String> includePatterns;

    private final Set<String> excludePatterns;

    private final Set<String> includeTags;

    private final Set<String> excludeTags;

    private JkUnit5TestSelection(JkPathSequence testClassRoots,
                                 Set<String> includePatterns,
                                 Set<String> excludePatterns,
                                 Set<String> includeTags,
                                 Set<String> excludeTags) {
        this.testClassRoots = testClassRoots;
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;
        this.includeTags = includeTags;
        this.excludeTags = excludeTags;
    }

    /**
     * Creates a <i>testSet</t> with the specified test class dir roots.  <p>
     * The created instance does not include any include filter so no test will be included out of the box. Use
     * {@link #andIncludePatterns(String...)} to include tests.
     */
    public static JkUnit5TestSelection of(JkPathSequence testClassRoots) {
        return new JkUnit5TestSelection(testClassRoots, Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
    }

    /**
     * @see #of(JkPathSequence)
     */
    public static JkUnit5TestSelection of(Path ... paths) {
        return of(JkPathSequence.of(Arrays.asList(paths)));
    }

    /**
     * Same as {@link #of(JkPathSequence)} but already including the standard include pattern.
     */
    public static JkUnit5TestSelection ofStandard(JkPathSequence testClassRoots) {
        return JkUnit5TestSelection.of(testClassRoots).andIncludePatterns(STANDARD_INCLUDE_PATTERN);
    }

    /**
     * @see #ofStandard(JkPathSequence)
     */
    public static JkUnit5TestSelection ofStandard(Path ... paths) {
        return JkUnit5TestSelection.ofStandard(JkPathSequence.of(Arrays.asList(paths)));
    }

    public JkUnit5TestSelection andIncludePatterns(String... patterns) {
        Set<String> includePatterns = new HashSet<>(this.includePatterns);
        for (String pattern : patterns) {
            includePatterns.add(pattern);
        }
        return new JkUnit5TestSelection(testClassRoots, includePatterns,
                excludePatterns, includeTags, excludeTags);
    }

    public JkUnit5TestSelection andExcludePatterns(String... patterns) {
        Set<String> excludePatterns = new HashSet<>(this.excludePatterns);
        for (String pattern : patterns) {
            excludePatterns.add(pattern);
        }
        return new JkUnit5TestSelection(testClassRoots, includePatterns,
                excludePatterns, includeTags, excludeTags);
    }

    public JkUnit5TestSelection andIncludeTags(String... patterns) {
        Set<String> includeTags = new HashSet<>(this.includeTags);
        for (String pattern : patterns) {
            includeTags.add(pattern);
        }
        return new JkUnit5TestSelection(testClassRoots, includePatterns,
                excludePatterns, includeTags, excludeTags);
    }

    public JkUnit5TestSelection excludeIncludeTags(String... patterns) {
        Set<String> excludeTags = new HashSet<>(this.excludeTags);
        for (String pattern : patterns) {
            excludeTags.add(pattern);
        }
        return new JkUnit5TestSelection(testClassRoots, includePatterns,
                excludePatterns, includeTags, excludeTags);
    }

    public JkPathSequence getTestClassRoots() {
        return testClassRoots;
    }

    public Set<String> getIncludePatterns() {
        return includePatterns;
    }

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
