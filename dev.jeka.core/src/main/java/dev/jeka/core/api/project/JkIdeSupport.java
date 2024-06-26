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

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.java.JkJavaVersion;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Information necessary to generate metadata project file for IDE.
 */
public class JkIdeSupport {

    private JkCompileLayout prodLayout;

    private JkCompileLayout testLayout;

    private JkQualifiedDependencySet dependencies;

    private JkJavaVersion sourceVersion;

    private JkDependencyResolver dependencyResolver;

    private List<Path> generatedSourceDirs = new LinkedList<>();

    private JkIdeSupport(Path baseDir) {
        this.prodLayout = JkCompileLayout.of().setBaseDir(baseDir);
        this.testLayout = JkCompileLayout.of()
                .setSourceMavenStyle(JkCompileLayout.Concern.TEST)
                .setStandardOutputDirs(JkCompileLayout.Concern.TEST)
                .setBaseDir(baseDir);
        this.dependencies = JkQualifiedDependencySet.of();
        this.sourceVersion = JkJavaVersion.V8;
        this.dependencyResolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());
    }

    public static JkIdeSupport of(Path baseDir) {
        return new JkIdeSupport(baseDir);
    }

    public JkCompileLayout getProdLayout() {
        return prodLayout;
    }

    public JkCompileLayout getTestLayout() {
        return testLayout;
    }

    public JkQualifiedDependencySet getDependencies() {
        return dependencies;
    }

    public JkJavaVersion getSourceVersion() {
        return sourceVersion;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    public List<Path> getGeneratedSourceDirs() {
        return generatedSourceDirs;
    }

    public JkIdeSupport setProdLayout(JkCompileLayout prodLayout) {
        this.prodLayout = prodLayout;
        return this;
    }

    public JkIdeSupport setTestLayout(JkCompileLayout testLayout) {
        this.testLayout = testLayout;
        return this;
    }

    public JkIdeSupport setDependencies(JkQualifiedDependencySet dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public JkIdeSupport setDependencies(
            JkDependencySet allCompileDeps,
            JkDependencySet allRuntimeDeps,
            JkDependencySet allTestDeps) {
        return setDependencies(JkQualifiedDependencySet.computeIdeDependencies(allCompileDeps, allRuntimeDeps, allTestDeps));
    }

    public JkIdeSupport setSourceVersion(JkJavaVersion sourceVersion) {
        this.sourceVersion = sourceVersion;
        return this;
    }

    public JkIdeSupport setDependencyResolver(JkDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
        return this;
    }

    public JkIdeSupport setGeneratedSourceDirs(List<Path> generatedSourceDirs) {
        this.generatedSourceDirs = generatedSourceDirs;
        return this;
    }
}
