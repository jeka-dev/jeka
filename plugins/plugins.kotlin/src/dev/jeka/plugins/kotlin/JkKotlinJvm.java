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

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.jeka.core.api.project.JkProjectCompilation.JAVA_SOURCES_COMPILE_ACTION;

public class JkKotlinJvm {

    public static final String KOTLIN_JVM_SOURCES_COMPILE_ACTION = "compile-kotlin-jvm-sources";

    private final JkKotlinCompiler kotlinCompiler;

    private boolean addStdlib = true;

    private String jvmVersion = JkJavaVersion.ofCurrent().toString();

    private Path kotlinSourceSir;

    private Path kotlinTestSourceSir;

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

    public Path kotlinSourceDir() {
        return kotlinSourceSir;
    }

    public Path getKotlinTestSourceDir() {
        return kotlinTestSourceSir;
    }

    /**
     * Configures the specified project for Kotlin compilation and testing.
     */
    public void configureProject(JkProject project, Path kotlinSourceDir, Path kotlinTestSourceDir) {

        JkPathTreeSet javaSourceTree = project.compilation.layout.getSources();
        JkPathTreeSet javaSourceTestTree = project.test.compilation.layout.getSources();

        if (!javaSourceTree.containFiles()) {
            project.compilation.compileActions.remove(JAVA_SOURCES_COMPILE_ACTION);
        }
        if (!javaSourceTestTree.containFiles()) {
            project.test.compilation.compileActions.remove(JAVA_SOURCES_COMPILE_ACTION);
        }


        JkProjectCompilation prodCompile = project.compilation;
        JkProjectCompilation testCompile = project.test.compilation;
        this.kotlinSourceSir = kotlinSourceDir;
        this.kotlinTestSourceSir = kotlinTestSourceDir;

        prodCompile.dependencies.addVersionProvider(kotlinVersionProvider());
        JkJavaVersion targetVersion = project.getJvmTargetVersion();

        Path classDir = prodCompile.layout.resolveClassDir();
        prodCompile.preCompileActions.replaceOrAppend(
                    KOTLIN_JVM_SOURCES_COMPILE_ACTION,
                    () -> compileKotlinInSpinner(targetVersion, project.compilation, kotlinSourceDir, classDir));


        Path testClassDir = testCompile.layout.resolveClassDir();
        testCompile.preCompileActions.replaceOrAppend(
                KOTLIN_JVM_SOURCES_COMPILE_ACTION,
                () -> {
                    project.test.compilation.dependencies.add(project.compilation.layout.resolveClassDir());
                    compileKotlinInSpinner(targetVersion, project.test.compilation, kotlinTestSourceDir, testClassDir);
                });

        JkPathTree javaInKotlinDir = JkPathTree.of(project.getBaseDir().resolve(kotlinSourceDir));
        JkPathTree javaInKotlinTestDir = JkPathTree.of(project.getBaseDir().resolve(kotlinTestSourceDir));

        if (javaInKotlinDir.andMatching("**/*.java").containFiles()) {
            prodCompile.layout.addSources(javaInKotlinDir);
        }
        if (javaInKotlinTestDir.andMatching("**/*.java").containFiles()) {
            testCompile.layout.addSources(javaInKotlinTestDir);
        }


        if (addStdlib) {
            prodCompile.dependencies.modify(this::addStdLibsToProdDeps);
            testCompile.dependencies.modify(this::addStdLibsToTestDeps);
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

    private void compileKotlinInSpinner(JkJavaVersion targetVersion,
                                        JkProjectCompilation compilation,
                                        Path kotlinSourceDir, Path targetClassDir) {

        JkPathTreeSet sources = compilation.layout.resolveSources()
                .and(kotlinSourceDir);
        if (!sources.containFiles()) {
            JkLog.info("No source to compile in " + sources);
            return;
        }

        JkLog.verbose("Kotlin sources: %s", sources);
        AtomicBoolean success = new AtomicBoolean(false);
        JkConsoleSpinner.of("Compiling Kotlin sources")
                .setAlternativeMassage("Compiling Kotlin sources. It may take a while...")
                .run(() -> success.set(this.compileKotlin(targetVersion, compilation, sources, targetClassDir)));
        if (success.get()) {
            JkLog.info("%s Kotlin source files compiled successfully.",
                    sources.count(Integer.MAX_VALUE, false));
        } else {
            JkLog.error("Kotlin compilation failed.");
        }

    }

    private boolean compileKotlin(JkJavaVersion targetVersion,
                                  JkProjectCompilation compilation,
                                  JkPathTreeSet sources,
                                  Path targetClassDir) {
        if (targetVersion == null) {
            targetVersion = JkJavaVersion.of(jvmVersion);
        }
        JkKotlinJvmCompileSpec compileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(compilation.resolveDependenciesAsFiles())
                .setOutputDir(targetClassDir)
                .setTargetVersion(targetVersion)
                .setSources(sources);
        return kotlinCompiler.compile(compileSpec);
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
