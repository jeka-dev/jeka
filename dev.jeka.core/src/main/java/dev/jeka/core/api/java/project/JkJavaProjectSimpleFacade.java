package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.testing.JkTestSelection;

import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Supplier;

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

    public JkJavaProjectSimpleFacade setBaseDir(String path) {
        project.setBaseDir(Paths.get(path));
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

    public JkJavaProjectSimpleFacade addDependencies(JkDependencySet dependencies) {
        project.getConstruction().getDependencyManagement().addDependencies(dependencies);
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
            Function<JkDependencySet, JkDependencySet> dependencyModifier) {
        project.getPublication().getMavenPublication().setDependencies(dependencyModifier);
        return this;
    }

    public JkJavaProjectSimpleFacade includeTestSuffixedByIT(boolean include) {
        project.getConstruction().getTesting().getTestSelection()
                .addIncludePatternsIf(include, JkTestSelection.IT_INCLUDE_PATTERN);
        return this;
    }

    public JkJavaProject getProject() {
        return project;
    }

}
