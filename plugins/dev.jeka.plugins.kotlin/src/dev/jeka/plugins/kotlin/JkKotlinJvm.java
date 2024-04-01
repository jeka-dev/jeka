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

package dev.jeka.plugins.kotlin;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.kotlin.JkKotlinModules;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectCompilation;
import dev.jeka.core.api.system.JkConsoleSpinner;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;

import static dev.jeka.core.api.project.JkProjectCompilation.JAVA_SOURCES_COMPILE_ACTION;

public class JkKotlinJvm {

    public static final String KOTLIN_JVM_SOURCES_COMPILE_ACTION = "compile-kotlin-jvm-sources";

    private final JkKotlinCompiler kotlinCompiler;

    private boolean addStdlib = true;

    private String jvmVersion = JkJavaVersion.ofCurrent().toString();

    private JkKotlinJvm(JkKotlinCompiler kotlinCompiler) {
        this.kotlinCompiler = kotlinCompiler;
    }

    public static JkKotlinJvm of(JkKotlinCompiler kotlinCompiler) {
        return new JkKotlinJvm(kotlinCompiler);
    }

    public static JkKotlinJvm of() {
        return new JkKotlinJvm(null);
    }

    /**
     * Sets whether to add the standard Kotlin library to the classpath.
     */
    public JkKotlinJvm setAddStdlib(boolean addStdlib) {
        this.addStdlib = addStdlib;
        return this;
    }

    /**
     * Sets the JVM target version to be used for compiling Kotlin code.
     */
    public JkKotlinJvm setJvmVersion(String jvmVersion) {
        this.jvmVersion = jvmVersion;
        return this;
    }

    /**
     * Retrieves the Kotlin compiler associated with this JkKotlinJvm.
     */
    public JkKotlinCompiler getKotlinCompiler() {
        return kotlinCompiler;
    }

    /**
     * Configures the specified project for Kotlin compilation and testing.
     */
    public void configure(JkProject project, String kotlinSourceDir, String kotlinTestSourceDir) {
        if (!JkUtilsString.isBlank(kotlinSourceDir)) {
            project.compilation.layout.setSources(kotlinTestSourceDir);
        }
        if (!JkUtilsString.isBlank(kotlinTestSourceDir)) {
            project.testing.compilation.layout.setSources(kotlinTestSourceDir);
        }
        JkProjectCompilation prodCompile = project.compilation;
        JkProjectCompilation testCompile = project.testing.compilation;
        prodCompile
                .customizeDependencies(deps -> deps.andVersionProvider(kotlinVersionProvider()))
                .preCompileActions
                    .replaceOrInsertBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                        () -> compileKotlinInSpinner(project, kotlinSourceDir));
        testCompile
                .preCompileActions
                    .replaceOrInsertBefore(KOTLIN_JVM_SOURCES_COMPILE_ACTION, JAVA_SOURCES_COMPILE_ACTION,
                        () -> compileTestKotlinInSpinner(project, kotlinTestSourceDir));

        JkPathTree javaInKotlinDir = JkPathTree.of(project.getBaseDir().resolve(kotlinSourceDir));
        JkPathTree javaInKotlinTestDir = JkPathTree.of(project.getBaseDir().resolve(kotlinTestSourceDir));
        prodCompile.layout.setSources(javaInKotlinDir);
        testCompile.layout.setSources(javaInKotlinTestDir);
        if (addStdlib) {
            prodCompile.customizeDependencies(this::addStdLibsToProdDeps);
            testCompile.customizeDependencies(this::addStdLibsToTestDeps);
        }

        /*
        project.setJavaIdeSupport(ideSupport -> {
            ideSupport.getProdLayout().addSource(project.getBaseDir().resolve(kotlinSourceDir));
            if (kotlinTestSourceDir != null) {
                ideSupport.getTestLayout().addSource(project.getBaseDir().resolve(kotlinTestSourceDir));
            }
            return ideSupport;
        });

         */
    }

    private JkVersionProvider kotlinVersionProvider() {
        return JkKotlinModules.versionProvider(kotlinCompiler.getVersion());
    }

    private void compileKotlinInSpinner(JkProject javaProject, String kotlinSourceDir) {
        JkConsoleSpinner.of("Compiling Kotlin sources")
                .setAlternativeMassage("Compiling Kotlin sources. It may take a while...")
                .run(() -> this.compileKotlin(javaProject, kotlinSourceDir));
    }

    private void compileKotlin(JkProject javaProject, String kotlinSourceDir) {
        JkProjectCompilation compilation = javaProject.compilation;
        JkPathTreeSet sources = compilation.layout.resolveSources();

        if (!JkUtilsString.isBlank(kotlinSourceDir)) {
            sources = sources .and(javaProject.getBaseDir().resolve(kotlinSourceDir));

        }
        if (sources.count(1, false) == 0) {
            JkLog.info("No source to compile in " + sources);
            return;
        }
        JkJavaVersion targetVersion = javaProject.getJvmTargetVersion();
        if (targetVersion == null) {
            targetVersion = JkJavaVersion.of(jvmVersion);
        }
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(compilation.resolveDependenciesAsFiles())
                .setOutputDir(compilation.layout.getOutputDir().resolve("classes"))
                .setTargetVersion(targetVersion)
                .setSources(sources);
        kotlinCompiler.compile(compileSpec);
    }

    private void compileTestKotlinInSpinner(JkProject javaProject, String kotlinTestSourceDir) {
        JkConsoleSpinner.of("Compiling Kotlin test sources")
                .setAlternativeMassage("Compiling Kotlin stest ources. It may take a while...")
                .run(() -> this.compileTestKotlin(javaProject, kotlinTestSourceDir));
    }

    private void compileTestKotlin(JkProject javaProject, String kotlinTestSourceDir) {
        JkProjectCompilation compilation = javaProject.testing.compilation;
        JkPathTreeSet sources = compilation.layout.resolveSources();
        if (JkUtilsString.isBlank(kotlinTestSourceDir)) {
            sources = sources.and(javaProject.getBaseDir().resolve(kotlinTestSourceDir));
        }
        if (sources.count(1, false) == 0) {
            JkLog.info("No source to compile in " + sources);
            return;
        }
        JkPathSequence classpath = JkPathSequence.of(compilation.resolveDependenciesAsFiles())
                .and(compilation.layout.getClassDirPath());
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setSources(compilation.layout.resolveSources())
                .setClasspath(classpath)
                .setOutputDir(compilation.layout.getOutputDir().resolve("test-classes"))
                .setTargetVersion(javaProject.getJvmTargetVersion());
        kotlinCompiler.compile(compileSpec);
    }

    private JkDependencySet addStdLibsToProdDeps(JkDependencySet deps) {
        return kotlinCompiler.isProvidedCompiler()
                ? deps.andFiles(kotlinCompiler.getStdLib())
                : deps.and(JkKotlinModules.STDLIB_JDK8).and(JkKotlinModules.REFLECT);
    }

    private JkDependencySet addStdLibsToTestDeps(JkDependencySet deps) {
        return kotlinCompiler.isProvidedCompiler() ? deps.and(JkKotlinModules.TEST) : deps;
    }

}
