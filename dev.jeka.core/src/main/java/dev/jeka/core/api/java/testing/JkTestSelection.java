package dev.jeka.core.api.java.testing;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.function.JkUnaryOperator;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Mutable object to specify a set of test to run according class root dirs, file patterns and tags.<p>
 *
 * By default, when no include/exclude pattern/tag are specified, the selector get all classes
 * under defined class root dirs.
 */
public final class JkTestSelection<T> implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STANDARD_INCLUDE_PATTERN = "^(Test.*|.+[.$]Test.*|.*Tests?)$";

    public static final String IT_INCLUDE_PATTERN = ".*IT";

    private JkPathSequence testClassRoots = JkPathSequence.of();

    private Set<String> includePatterns = new LinkedHashSet<>();

    private Set<String> excludePatterns = new LinkedHashSet<>();

    private Set<String> includeTags = new LinkedHashSet<>();

    private Set<String> excludeTags = new LinkedHashSet<>();

    private JkUnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer;

    /**
     * For parent chaining
     */
    public final transient T __;

    private JkTestSelection(T __) {
        this.__ = __;
    }

    /**
     * Creates an empty <i>testSet</t>
     * The created instance does not include any include filter nor class root dirs
     * so no test will be included out of the box.
     */
    public static JkTestSelection<Void> of() {
        return ofParent(null);
    }

    /**
     * Same as {@link #of()} but mentioning a parent for chaining
     */
    public static <T> JkTestSelection<T> ofParent(T parent) {
        return new JkTestSelection(parent);
    }

    /**
     * Returns the test class rot dirs to discover the tests from.
     */
    public JkPathSequence getTestClassRoots() {
        return testClassRoots;
    }

    /**
     * Adds specified dir to the test class root directories. It can be an- collection of path or
     * a single path (as Path implements Iterable<Path>
     */
    public JkTestSelection<T> addTestClassRoots(Iterable<Path> paths) {
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

    public JkTestSelection<T> addIncludePatterns(Iterable<String> patterns) {
        JkUtilsIterable.addAllWithoutDuplicate(this.includePatterns, patterns);
        return this;
    }

    public JkTestSelection<T> addIncludeStandardPatterns() {
        return addExcludePatterns(STANDARD_INCLUDE_PATTERN);
    }

    public JkTestSelection<T> addIncludePatterns(String ...patterns) {
        return addIncludePatterns(Arrays.asList(patterns));
    }

    public JkTestSelection<T> addIncludePatternsIf(boolean condition, String ...patterns) {
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

    public JkTestSelection<T> addExcludePatterns(Iterable<String> patterns) {
        JkUtilsIterable.addAllWithoutDuplicate(this.excludePatterns, patterns);
        return this;
    }

    public JkTestSelection<T> addExcludePatterns(String ...patterns) {
        return addExcludePatterns(Arrays.asList(patterns));
    }

    public Set<String> getIncludeTags() {
        return Collections.unmodifiableSet(includeTags);
    }

    public JkTestSelection<T> addIncludeTags(Iterable<String> patterns) {
        JkUtilsIterable.addAllWithoutDuplicate(this.includeTags, patterns);
        return this;
    }

    public JkTestSelection<T> addIncludeTags(String ...patterns) {
        return addIncludeTags(Arrays.asList(patterns));
    }

    public Set<String> getExcludeTags() {
        return excludeTags;
    }

    public JkTestSelection<T> addExcludeTags(Iterable<String> patterns) {
        JkUtilsIterable.addAllWithoutDuplicate(this.excludeTags, patterns);
        return this;
    }

    public JkTestSelection<T> addExcludeTags(String ...patterns) {
        return addExcludeTags(Arrays.asList(patterns));
    }

    public JkUnaryOperator<LauncherDiscoveryRequestBuilder> getDiscoveryConfigurer() {
        return discoveryConfigurer;
    }

    public JkTestSelection<T> setTestClassRoots(UnaryOperator<JkPathSequence> pathSequencer) {
        testClassRoots = pathSequencer.apply(testClassRoots);
        return this;
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
    public JkTestSelection<T> setDiscoveryConfigurer(JkUnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer) {
        this.discoveryConfigurer = discoveryConfigurer;
        return this;
    }

    @Override
    public JkTestSelection clone() {
        try {
            JkTestSelection<T> result = (JkTestSelection<T>) super.clone();
            result.excludePatterns = new HashSet<>(excludePatterns);
            result.excludeTags = new HashSet<>(excludeTags);
            result.includePatterns = new HashSet<>(includePatterns);
            result.includeTags = new HashSet<>(includeTags);
            return result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "testClassRoots=" + testClassRoots.relativizedTo(Paths.get("").toAbsolutePath()) +
                ", includePatterns=" + includePatterns +
                ", excludePatterns=" + excludePatterns +
                ", includeTags=" + includeTags +
                ", excludeTags=" + excludeTags +
                ", discoveryConfigurer=" + discoveryConfigurer;
    }
}
