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

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.java.JkJavaCompileSpec;

import java.nio.file.Path;
import java.util.List;

/**
 * Adapter for both JkProject an BaseKBean that can be used as an abstraction
 * on this two classes.
 */
public interface JkBuildable {

    enum Adapted {
        PROJECT, BASE
    }

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


}
