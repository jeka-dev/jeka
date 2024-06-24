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

public class JkDependencySetModifier {

    private  Function<JkDependencySet, JkDependencySet> modifier = deps -> deps;

    private JkDependencySetModifier() {
    }

    public static JkDependencySetModifier of() {
        return new JkDependencySetModifier();
    }

    public JkDependencySetModifier add(@JkDepSuggest String coordinate, Object... tokens) {
        modifier = modifier.andThen(deps -> deps.and(coordinate, tokens));
        return this;
    }

    public JkDependencySetModifier addWithExclusions(@JkDepSuggest String coordinate, @JkDepSuggest String... exclusions) {
        modifier = modifier.andThen(deps -> deps.and(coordinate).withLocalExclusions(exclusions));
        return this;
    }

    public JkDependencySetModifier move(@JkDepSuggest String moduleId, JkDependencySet.Hint hint) {
        modifier = modifier.andThen(deps -> deps.withMoving(moduleId, hint));
        return this;
    }

    public JkDependencySetModifier add(JkCoordinate coordinates) {
        modifier = modifier.andThen(deps -> deps.and(coordinates));
        return this;
    }

    public JkDependencySetModifier add(Path file) {
        modifier = modifier.andThen(deps -> deps.andFiles(file));
        return this;
    }

    public JkDependencySetModifier add(JkDependency dependency) {
        modifier = modifier.andThen(deps -> deps.and(dependency));
        return this;
    }

    public JkDependencySetModifier add(@JkDepSuggest String coordinate, JkTransitivity transitivity, Object ...tokens) {
        modifier = modifier.andThen(deps -> deps.and(coordinate, transitivity, tokens));
        return this;
    }

    public JkDependencySetModifier add(JkDependencySet.Hint hint, String coordinate) {
        modifier = modifier.andThen(deps -> deps.and(hint, coordinate));
        return this;
    }

    public JkDependencySetModifier addVersionProvider(JkVersionProvider versionProvider) {
        modifier = modifier.andThen(deps -> deps.andVersionProvider(versionProvider));
        return this;
    }

    public JkDependencySetModifier remove(@JkDepSuggest String moduleId) {
        modifier = modifier.andThen(deps -> deps.minus(moduleId));
        return this;
    }

    public JkDependencySetModifier remove(Path file) {
        modifier = modifier.andThen(deps -> deps.minus(file));
        return this;
    }

    public JkDependencySetModifier modify(Function<JkDependencySet, JkDependencySet> modifier) {
        this.modifier = this.modifier.andThen(modifier);
        return this;
    }

    public JkDependencySet get() {
        return modifier.apply(JkDependencySet.of());
    }

}
