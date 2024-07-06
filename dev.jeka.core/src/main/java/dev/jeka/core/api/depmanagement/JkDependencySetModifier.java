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

package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;
import java.util.function.Function;


/**
 * This class allows modifying a JkDependencySet using a series of method calls.
 */
public class JkDependencySetModifier {

    private  Function<JkDependencySet, JkDependencySet> modifier = deps -> deps;

    private JkDependencySetModifier() {
    }

    /**
     * Returns a new instance of JkDependencySetModifier.
     */
    public static JkDependencySetModifier of() {
        return new JkDependencySetModifier();
    }

    /**
     * Adds a dependency to the JkDependencySetModifier.
     *
     * @param coordinate the dependency coordinate in the format of group:artifactId:version
     * @param tokens     the tokens for formatting the coordinate (in case of variable version for instance)
     */
    public JkDependencySetModifier add(@JkDepSuggest String coordinate, Object... tokens) {
        modifier = modifier.andThen(deps -> deps.and(coordinate, tokens));
        return this;
    }

    /**
     * Adds a dependency to the {@link JkDependencySetModifier} with exclusions.
     *
     * @param coordinate  the dependency coordinate in the format of group:artifactId:version
     * @param exclusions  the moduleIds to exclude (e.g. "a.group:a.name", "another.group:another.name", ...)
     */
    public JkDependencySetModifier addWithExclusions(@JkDepSuggest String coordinate, @JkDepSuggest String... exclusions) {
        modifier = modifier.andThen(deps -> deps.and(coordinate).withLocalExclusions(exclusions));
        return this;
    }

    /**
     * Moves a dependency within the JkDependencySetModifier,. This is used to alter
     * the order in which dependencies should be declared.
     */
    public JkDependencySetModifier move(@JkDepSuggest String moduleId, JkDependencySet.Hint hint) {
        modifier = modifier.andThen(deps -> deps.withMoving(moduleId, hint));
        return this;
    }

    /**
     * Adds a JkCoordinate to the JkDependencySetModifier.
     */
    public JkDependencySetModifier add(JkCoordinate coordinates) {
        modifier = modifier.andThen(deps -> deps.and(coordinates));
        return this;
    }

    /**
     * Adds a file-based dependency to the JkDependencySetModifier.
     */
    public JkDependencySetModifier add(Path file) {
        modifier = modifier.andThen(deps -> deps.andFiles(file));
        return this;
    }

    /**
     * Adds a dependency to the JkDependencySetModifier.
     */
    public JkDependencySetModifier add(JkDependency dependency) {
        modifier = modifier.andThen(deps -> deps.and(dependency));
        return this;
    }

    /**
     * Adds a dependency to the JkDependencySetModifier.
     *
     * @param coordinate the dependency coordinate in the format of group:artifactId:version
     * @param transitivity the transitivity of the dependency
     * @param tokens the tokens for formatting the coordinate (in case of variable version for instance)
     */
    public JkDependencySetModifier add(@JkDepSuggest String coordinate, JkTransitivity transitivity, Object ...tokens) {
        modifier = modifier.andThen(deps -> deps.and(coordinate, transitivity, tokens));
        return this;
    }

    /**
     * Adds a dependency to the JkDependencySetModifier.
     *
     * @param hint       the hint for prioritizing the dependency
     * @param coordinate the dependency coordinate in the format of group:artifactId:version
     */
    public JkDependencySetModifier add(JkDependencySet.Hint hint, String coordinate) {
        modifier = modifier.andThen(deps -> deps.and(hint, coordinate));
        return this;
    }

    /**
     * Adds a version provider to the JkDependencySetModifier.
     */
    public JkDependencySetModifier addVersionProvider(JkVersionProvider versionProvider) {
        modifier = modifier.andThen(deps -> deps.andVersionProvider(versionProvider));
        return this;
    }

    /**
     * Removes a dependency from the JkDependencySetModifier.
     *
     * @param moduleId the module id of the dependency to be removed (e.g a.group:a-module)
     */
    public JkDependencySetModifier remove(@JkDepSuggest String moduleId) {
        modifier = modifier.andThen(deps -> deps.minus(moduleId));
        return this;
    }

    /**
     * Removes all dependencies from the JkDependencySetModifier.
     */
    public JkDependencySetModifier removeAll() {
        modifier = modifier.andThen(deps -> JkDependencySet.of());
        return this;
    }

    /**
     * Removes a file-based dependency from the JkDependencySetModifier.
     *
     * @param file the file representing the dependency to be removed
     */
    public JkDependencySetModifier remove(Path file) {
        modifier = modifier.andThen(deps -> deps.minus(file));
        return this;
    }

    /**
     * Modifies the {@link JkDependencySet} using the provided modifier function.
     * The modifier function takes a {@link JkDependencySet} as input and returns a modified {@link JkDependencySet}.
     *
     * @param modifier the function used to modify the {@link JkDependencySet}
     */
    public JkDependencySetModifier modify(Function<JkDependencySet, JkDependencySet> modifier) {
        this.modifier = this.modifier.andThen(modifier);
        return this;
    }

    /**
     * Compiutes  a {@link JkDependencySet} by applying the modifier function to an empty set.
     */
    public JkDependencySet get() {
        return modifier.apply(JkDependencySet.of());
    }

}
