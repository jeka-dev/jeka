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

package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;

/**
 * Provides factory methods to create different types of publications for a JkProject.
 */
public class JkProjectPublications {

    /**
     * Creates an Ivy publication from the specified
     */
    public static JkIvyPublication ivyPublication(JkProject project) {
        return JkIvyPublication.of()
                .putMainArtifact(project.artifactLocator.getMainArtifactPath())
                .setVersionSupplier(project::getVersion)
                .setModuleIdSupplier(project::getModuleId)
                .configureDependencies(deps -> JkIvyPublication.getPublishDependencies(
                        project.compilation.dependencies.get(),
                        project.packaging.runtimeDependencies.get(),
                        project.getDuplicateConflictStrategy()));
    }

}
