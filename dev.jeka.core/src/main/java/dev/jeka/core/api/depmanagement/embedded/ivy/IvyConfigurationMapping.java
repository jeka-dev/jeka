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

package dev.jeka.core.api.depmanagement.embedded.ivy;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An  Ivy configuration to configurations mapping declared along each dependency. </p>
 * The left part describe for which purpose you need this dependency (compile, runtime, test). <br/>
 * The right part describe which transitive dependencies you want to retrieve along the dependency. <br/>
 * <p>
 * For example, your component 'A' depends on component 'B' for compiling. 'A' needs jar 'B' itself, but
 * also its transitive dependencies that 'B' declares for 'compile' and 'runtime' purposes. Then, 'A' to 'B'
 * dependency should be declared with configuration mapping 'compile -> compile, runtime';
 *
 * This concept matches strictly with the <i>configuration</i> concept found in Ivy : <a href="http://wrongnotes.blogspot.be/2014/02/simplest-explanation-of-ivy.html">see here.</a>.
 */
final class IvyConfigurationMapping {

    private static final String ARROW = "->";

    /**
     * Useful when using configuration mapping. As documented in Ivy, it stands for the main archive.
     */
    public static final String ARCHIVE_MASTER = "archives(master)";

    public static final String COMPILE = "compile";

    public static final String RUNTIME = "runtime";

    public static final String TEST = "test";

    private final Set<String> left;

    private final Set<String> right;

    private static final IvyConfigurationMapping EMPTY =
            new IvyConfigurationMapping(Collections.emptySet(), Collections.emptySet());

    private IvyConfigurationMapping(Set<String> left, Set<String> right) {
        this.left = left;
        this.right = right;
    }

    public static IvyConfigurationMapping of(Set<String> left, Set<String> right) {
        return new IvyConfigurationMapping(left, right);
    }

    public static IvyConfigurationMapping of(String left, String ... rights) {
        return new IvyConfigurationMapping(Collections.singleton(left), Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(rights))));
    }

    public static List<IvyConfigurationMapping> ofMultiple(String ivyExpression) {
        if (ivyExpression == null) {
            return Collections.singletonList(EMPTY);
        }
        String[] items = ivyExpression.split(";");
        return Arrays.stream(items)
                .map(IvyConfigurationMapping::of)
                .collect(Collectors.toList());

    }

    public static IvyConfigurationMapping of(String ivyItemExpression) {
        if (ivyItemExpression == null) {
            return EMPTY;
        }
        String[] items = ivyItemExpression.split(ARROW);
        if (items.length > 2) {
            throw new IllegalArgumentException("More than one '->' detected in ivy expression " + ivyItemExpression);
        }
        final Set<String> right;
        if (items.length == 1) {
            right = Collections.emptySet();
        } else {
            right = ofPart(items[1]);
        }
        return new IvyConfigurationMapping(ofPart(items[0]), right);
    }

    public Set<String> getLeft() {
        return left;
    }

    public Set<String> getRight() {
        return right;
    }

    private static Set<String> ofPart(String comaSeparated) {
        return Arrays.stream(comaSeparated.split(",")).map(String::trim).collect(Collectors.toSet());
    }

}