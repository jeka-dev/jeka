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

package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkInternalDependencyResolver;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Not part of the public API.
 */
public interface JkInternalPublisher {

    String FACTORY_CLASS_NAME = "dev.jeka.core.api.depmanagement.embedded.ivy.IvyInternalPublisherFactory";

    /**
     * Publishes the specified module to the repository mentioned in the publication.
     *
     * @param coordinate
     *            The module/version to publish.
     * @param publishedArtifacts
     *            The artifacts to publish.
     * @param dependencies
     *            The dependencies of the published module.
     */
    void publishIvy(JkCoordinate coordinate, List<JkIvyPublication.JkIvyPublishedArtifact> publishedArtifacts,
                    JkQualifiedDependencySet dependencies);

    void publishMaven(JkCoordinate coordinate,
                      JkArtifactPublisher artifactPublisher,
                      JkPomMetadata pomMetadata,
                      JkDependencySet dependencySet,
                      Map<JkModuleId, JkVersion> managedDependencies);

    static JkInternalPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(FACTORY_CLASS_NAME);
        if (factoryClass != null) {
            return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", publishRepos, artifactDir);
        }
        factoryClass = JkClassLoader.of(JkInternalDependencyResolver.InternalVvyClassloader.get())
                .load(FACTORY_CLASS_NAME);
        return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", publishRepos, artifactDir);
        /*
        return JkInternalDependencyResolver.InternalVvyClassloader.get().createCrossClassloaderProxy(
                JkInternalPublisher.class, FACTORY_CLASS_NAME, "of", publishRepos, artifactDir);

         */

    }

}
