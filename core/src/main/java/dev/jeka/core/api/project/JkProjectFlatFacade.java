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

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.git.JkGit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static dev.jeka.core.api.project.JkCompileLayout.Concern.PROD;
import static dev.jeka.core.api.project.JkCompileLayout.Concern.TEST;

/**
 * A simplified facade over {@link JkProject} to access its configuration
 * through a single entry point.
 *
 * This class organizes its methods into the following categories for easier exploration,
 * following a consistent naming convention:
 *
 * <ul>
 *   <li><b>Configuration Methods:</b> setXXX(), addXXX(), removeXXX()
 *       <br>These methods modify the underlying project configuration.</li>
 *   <li><b>Action Methods:</b> doXXX()
 *       <br>These methods trigger specific actions, such as building the project.</li>
 *   <li><b>Getter Methods:</b> getXXX()
 *       <br>These methods retrieve information from the project.</li>
 * </ul>
 */
public class JkProjectFlatFacade {

    private final JkProject project;

    public final JkDependencyFacade dependencies;

    JkProjectFlatFacade(JkProject project) {
        this.project = project;
        this.dependencies = new JkDependencyFacade();
    }

    /**
     * Sets the Java JVM version that will be used to compile sources and generate bytecode.
     */
    public JkProjectFlatFacade setJvmTargetVersion(JkJavaVersion version) {
        project.setJvmTargetVersion(version);
        return this;
    }

    /**
     * Sets if the project should produce regular or fat jar by default.
     */
    public JkProjectFlatFacade setMainArtifactJarType(JkProjectPackaging.JarType jarType) {
        if (jarType == JkProjectPackaging.JarType.REGULAR) {
            project.pack.setJarMaker(project.pack::createBinJar);
        } else if (jarType == JkProjectPackaging.JarType.FAT) {
            project.pack.setJarMaker(project.pack::createFatJar);
        } else if (jarType == JkProjectPackaging.JarType.SHADE) {
            project.pack.setJarMaker(project.pack::createShadeJar);
        } else {
            throw new IllegalArgumentException("Jar type " + jarType + " is not handled.");
        }
        return this;
    }

    public JkProjectFlatFacade addShadeJarArtifact(String classifier) {
        JkArtifactId artifactId = JkArtifactId.of(classifier, "jar");
        Path path = project.artifactLocator.getArtifactPath(artifactId);
        project.pack.actions.append("create-shade-jar",
                () -> project.pack.createShadeJar(path));
        return this;
    }

    /**
     * Sets the source encoding for the project.
     */
    public JkProjectFlatFacade setSourceEncoding(String encoding) {
        project.setSourceEncoding(encoding);
        return this;
    }

    /**
     * Sets the base directory for this Project.
     */
    public JkProjectFlatFacade setBaseDir(Path baseDir) {
        project.setBaseDir(baseDir);
        return this;
    }

    /**
     * Sets the base directory for this Project by specifying a String representing the
     * base directory path.
     */
    public JkProjectFlatFacade setBaseDir(String baseDir) {
        return setBaseDir(Paths.get(baseDir));
    }

    /**
     * Sets the layout style for the project's compilation layout.
     * <p>
     */
    public JkProjectFlatFacade setLayoutStyle(JkCompileLayout.Style style) {
        if (style == JkCompileLayout.Style.SIMPLE) {
            project.compilation.layout.setSourceSimpleStyle(PROD);
            project.test.compilation.layout.setSourceSimpleStyle(TEST);
        } else if (style == JkCompileLayout.Style.MAVEN) {
            project.compilation.layout.setSourceMavenStyle(PROD);
            project.test.compilation.layout.setSourceMavenStyle(TEST);
        } else {
            throw new IllegalStateException("Style " + style + " not handled.");
        }
        return this;
    }

    /**
     * The resources will be located in same the dir than the sources.
     */
    public JkProjectFlatFacade setMixResourcesAndSources() {
        project.compilation.layout.setMixResourcesAndSources();
        project.test.compilation.layout.setMixResourcesAndSources();
        return this;
    }

    /**
     * Sets the main class name to use in #runXxx and for building docker images.
     * The value <code>"auto"</code> means that it will e auto-discovered.
     *
     * @see JkProjectPackaging#setMainClass(String)
     */
    public JkProjectFlatFacade setMainClass(String mainClass) {
        project.pack.setMainClass(mainClass);
        return this;
    }


    /**
     * Adds compile-only dependencies to the project.
     *
     * @param coordinate the dependency to be added in the format of group:artifactId:version
     */
    public JkProjectFlatFacade addCompileOnlyDeps(@JkDepSuggest String coordinate) {
        dependencies.compile.add(coordinate);
        String moduleId = JkCoordinate.of(coordinate).getModuleId().toColonNotation();
        dependencies.runtime.remove(moduleId);
        return this;
    }


    /**
     * Sets whether to skip running tests for the project.
     */
    public JkProjectFlatFacade setTestsSkipped(boolean skipped) {
        project.test.setSkipped(skipped);
        return this;
    }


    /**
     * Sets the supplier for computing the project version.
     *
     * @param versionSupplier the supplier for computing the project version.
     */
    public JkProjectFlatFacade setVersionSupplier(Supplier<String> versionSupplier) {
        project.setVersionSupplier(() -> JkVersion.of(versionSupplier.get()));
        return this;
    }

    /**
     * Sets the version of the project.
     *
     * @param version the version to set for the project.
     */
    public JkProjectFlatFacade setVersion(String version) {
        return setVersionSupplier(() -> version);
    }

    /**
     * The published version will be computed according the current git tag.
     *
     * @see JkGit#getVersionFromTag()
     */
    public JkProjectFlatFacade setVersionFromGitTag() {
        return setVersionSupplier(() -> JkGit.of(getProject().getBaseDir()).getVersionFromTag());
    }

    /**
     * The published version will be computed according the last Git commit message.
     *
     * @see JkGit#getVersionFromCommitMessage(String)
     */
    public JkProjectFlatFacade setVersionFromGitTagCommitMessage(String suffixKeyword) {
        return setVersionSupplier(() -> JkGit.of(getProject().getBaseDir())
                .getVersionFromCommitMessage(suffixKeyword));
    }

    /**
     * @param moduleId group + artifactId to use when publishing on a binary repository.
     *                 Must be formatted as 'group:artifactId'
     */
    public JkProjectFlatFacade setModuleId(String moduleId) {
        project.setModuleId(JkModuleId.of(moduleId));
        return this;
    }

    /**
     * By default, every class in test folder are run. If you add an exclude filter,
     * tests accepting this filter won't be run.
     *
     * @param condition : the filter will be added only if this parameter is <code>true</code>.
     */
    public JkProjectFlatFacade addTestExcludeSuffixIf(boolean condition, String suffix) {
        if (condition) {
            project.test.selection.addExcludePatterns(".*" + suffix);
        }
        return this;
    }

    /**
     * By default, every class in test folder are run. If you add an including filter, only
     * tests accepting one of the declared filters will run.
     *
     * @param condition : the filter will be added only if this parameter is <code>true</code>.
     */
    public JkProjectFlatFacade addTestIncludeSuffixIf(boolean condition, String suffix) {
        project.test.selection.addIncludePatternsIf(condition, ".*" + suffix);
        return this;
    }

    /**
     * Adds a test include filters for test classes named as <code>^(Test.*|.+[.$]Test.*|.*Tests?)$</code>.
     * This is a standard filter in many tools.
     *
     * @see #addTestIncludeSuffixIf(boolean, String)
     */
    public JkProjectFlatFacade addTestMavenIncludePattern(boolean condition) {
        project.test.selection.addIncludePatternsIf(condition,
                JkTestSelection.MAVEN_INCLUDE_PATTERN);
        return this;
    }

    /**
     * Removes all test include patterns, effectively clearing any previously set
     * test class inclusion filters. This ensures that all class tests are included.
     */
    public JkProjectFlatFacade removeAllTestIncludePatterns() {
        project.test.selection.setIncludePatterns(Collections.emptySet());
        return this;
    }

    /**
     * Sets the style for displaying test progress during test execution.
     */
    public JkProjectFlatFacade setTestProgressStyle(JkTestProcessor.JkProgressStyle style) {
        project.test.processor.engineBehavior.setProgressDisplayer(style);
        return this;
    }

    /**
     * Adds a source generator to the project.
     */
    public JkProjectFlatFacade addSourceGenerator(JkProjectSourceGenerator sourceGenerator) {
        project.compilation.addSourceGenerator(sourceGenerator);
        return this;
    }

    // ------------------------------ action methods ---------------------------------

    /**
     * Executes the packing process for this project, which includes compiling, testing, and creating JAR files.
     *
     * @see JkProject#packActions
     */
    public JkProjectFlatFacade doPack() {
        project.pack.run();
        return this;
    }

    /**
     * Same as {@link #doPack()} but skips the testing phase.
     */
    public JkProjectFlatFacade doFastPack() {
        project.compilation.runIfNeeded();  // Better to launch it first explicitly for log clarity
        project.pack.actions.run();
        return this;
    }

    // ----------------------------- get methods -----------------------------------

    /**
     * Returns the project associated with this facade.
     */
    public JkProject getProject() {
        return project;
    }

    /**
     * Returns the main JAR file produced by the build.
     */
    public Path getMainJar() {
        return project.artifactLocator.getMainArtifactPath();
    }

    /**
     * Returns the directory where compiled classes are stored.
     */
    public Path getClassDir() {
        return project.compilation.layout.resolveClassDir();
    }

    /**
     * Returns the classpath used for running the built jar.
     */
    public List<Path> getRuntimeClasspath() {
        return project.pack.resolveRuntimeDependenciesAsFiles();
    }

    // -------------------------------------------------------------------

    public class JkDependencyFacade {

        public final JkDependencySetModifier compile;

        public final JkDependencySetModifier runtime;

        public final JkDependencySetModifier test;

        private JkDependencyFacade() {
            this.compile = project.compilation.dependencies;;
            this.runtime = project.pack.runtimeDependencies;
            this.test = project.test.compilation.dependencies;
        }

    }

}
