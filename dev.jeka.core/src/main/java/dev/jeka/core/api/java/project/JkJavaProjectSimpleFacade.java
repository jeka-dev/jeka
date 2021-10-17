package dev.jeka.core.api.java.project;

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
 * Simple facade over {@link JkJavaProject} to access common setting conveniently.
 */
public class JkJavaProjectSimpleFacade {

    private final JkJavaProject project;

    JkJavaProjectSimpleFacade(JkJavaProject project) {
        this.project = project;
    }

    public JkJavaProjectSimpleFacade setJavaVersion(JkJavaVersion version) {
        project.getConstruction().setJvmTargetVersion(version);
        return this;
    }

    public JkJavaProjectSimpleFacade applyOnProject(Consumer<JkJavaProject> projectConsumer) {
        project.apply(projectConsumer);
        return this;
    }

    public JkJavaProjectSimpleFacade apply(Consumer<JkJavaProjectSimpleFacade> facadeConsumer) {
        facadeConsumer.accept(this);
        return this;
    }

    public JkJavaProjectSimpleFacade setBaseDir(String path) {
        project.setBaseDir(Paths.get(path));
        return this;
    }

    public JkJavaProjectSimpleFacade setBaseDir(Path path) {
        project.setBaseDir(path);
        return this;
    }

    public JkJavaProjectSimpleFacade setJavaSourceEncoding(String sourceEncoding) {
        project.getConstruction().setSourceEncoding(sourceEncoding);
        return this;
    }

    /**
     * Sets product Java source files and resources in "src".
     * Sets test Java source files and resources in "test".
     */
    public JkJavaProjectSimpleFacade setSimpleLayout() {
        project.getConstruction().getCompilation().getLayout().setSourceSimpleStyle(JkCompileLayout.Concern.PROD);
        project.getConstruction().getTesting().getCompilation().getLayout()
                .setSourceSimpleStyle(JkCompileLayout.Concern.TEST);
        return this;
    }

    /**
     * The resources will be located in same dirs than sources.
     */
    public JkJavaProjectSimpleFacade mixResourcesAndSources() {
        project.getConstruction().getCompilation().getLayout().mixResourcesAndSources();
        project.getConstruction().getTesting().getCompilation().getLayout().mixResourcesAndSources();
        return this;
    }

    public JkJavaProjectSimpleFacade setCompileDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        project.getConstruction().getCompilation().setDependencies(modifier);
        return this;
    }

    public JkJavaProjectSimpleFacade setTestDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        project.getConstruction().getTesting().getCompilation().setDependencies(modifier);
        return this;
    }

    /**
     * Add specified dependencies at head of preset dependencies.
     */
    public JkJavaProjectSimpleFacade addTestDependencies(Function<JkDependencySet, JkDependencySet> modifier) {
        return setTestDependencies(deps -> deps.and(JkDependencySet.Hint.first(), modifier.apply(JkDependencySet.of())));
    }

    /**
     * Specify the dependencies to add or remove from the production compilation dependencies to
     * get the runtime dependencies.
     * @param modifier An function that define the runtime dependencies from the compilation ones.
     */
    public JkJavaProjectSimpleFacade setRuntimeDependencies(UnaryOperator<JkDependencySet> modifier) {
        project.getConstruction().setRuntimeDependencies(modifier);
        return this;
    }


    public JkJavaProjectSimpleFacade setPublishedMavenVersion(Supplier<String> versionSupplier) {
        project.getPublication().getMaven().setVersion(versionSupplier);
        return this;
    }

    public JkJavaProjectSimpleFacade setPublishedMavenVersion(String version) {
        return setPublishedMavenVersion(() -> version);
    }

    /**
     * The published version will be computed according the current git tag.
     * @see JkGitProcess#getVersionFromTag()
     */
    public JkJavaProjectSimpleFacade setPublishedMavenVersionFromGitTag() {
        return setPublishedMavenVersion(() -> JkGitProcess.of(getProject().getBaseDir()).getVersionFromTag());
    }

    /**
     * The published version will be computed according the git last commit message.
     * @see JkGitProcess#getVersionFromCommitMessage(String)
     */
    public JkJavaProjectSimpleFacade setPublishedVersionFromGitTagCommitMessage(String suffixKeyword) {
        return setPublishedMavenVersion(() -> JkGitProcess.of(getProject().getBaseDir())
                .getVersionFromCommitMessage(suffixKeyword));
    }

    /**
     * @param moduleId group + artifactId to use when publishing on a binary repository.
     *                 Must be formatted as 'group:artifactId'
     */
    public JkJavaProjectSimpleFacade setPublishedMavenModuleId(String moduleId) {
        project.getPublication().getMaven().setModuleId(moduleId);
        return this;
    }

    public JkJavaProjectSimpleFacade setPublishedDependencies(
            Function<JkDependencySet, JkDependencySet> dependencyModifier) {
        project.getPublication().getMaven().setDependencies(dependencyModifier);
        return this;
    }

    /**
     * By default, every classes in test folder are run. If you add a exclude filter,
     * tests accepting this filter won't be run.
     * @param condition : the filter will be added only if this parameter is <code>true</code>.
     */
    public JkJavaProjectSimpleFacade addTestExcludeFilterSuffixedBy(String suffix, boolean condition) {
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
    public JkJavaProjectSimpleFacade addTestIncludeFilterSuffixedBy(String suffix, boolean condition) {
        project.getConstruction().getTesting().getTestSelection().addIncludePatternsIf(condition, ".*" + suffix);
        return this;
    }

    /**
     * @see #addTestIncludeFilterSuffixedBy(String, boolean)
     * Adds a test include filters for test classes named as <code>^(Test.*|.+[.$]Test.*|.*Tests?)$</code>.
     * This is a standard filter in many tools.
     */
    public JkJavaProjectSimpleFacade addTestIncludeFilterOnStandardNaming(boolean condition) {
        project.getConstruction().getTesting().getTestSelection().addIncludePatternsIf(condition,
                JkTestSelection.STANDARD_INCLUDE_PATTERN);
       return this;
    }

    public JkJavaProject getProject() {
        return project;
    }

}
