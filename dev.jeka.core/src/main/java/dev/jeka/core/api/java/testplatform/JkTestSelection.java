package dev.jeka.core.api.java.testplatform;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.function.JkUnaryOperator;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

/**
 * Mutable object to specify a set of test to run according class root dirs, file patterns and tags.
 */
public final class JkTestSelection implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STANDARD_INCLUDE_PATTERN = "^(Test.*|.+[.$]Test.*|.*Tests?)$";

    public static final String IT_INCLUDE_PATTERN = ".*IT";

    private JkPathSequence testClassRoots;

    private Set<String> includePatterns = new LinkedHashSet<>();

    private Set<String> excludePatterns = new LinkedHashSet<>();

    private Set<String> includeTags = new LinkedHashSet<>();

    private Set<String> excludeTags = new LinkedHashSet<>();

    private JkUnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer;

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
     * @see #setDiscoveryConfigurer(JkUnaryOperator)
     */
    public static JkTestSelection of(JkUnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer)  {
        JkTestSelection result = JkTestSelection.of();
        result.discoveryConfigurer = discoveryConfigurer;
        return result;
    }


    /**
     * Returns the test class rot dirs to discover the tests from.
     */
    public JkPathSequence getTestClassRoots() {
        return testClassRoots;
    }

    public JkTestSelection addTestClassRoots(Iterable<Path> paths) {
        List<Path> pathList = JkUtilsPath.disambiguate(paths);
        testClassRoots = testClassRoots.and(pathList);
        return this;
    }

    /**
     * Returns an unmodifiable set of include classname patterns (ex : ".*IT")
     */
    public Set<String> getIncludePatterns() {
        return Collections.unmodifiableSet(includePatterns);
    }

    public JkTestSelection addIncludePatterns(Iterable<String> patterns) {
        JkUtilsIterable.addAllWithoutDuplicate(this.includePatterns, patterns);
        return this;
    }

    public JkTestSelection addIncludePatterns(String ...patterns) {
        return addIncludePatterns(Arrays.asList(patterns));
    }

    public JkTestSelection addIncludePatternsIf(boolean condition, String ...patterns) {
        if (!condition) {
            return this;
        }
        return addIncludePatterns(Arrays.asList(patterns));
    }

    /**
     * Returns an unmodifiable set of exclude classname patterns.
     */
    public Set<String> getExcludePatterns() {
        return Collections.unmodifiableSet(excludePatterns);
    }

    public JkTestSelection addExcludePatterns(Iterable<String> patterns) {
        JkUtilsIterable.addAllWithoutDuplicate(this.excludePatterns, patterns);
        return this;
    }

    public JkTestSelection addExcludePatterns(String ...patterns) {
        return addExcludePatterns(Arrays.asList(patterns));
    }

    public Set<String> getIncludeTags() {
        return Collections.unmodifiableSet(includeTags);
    }

    public JkTestSelection addIncludeTags(Iterable<String> patterns) {
        JkUtilsIterable.addAllWithoutDuplicate(this.includeTags, patterns);
        return this;
    }

    public JkTestSelection addIncludeTags(String ...patterns) {
        return addIncludeTags(Arrays.asList(patterns));
    }

    public Set<String> getExcludeTags() {
        return excludeTags;
    }

    public JkTestSelection addExcludeTags(Iterable<String> patterns) {
        JkUtilsIterable.addAllWithoutDuplicate(this.excludeTags, patterns);
        return this;
    }

    public JkTestSelection addExcludeTags(String ...patterns) {
        return addExcludeTags(Arrays.asList(patterns));
    }

    public JkUnaryOperator<LauncherDiscoveryRequestBuilder> getDiscoveryConfigurer() {
        return discoveryConfigurer;
    }

    /**
     * Set a native Junit-platform configurer to build the {@link org.junit.platform.launcher.LauncherDiscoveryRequest}
     * passed to Junit-platform. The configurer will apply on a builder instance created from
     * patterns, tags and class root dirs defined in this instance.
     * <pre>
     *     <code>
     *         setDiscoveryConfigurer(classpath, builder -> builder
     *                 .filters(
     *                         ClassNameFilter.includeClassNamePatterns(ClassNameFilter.STANDARD_INCLUDE_PATTERN)
     *                 )
     *                 .selectors(DiscoverySelectors.selectMethod("mytest.MyTest#toto"))
     *                 .configurationParameter("run.it", "false")
     *         );
     *     </code>
     * </pre>
     */
    public void setDiscoveryConfigurer(JkUnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer) {
        this.discoveryConfigurer = discoveryConfigurer;
    }
}
