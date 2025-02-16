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

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.function.JkConsumers;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.system.JkProcess;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adapter for both JkProject an BaseKBean that can be used as an abstraction
 * on this two classes.
 */
public interface JkBuildable {

    enum Adapted {
        PROJECT, BASE
    }

    /**
     * Files for artifacts to publish.
     */
    JkArtifactLocator getArtifactLocator();

    Path getClassDir();

    JkResolveResult resolveRuntimeDependencies();

    List<Path> getRuntimeDependenciesAsFiles();

    JkVersion getVersion();

    JkModuleId getModuleId();

    Path getOutputDir();

    Path getBaseDir();

    String getMainClass();

    void compileIfNeeded();

    JkDependencyResolver getDependencyResolver();

    Path getMainJarPath();

    Adapted getAdapted();

    boolean compile(JkJavaCompileSpec compileSpec);

    /**
     * Get the set of dependencies required for compilation.
     */
    JkDependencySet getCompiledDependencies();

    /**
     * Get the dependencies needed at runtime,
     * typically including compile-time dependencies and those used only at runtime.
     */
    JkDependencySet getRuntimesDependencies();

    /**
     * Returns the strategy for resolving dependency conflicts.
     */
    JkCoordinate.ConflictStrategy getDependencyConflictStrategy();

    void createSourceJar(Path targetFile);

    void createJavadocJar(Path targetFile);

    void setVersionSupplier(java.util.function.Supplier<JkVersion> versionSupplier);

    JkConsumers<JkManifest> getManifestCustomizers();

    interface Supplier {
        JkBuildable asBuildable();
    }

    JkJavaProcess prepareRunJar();

}
