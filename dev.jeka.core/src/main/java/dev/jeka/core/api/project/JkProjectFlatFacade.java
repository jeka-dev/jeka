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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static dev.jeka.core.api.project.JkCompileLayout.Concern.PROD;
import static dev.jeka.core.api.project.JkCompileLayout.Concern.TEST;

/**
 * Flat facade over {@link JkProject} to access {@link JkProject} configuration
 * from a single entry point.
 */
public class JkProjectFlatFacade {

    private final JkProject project;

    public final JkDependencySetModifier compileDependencies;

    public final JkDependencySetModifier runtimeDependencies;

    public final JkDependencySetModifier testDependencies;

    JkProjectFlatFacade(JkProject project) {
        this.project = project;
        this.compileDependencies = project.compilation.dependencies;;
        this.runtimeDependencies = project.packaging.runtimeDependencies;
        this.testDependencies = project.testing.compilation.dependencies;
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
            project.setJarMaker(project.packaging::createBinJar);
        } else if (jarType == JkProjectPackaging.JarType.FAT) {
            project.setJarMaker(project.packaging::createFatJar);
        } else if (jarType == JkProjectPackaging.JarType.SHADE) {
            project.setJarMaker(project.packaging::createShadeJar);
        } else {
            throw new IllegalArgumentException("Jar type " + jarType + " is not handled.");
        }
        return this;
    }

    public JkProjectFlatFacade addShadeJarArtifact(String classifier) {
        JkArtifactId artifactId = JkArtifactId.of(classifier, "jar");
        Path path = project.artifactLocator.getArtifactPath(artifactId);
        project.packActions.append(() -> project.packaging.createShadeJar(path));
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
            project.testing.compilation.layout.setSourceSimpleStyle(TEST);
        } else if (style == JkCompileLayout.Style.MAVEN) {
            project.compilation.layout.setSourceMavenStyle(PROD);
            project.testing.compilation.layout.setSourceMavenStyle(TEST);
        } else {
            throw new IllegalStateException("Style " + style + " not handled.");
        }
        return this;
    }

    /**
     * The resources will be located in same the dir than the sources.
     */
    public JkProjectFlatFacade mixResourcesAndSources() {
        project.compilation.layout.mixResourcesAndSources();
        project.testing.compilation.layout.mixResourcesAndSources();
        return this;
    }

    /**
     * Sets the main class name to use in #runXxx and for building docker images.
     * The value <code>"auto"</code> means that it will e auto-discovered.
     *
     * @see JkProjectPackaging#setMainClass(String)
     */
    public JkProjectFlatFacade setMainClass(String mainClass) {
        project.packaging.setMainClass(mainClass);
        return this;
    }



    /**
     * Adds compile-only dependencies to the project.
     *
     * @param coordinate the dependency to be added in the format of group:artifactId:version
     */
    public JkProjectFlatFacade addCompileOnlyDeps(@JkDepSuggest String coordinate) {
        compileDependencies.add(coordinate);
        String moduleId = JkCoordinate.of(coordinate).getModuleId().toColonNotation();
        runtimeDependencies.remove(moduleId);
        return this;
    }

    /**
     * Adds test dependencies to the project.
     *
     * @param coordinates the dependencies to be added in the format of group:artifactId:version
     */
    public JkProjectFlatFacade addTestDeps(@JkDepSuggest String... coordinates) {
        UnaryOperator<JkDependencySet> addFun = deps -> addFirst(deps, coordinates);
        return prependTestDeps(addFun);
    }


    /**
     * Sets whether to skip running tests for the project.
     */
    public JkProjectFlatFacade skipTests(boolean skipped) {
        project.testing.setSkipped(skipped);
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
     * The published version will be computed according the git last commit message.
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
    public JkProjectFlatFacade addTestExcludeFilterSuffixedBy(String suffix, boolean condition) {
        if (condition) {
            project.testing.testSelection.addExcludePatterns(".*" + suffix);
        }
        return this;
    }

    /**
     * By default, every class in test folder are run. If you add an including filter, only
     * tests accepting one of the declared filters will run.
     *
     * @param condition : the filter will be added only if this parameter is <code>true</code>.
     */
    public JkProjectFlatFacade addTestIncludeFilterSuffixedBy(String suffix, boolean condition) {
        project.testing.testSelection.addIncludePatternsIf(condition, ".*" + suffix);
        return this;
    }

    /**
     * Adds a test include filters for test classes named as <code>^(Test.*|.+[.$]Test.*|.*Tests?)$</code>.
     * This is a standard filter in many tools.
     *
     * @see #addTestIncludeFilterSuffixedBy(String, boolean)
     */
    public JkProjectFlatFacade addTestIncludeFilterOnStandardNaming(boolean condition) {
        project.testing.testSelection.addIncludePatternsIf(condition,
                JkTestSelection.STANDARD_INCLUDE_PATTERN);
        return this;
    }

    /**
     * Sets the style for displaying test progress during test execution.
     */
    public JkProjectFlatFacade setTestProgressStyle(JkTestProcessor.JkProgressOutputStyle style) {
        project.testing.testProcessor.engineBehavior.setProgressDisplayer(style);
        return this;
    }

    /**
     * Adds a source generator to the project.
     */
    public JkProjectFlatFacade addSourceGenerator(JkProjectSourceGenerator sourceGenerator) {
        project.compilation.addSourceGenerator(sourceGenerator);
        return this;
    }

    /**
     * Retrieves the project associated with this facade.
     */
    public JkProject getProject() {
        return project;
    }

    private JkProjectFlatFacade prependTestDeps(Function<JkDependencySet, JkDependencySet> modifier) {
        testDependencies.modify(deps -> deps.and(JkDependencySet.Hint.first(), modifier.apply(JkDependencySet.of())));
        return this;
    }

    private JkDependencySet add(JkDependencySet deps, String... descriptors) {
        JkDependencySet result = deps;
        for (String descriptor : descriptors) {
            result = result.and(descriptor);
        }
        return result;
    }

    private JkDependencySet addFirst(JkDependencySet deps, String... descriptors) {
        JkDependencySet result = deps;
        List<String> items = new LinkedList<>(Arrays.asList(descriptors));
        Collections.reverse(items);
        for (String descriptor : items) {
            result = result.and(JkDependencySet.Hint.first(), descriptor);
        }
        return result;
    }


    private JkDependencySet minus(JkDependencySet deps, String... descriptors) {
        JkDependencySet result = deps;
        for (String descriptor : descriptors) {
            result = result.minus(descriptor);
        }
        return result;
    }

}
