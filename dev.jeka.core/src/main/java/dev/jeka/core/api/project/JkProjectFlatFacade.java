package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.JkGit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static dev.jeka.core.api.project.JkCompileLayout.Concern.PROD;
import static dev.jeka.core.api.project.JkCompileLayout.Concern.TEST;

/**
 * Simple facade over {@link JkProject} to access common setting conveniently.
 */
public class JkProjectFlatFacade {

    private final JkProject project;

    JkProjectFlatFacade(JkProject project) {
        this.project = project;
    }

    public JkProjectFlatFacade applyOnProject(Consumer<JkProject> projectConsumer) {
        project.apply(projectConsumer);
        return this;
    }

    public JkProjectFlatFacade apply(Consumer<JkProjectFlatFacade> facadeConsumer) {
        facadeConsumer.accept(this);
        return this;
    }

    public JkProjectFlatFacade setJvmTargetVersion(JkJavaVersion version) {
        project.setJvmTargetVersion(version);
        return this;
    }

    public JkProjectFlatFacade setMainArtifactJarType(JkProjectPackaging.JarType jarType) {
        if (jarType == JkProjectPackaging.JarType.REGULAR) {
            project.packActions.set(project.packaging::createBinJar);
        } else if (jarType == JkProjectPackaging.JarType.FAT) {
            project.packActions.set(() -> project.packaging.createFatJar(project.artifactLocator.getMainArtifactPath()));
        } else {
            throw new IllegalArgumentException("Jar type " + jarType + " is not handled.");
        }
        return this;
    }

    public JkProjectFlatFacade setSourceEncoding(String encoding) {
        project.setSourceEncoding(encoding);
        return this;
    }

    public JkProjectFlatFacade setBaseDir(Path baseDir) {
        project.setBaseDir(baseDir);
        return this;
    }

    public JkProjectFlatFacade setBaseDir(String baseDir) {
        return setBaseDir(Paths.get(baseDir));
    }

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
     * The resources will be located in same dirs than sources.
     */
    public JkProjectFlatFacade mixResourcesAndSources() {
        project.compilation.layout.mixResourcesAndSources();
        project.testing.compilation.layout.mixResourcesAndSources();
        return this;
    }

    public JkProjectFlatFacade publishJavadocAndSources(boolean includeJavaDoc, boolean includeSources) {
        if (!includeJavaDoc) {
            project.mavenPublication.removeArtifact(JkArtifactId.JAVADOC_ARTIFACT_ID);
        }
        if (!includeSources) {
            project.mavenPublication.removeArtifact(JkArtifactId.SOURCES_ARTIFACT_ID);
        }
        return this;
    }

    public JkProjectFlatFacade configureCompileDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        project.compilation.configureDependencies(modifier);
        return this;
    }

    public JkProjectFlatFacade configureRuntimeDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        project.packaging.configureRuntimeDependencies(modifier);
        return this;
    }

    public JkProjectFlatFacade configureTestDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        project.testing.compilation.configureDependencies(modifier);
        return this;
    }

    public JkProjectFlatFacade addCompileDeps(@JkDepSuggest String... moduleDescriptors) {
        UnaryOperator<JkDependencySet> addFun = deps -> add(deps, moduleDescriptors);
        return configureCompileDependencies(addFun);
    }

    public JkProjectFlatFacade addCompileOnlyDeps(@JkDepSuggest String... moduleDescriptors) {
        UnaryOperator<JkDependencySet> addFun = deps -> add(deps, moduleDescriptors);
        configureCompileDependencies(addFun);
        String[] groupAndNames = Arrays.stream(moduleDescriptors)
                .map(JkCoordinate::of)
                .map(coodinate -> coodinate.getModuleId().getColonNotation())
                .collect(Collectors.toList())
                .toArray(new String[0]);
        UnaryOperator<JkDependencySet> minusFun = deps -> minus(deps, groupAndNames);
        return configureRuntimeDependencies(minusFun);
    }

    public JkProjectFlatFacade addRuntimeDeps(@JkDepSuggest String... moduleDescriptors) {
        UnaryOperator<JkDependencySet> addFun = deps -> add(deps, moduleDescriptors);
        return configureRuntimeDependencies(addFun);
    }

    public JkProjectFlatFacade addTestDeps(@JkDepSuggest String... moduleDescriptors) {
        UnaryOperator<JkDependencySet> addFun = deps -> addFirst(deps, moduleDescriptors);
        return addTestDeps(addFun);
    }

    public JkProjectFlatFacade skipTests(boolean skipped) {
        project.testing.setSkipped(skipped);
        return this;
    }

    /**
     * Add specified dependencies at head of preset dependencies.
     */
    public JkProjectFlatFacade addTestDeps(Function<JkDependencySet, JkDependencySet> modifier) {
        return configureTestDependencies(deps -> deps.and(JkDependencySet.Hint.first(), modifier.apply(JkDependencySet.of())));
    }

    public JkProjectFlatFacade setVersionSupplier(Supplier<String> versionSupplier) {
        project.setVersionSupplier(() -> JkVersion.of(versionSupplier.get()));
        return this;
    }

    public JkProjectFlatFacade setVersion(String version) {
        return setVersionSupplier(() -> version);
    }

    /**
     * The published version will be computed according the current git tag.
     * @see JkGit#getVersionFromTag()
     */
    public JkProjectFlatFacade setVersionFromGitTag() {
        return setVersionSupplier(() -> JkGit.of(getProject().getBaseDir()).getVersionFromTag());
    }

    /**
     * The published version will be computed according the git last commit message.
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
     * Configures the dependencies to be published in a Maven repository.
     */
    public JkProjectFlatFacade configurePublishedDeps(Function<JkDependencySet, JkDependencySet> dependencyModifier) {
        project.mavenPublication.configureDependencies(dependencyModifier);
        return this;
    }

    /**
     * By default, every class in test folder are run. If you add an exclude filter,
     * tests accepting this filter won't be run.
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
     * @param condition : the filter will be added only if this parameter is <code>true</code>.
     */
    public JkProjectFlatFacade addTestIncludeFilterSuffixedBy(String suffix, boolean condition) {
        project.testing.testSelection.addIncludePatternsIf(condition, ".*" + suffix);
        return this;
    }

    /**
     * @see #addTestIncludeFilterSuffixedBy(String, boolean)
     * Adds a test include filters for test classes named as <code>^(Test.*|.+[.$]Test.*|.*Tests?)$</code>.
     * This is a standard filter in many tools.
     */
    public JkProjectFlatFacade addTestIncludeFilterOnStandardNaming(boolean condition) {
        project.testing.testSelection.addIncludePatternsIf(condition,
                JkTestSelection.STANDARD_INCLUDE_PATTERN);
       return this;
    }

    public JkProjectFlatFacade addSourceGenerator(JkProjectSourceGenerator sourceGenerator) {
        project.compilation.addSourceGenerator(sourceGenerator);
        return this;
    }

    public JkProject getProject() {
        return project;
    }

    private JkDependencySet add(JkDependencySet deps, String ... descriptors) {
        JkDependencySet result = deps;
        for (String descriptor : descriptors) {
            result = result.and(descriptor);
        }
        return result;
    }

    private JkDependencySet addFirst(JkDependencySet deps, String ... descriptors) {
        JkDependencySet result = deps;
        List<String> items = new LinkedList<>(Arrays.asList(descriptors));
        Collections.reverse(items);
        for (String descriptor : items) {
            result = result.and(JkDependencySet.Hint.first(), descriptor);
        }
        return result;
    }


    private JkDependencySet minus(JkDependencySet deps, String ... descriptors) {
        JkDependencySet result = deps;
        for (String descriptor : descriptors) {
            result = result.minus(descriptor);
        }
        return result;
    }

}
