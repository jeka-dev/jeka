package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.testing.JkTestSelection;

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
        project.getConstruction().getCompilation().setJavaVersion(version);
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
        project.getConstruction().getCompilation().setSourceEncoding(sourceEncoding);
        return this;
    }

    /**
     * Sets product Java source files and resources in "src".
     * Sets test Java source files and resources in "test".
     */
    public JkJavaProjectSimpleFacade setSimpleLayout() {
        project.getConstruction().getCompilation().getLayout().setSourceSimpleStyle(JkCompileLayout.Concern.PROD);
        project.getConstruction().getCompilation().getLayout().setSourceSimpleStyle(JkCompileLayout.Concern.TEST);
        return this;
    }

    public JkJavaProjectSimpleFacade addCompileDependencies(JkDependencySet dependencies) {
        project.getConstruction().getCompilation().addDependencies(dependencies);
        return this;
    }

    public JkJavaProjectSimpleFacade addTestDependencies(JkDependencySet dependencies) {
        project.getConstruction().getTesting().getCompilation().addDependencies(dependencies);
        return this;
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


    public JkJavaProjectSimpleFacade setPublishedVersion(Supplier<String> versionSupplier) {
        project.getPublication().setVersionSupplier(() -> JkVersion.of(versionSupplier.get()));
        return this;
    }

    public JkJavaProjectSimpleFacade setPublishedVersion(String version) {
        return setPublishedVersion(() -> version);
    }

    /**
     * @param moduleId group + artifactId to use when publishing on a binary repository.
     *                 Must be formatted as 'group:artifactId'
     */
    public JkJavaProjectSimpleFacade setPublishedModuleId(String moduleId) {
        project.getPublication().setModuleId(moduleId);
        return this;
    }

    public JkJavaProjectSimpleFacade setPublishedDependencies(
            Function<JkQualifiedDependencies, JkQualifiedDependencies> dependencyModifier) {
        project.getPublication().getMavenPublication().setDependencies(dependencyModifier);
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
