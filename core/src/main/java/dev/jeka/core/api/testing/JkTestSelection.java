/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.testing;

import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.function.JkUnaryOperator;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

/**
 * Mutable object to specify a set of test to run according class root dirs, file patterns and tags.<p>
 *
 * By default, when no include/exclude pattern/tag are specified, the selector get all classes
 * under defined class root dirs.
 */
public final class JkTestSelection implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String MAVEN_INCLUDE_PATTERN = "^(Test.*|.+[.$]Test.*|.*Tests?)$";

    /**
     * A regular expression pattern for selecting integration test classes.
     * The pattern `.*IT` matches test class names ending with "IT".<p>
     * Integration tests are tests run within the regular test phase but
     * running slower than regular unit tests.
     */
    public static final String IT_PATTERN = ".*IT";

    /**
     * A regular expression that matches strings beginning with "e2e.".
     * <p>
     * This regular expression is commonly used to identify fully qualified
     * end-to-end (E2E) classes that belong to the <i>e2e</i> package.
     * <p>
     * End-to-end tests are tests running in <i>e2eTest</i> after the
     * application or library has been packaged.
     */
    public static final String E2E_PATTERN = "^e2e\\..*";

    private JkPathSequence testClassRoots = JkPathSequence.of();

    private Set<String> includePatterns = new LinkedHashSet<>();

    private Set<String> excludePatterns = new LinkedHashSet<>();

    private Set<String> includeTags = new LinkedHashSet<>();

    private Set<String> excludeTags = new LinkedHashSet<>();

    private JkUnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer;

    private JkTestSelection() {
    }

    private JkTestSelection(JkTestSelection other) {
        this.discoveryConfigurer = other.discoveryConfigurer;
        this.excludePatterns = new HashSet<>(other.excludePatterns);
        this.excludeTags = new HashSet<>(other.excludeTags);
        this.testClassRoots = other.testClassRoots;
        this.includePatterns = new HashSet<>(other.includePatterns);
        this.includeTags = new HashSet<>(other.includeTags);
    }

    /**
     * Creates an empty <i>testSet</t>
     * The created instance does not include any include filter nor class root dirs
     * so no test will be included out of the box.
     */
    public static JkTestSelection of() {
        return new JkTestSelection();
    }

    /**
     * Returns the test class rot dirs to discover the tests from.
     */
    public JkPathSequence getTestClassRoots() {
        return testClassRoots;
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

    public JkTestSelection addIncludeMavenPatterns() {
        return addIncludePatterns(MAVEN_INCLUDE_PATTERN);
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

    /**
     * Adds the specified exclude patterns to the current test selection.
     * Exclude patterns are regular expressions used to filter test class names.
     */
    public JkTestSelection addExcludePatterns(String ...patterns) {
        return addExcludePatterns(Arrays.asList(patterns));
    }

    /**
     * @see #addExcludePatterns
     */
    public JkTestSelection addExcludePatternsIf(boolean condition, String ...patterns) {
        if (condition) {
            addExcludePatterns(patterns);
        }
        return this;
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
     * Sets the include patterns for selecting test classes. Include patterns are
     * regular expressions used to filter test class names. Only test classes
     * whose names match at least one of the specified patterns will be included.
     */
    public JkTestSelection setIncludePatterns(Collection<String> includePatterns) {
        this.includePatterns = new LinkedHashSet<>(includePatterns);
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
    public JkTestSelection setDiscoveryConfigurer(JkUnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer) {
        this.discoveryConfigurer = discoveryConfigurer;
        return this;
    }

    public JkTestSelection copy() {
        return new JkTestSelection(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (testClassRoots != null) {
            sb.append("Test Class Roots: ").append(testClassRoots.relativizeFromWorkingDir());
        } else {
            sb.append("Test Class Roots: not set.");
        }
        if (!includePatterns.isEmpty()) {
            sb.append(", Include Patterns: ").append(includePatterns);
        }
        if (!excludePatterns.isEmpty()) {
            sb.append(", Exclude Patterns: ").append(excludePatterns);
        }
        if (!includeTags.isEmpty()) {
            sb.append(", Include Tags: ").append(includeTags);
        }
        if (!excludeTags.isEmpty()) {
            sb.append(", Exclude Tags: ").append(excludeTags);
        }
        return sb.toString();
    }

    /**
     * Adds specified dir to the test class root directories. It can be a collection of path or
     * a single path (as Path implements Iterable<Path>)
     */
    JkTestSelection withTestClassRoots(Iterable<Path> paths) {
        List<Path> pathList = JkUtilsPath.disambiguate(paths);
        JkTestSelection result = copy();
        result.testClassRoots = JkPathSequence.of(pathList);
        return result;
    }

    boolean hasTestClasses() {
        return JkPathTreeSet.ofRoots(this.testClassRoots.getEntries()).containFiles();
    }
}
