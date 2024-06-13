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

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;

/**
 * Provides factory methods to create different types of publications for a JkProject.
 */
public class JkProjectPublications {

    /**
     * Creates a JkMavenPublication for the specified JkProject.
     */
    public static JkMavenPublication mavenPublication(JkProject project) {
        return JkMavenPublication.of(project.artifactLocator)
                .setModuleIdSupplier(project::getModuleId)
                .setVersionSupplier(project::getVersion)
                .customizeDependencies(deps -> JkMavenPublication.computeMavenPublishDependencies(
                        project.compilation.getDependencies(),
                        project.packaging.getRuntimeDependencies(),
                        project.getDuplicateConflictStrategy()))
                .setBomResolutionRepos(project.dependencyResolver::getRepos)
                .putArtifact(JkArtifactId.MAIN_JAR_ARTIFACT_ID)
                .putArtifact(JkArtifactId.SOURCES_ARTIFACT_ID, project.packaging::createSourceJar)
                .putArtifact(JkArtifactId.JAVADOC_ARTIFACT_ID, project.packaging::createJavadocJar);
    }

    /**
     * Creates an Ivy publication from the specified
     */
    public static JkIvyPublication ivyPublication(JkProject project) {
        return JkIvyPublication.of()
                .putMainArtifact(project.artifactLocator.getMainArtifactPath())
                .setVersionSupplier(project::getVersion)
                .setModuleIdSupplier(project::getModuleId)
                .configureDependencies(deps -> JkIvyPublication.getPublishDependencies(
                        project.compilation.getDependencies(),
                        project.packaging.getRuntimeDependencies(),
                        project.getDuplicateConflictStrategy()));
    }

}
