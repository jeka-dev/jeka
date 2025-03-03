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

import java.util.Objects;
import java.util.Optional;

/**
 * A piece of information aiming at excluding transitive dependencies.
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyExclusion {

    private final JkModuleId moduleId;

    private final JkCoordinate.JkArtifactSpecification artifactSpecification;

    private JkDependencyExclusion(JkModuleId moduleId,
                                  JkCoordinate.JkArtifactSpecification artifactSpecification) {
        super();
        this.moduleId = moduleId;
        this.artifactSpecification = artifactSpecification;
    }

    /**
     * Creates an exclusion of the specified module.
     */
    @SuppressWarnings("unchecked")
    public static JkDependencyExclusion of(JkModuleId moduleId) {
        return new JkDependencyExclusion(moduleId, null);
    }

    /**
     * Creates an exclusion of the specified module.
     */
    public static JkDependencyExclusion of(String group, String name) {
        return of(JkModuleId.of(group, name));
    }

    /**
     * Creates an exclusion of the specified module.
     */
    public static JkDependencyExclusion of(String moduleId) {
        return of(JkModuleId.of(moduleId));
    }

    public JkDependencyExclusion withClassierAndType(String classifier, String type) {
        return new JkDependencyExclusion(moduleId, JkCoordinate.JkArtifactSpecification.of(classifier, type));
    }

    public JkDependencyExclusion withType(String type) {
        return withClassierAndType(null, type);
    }

    public JkDependencyExclusion withClassifier(String classifier) {
        return withClassierAndType(classifier, null);
    }

    /**
     * Returns the module id to exclude.
     */
    public JkModuleId getModuleId() {
        return moduleId;
    }

    public String getClassifier() {
        return Optional.ofNullable(artifactSpecification).map(spec -> spec.getClassifier()).orElse(null);
    }

    public String getType() {
        return Optional.ofNullable(artifactSpecification).map(spec -> spec.getType()).orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkDependencyExclusion that = (JkDependencyExclusion) o;
        if (!moduleId.equals(that.moduleId)) return false;
        return Objects.equals(artifactSpecification, that.artifactSpecification);
    }

    @Override
    public int hashCode() {
        int result = moduleId.hashCode();
        result = 31 * result + (artifactSpecification != null ? artifactSpecification.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JkDependencyExclusion{" +
                "moduleId=" + moduleId +
                ", artifactSpecification=" + artifactSpecification +
                '}';
    }

}
