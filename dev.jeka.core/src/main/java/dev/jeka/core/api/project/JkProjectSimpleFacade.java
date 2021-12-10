package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.testing.JkTestSelection;
import dev.jeka.core.api.tooling.JkGitProcess;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Simple facade over {@link JkProject} to access common setting conveniently.
 */
public class JkProjectSimpleFacade {

    private final JkProject project;

    JkProjectSimpleFacade(JkProject project) {
        this.project = project;
    }

    public JkProjectSimpleFacade setJvmTargetVersion(JkJavaVersion version) {
        project.getConstruction().setJvmTargetVersion(version);
        return this;
    }

    public JkProjectSimpleFacade applyOnProject(Consumer<JkProject> projectConsumer) {
        project.apply(projectConsumer);
        return this;
    }

    public JkProjectSimpleFacade apply(Consumer<JkProjectSimpleFacade> facadeConsumer) {
        facadeConsumer.accept(this);
        return this;
    }

    public JkProjectSimpleFacade setBaseDir(String path) {
        project.setBaseDir(Paths.get(path));
        return this;
    }

    public JkProjectSimpleFacade setBaseDir(Path path) {
        project.setBaseDir(path);
        return this;
    }

    public JkProjectSimpleFacade setJavaSourceEncoding(String sourceEncoding) {
        project.getConstruction().setSourceEncoding(sourceEncoding);
        return this;
    }

    /**
     * Sets product Java source files and resources in "src".
     * Sets test Java source files and resources in "test".
     */
    public JkProjectSimpleFacade useSimpleLayout() {
        project.getConstruction().getCompilation().getLayout().setSourceSimpleStyle(JkCompileLayout.Concern.PROD);
        project.getConstruction().getTesting().getCompilation().getLayout()
                .setSourceSimpleStyle(JkCompileLayout.Concern.TEST);
        return this;
    }

    /**
     * The resources will be located in same dirs than sources.
     */
    public JkProjectSimpleFacade mixResourcesAndSources() {
        project.getConstruction().getCompilation().getLayout().mixResourcesAndSources();
        project.getConstruction().getTesting().getCompilation().getLayout().mixResourcesAndSources();
        return this;
    }

    public JkProjectSimpleFacade configureCompileDeps(Function<JkDependencySet, JkDependencySet> modifier) {
        project.getConstruction().getCompilation().configureDependencies(modifier);
        return this;
    }

    public JkProjectSimpleFacade includeJavadocAndSources(boolean includeJavaDoc, boolean includeSources) {
        project.includeJavadocAndSources(includeJavaDoc, includeSources);
        return this;
    }

    public JkProjectSimpleFacade configureTestDeps(Function<JkDependencySet, JkDependencySet> modifier) {
        project.getConstruction().getTesting().getCompilation().configureDependencies(modifier);
        return this;
    }

    public JkProjectSimpleFacade skipTests(boolean skipped) {
        project.getConstruction().getTesting().setSkipped(skipped);
        return this;
    }

    /**
     * Add specified dependencies at head of preset dependencies.
     */
    public JkProjectSimpleFacade addTestDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        return configureTestDeps(deps -> deps.and(JkDependencySet.Hint.first(), modifier.apply(JkDependencySet.of())));
    }

    /**
     * Specify the dependencies to add or remove from the production compilation dependencies to
     * get the runtime dependencies.
     * @param modifier An function that define the runtime dependencies from the compilation ones.
     */
    public JkProjectSimpleFacade configureRuntimeDeps(UnaryOperator<JkDependencySet> modifier) {
        project.getConstruction().configureRuntimeDependencies(modifier);
        return this;
    }


    public JkProjectSimpleFacade setPublishedVersion(Supplier<String> versionSupplier) {
        project.getPublication().setVersion(versionSupplier);
        return this;
    }

    public JkProjectSimpleFacade setPublishedVersion(String version) {
        return setPublishedVersion(() -> version);
    }

    /**
     * The published version will be computed according the current git tag.
     * @see JkGitProcess#getVersionFromTag()
     */
    public JkProjectSimpleFacade setPublishedVersionFromGitTag() {
        return setPublishedVersion(() -> JkGitProcess.of(getProject().getBaseDir()).getVersionFromTag());
    }

    /**
     * The published version will be computed according the git last commit message.
     * @see JkGitProcess#getVersionFromCommitMessage(String)
     */
    public JkProjectSimpleFacade setPublishedVersionFromGitTagCommitMessage(String suffixKeyword) {
        return setPublishedVersion(() -> JkGitProcess.of(getProject().getBaseDir())
                .getVersionFromCommitMessage(suffixKeyword));
    }

    /**
     * @param moduleId group + artifactId to use when publishing on a binary repository.
     *                 Must be formatted as 'group:artifactId'
     */
    public JkProjectSimpleFacade setPublishedModuleId(String moduleId) {
        project.getPublication().setModuleId(moduleId);
        return this;
    }

    /**
     * Configures the dependencies to be published in a Maven repository.
     */
    public JkProjectSimpleFacade configurePublishedDeps(
            Function<JkDependencySet, JkDependencySet> dependencyModifier) {
        project.getPublication().getMaven().configureDependencies(dependencyModifier);
        return this;
    }

    /**
     * By default, every class in test folder are run. If you add a exclude filter,
     * tests accepting this filter won't be run.
     * @param condition : the filter will be added only if this parameter is <code>true</code>.
     */
    public JkProjectSimpleFacade addTestExcludeFilterSuffixedBy(String suffix, boolean condition) {
        if (condition) {
            project.getConstruction().getTesting().getTestSelection().addExcludePatterns(".*" + suffix);
        }
        return this;
    }

    /**
     * By default, every classes in test folder are run. If you add an include filter, only
     * tests accepting one of the declared filters will run.
     * @param condition : the filter will be added only if this parameter is <code>true</code>.
     */
    public JkProjectSimpleFacade addTestIncludeFilterSuffixedBy(String suffix, boolean condition) {
        project.getConstruction().getTesting().getTestSelection().addIncludePatternsIf(condition, ".*" + suffix);
        return this;
    }

    /**
     * @see #addTestIncludeFilterSuffixedBy(String, boolean)
     * Adds a test include filters for test classes named as <code>^(Test.*|.+[.$]Test.*|.*Tests?)$</code>.
     * This is a standard filter in many tools.
     */
    public JkProjectSimpleFacade addTestIncludeFilterOnStandardNaming(boolean condition) {
        project.getConstruction().getTesting().getTestSelection().addIncludePatternsIf(condition,
                JkTestSelection.STANDARD_INCLUDE_PATTERN);
       return this;
    }

    public JkProject getProject() {
        return project;
    }

}
