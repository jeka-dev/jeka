package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.JkGitProcess;

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



    /**
     * Sets product Java source files and resources in "src".
     * Sets test Java source files and resources in "test".
     */
    public JkProjectFlatFacade useSimpleLayout() {
        project.prodCompilation.layout.setSourceSimpleStyle(JkCompileLayout.Concern.PROD);
        project.testing.testCompilation.layout
                .setSourceSimpleStyle(JkCompileLayout.Concern.TEST);
        return this;
    }

    /**
     * The resources will be located in same dirs than sources.
     */
    public JkProjectFlatFacade mixResourcesAndSources() {
        project.prodCompilation.layout.mixResourcesAndSources();
        project.testing.testCompilation.layout.mixResourcesAndSources();
        return this;
    }

    public JkProjectFlatFacade includeJavadocAndSources(boolean includeJavaDoc, boolean includeSources) {
        project.includeJavadocAndSources(includeJavaDoc, includeSources);
        return this;
    }

    public JkProjectFlatFacade configureCompileDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        project.prodCompilation.configureDependencies(modifier);
        return this;
    }

    public JkProjectFlatFacade configureRuntimeDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        project.packaging.configureRuntimeDependencies(modifier);
        return this;
    }

    public JkProjectFlatFacade configureTestDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        project.testing.testCompilation.configureDependencies(modifier);
        return this;
    }

    public JkProjectFlatFacade addCompileDeps(@JkDepSuggest String... moduleDescriptors) {
        UnaryOperator<JkDependencySet> addFun = deps -> add(deps, moduleDescriptors);
        return configureCompileDependencies(addFun);
    }

    public JkProjectFlatFacade addCompileOnlyDeps(@JkDepSuggest String... moduleDescriptors) {
        UnaryOperator<JkDependencySet> addFun = deps -> add(deps, moduleDescriptors);
        configureCompileDependencies(addFun);
        UnaryOperator<JkDependencySet> minusFun = deps -> minus(deps, moduleDescriptors);
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

    public JkProjectFlatFacade setPublishedVersion(Supplier<String> versionSupplier) {
        project.publication.setVersion(versionSupplier);
        return this;
    }

    public JkProjectFlatFacade setPublishedVersion(String version) {
        return setPublishedVersion(() -> version);
    }

    /**
     * The published version will be computed according the current git tag.
     * @see JkGitProcess#getVersionFromTag()
     */
    public JkProjectFlatFacade setPublishedVersionFromGitTag() {
        return setPublishedVersion(() -> JkGitProcess.of(getProject().getBaseDir()).getVersionFromTag());
    }

    /**
     * The published version will be computed according the git last commit message.
     * @see JkGitProcess#getVersionFromCommitMessage(String)
     */
    public JkProjectFlatFacade setPublishedVersionFromGitTagCommitMessage(String suffixKeyword) {
        return setPublishedVersion(() -> JkGitProcess.of(getProject().getBaseDir())
                .getVersionFromCommitMessage(suffixKeyword));
    }

    /**
     * @param moduleId group + artifactId to use when publishing on a binary repository.
     *                 Must be formatted as 'group:artifactId'
     */
    public JkProjectFlatFacade setPublishedModuleId(String moduleId) {
        project.publication.setModuleId(moduleId);
        return this;
    }

    /**
     * Configures the dependencies to be published in a Maven repository.
     */
    public JkProjectFlatFacade configurePublishedDeps(Function<JkDependencySet, JkDependencySet> dependencyModifier) {
        project.publication.maven.configureDependencies(dependencyModifier);
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
