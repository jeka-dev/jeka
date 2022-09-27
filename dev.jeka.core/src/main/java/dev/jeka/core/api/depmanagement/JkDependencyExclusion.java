package dev.jeka.core.api.depmanagement;

import java.util.Objects;
import java.util.Optional;

/**
 * A piece of information aiming at excluding transitive dependencies.
 *
 * @author Jerome Angibaud
 */
public final class JkDependencyExclusion {

    private final JkCoordinate.GroupAndName moduleId;

    private final JkCoordinate.JkArtifactSpecification artifactSpecification;

    private JkDependencyExclusion(JkCoordinate.GroupAndName moduleId,
                                  JkCoordinate.JkArtifactSpecification artifactSpecification) {
        super();
        this.moduleId = moduleId;
        this.artifactSpecification = artifactSpecification;
    }

    /**
     * Creates an exclusion of the specified module.
     */
    @SuppressWarnings("unchecked")
    public static JkDependencyExclusion of(JkCoordinate.GroupAndName moduleId) {
        return new JkDependencyExclusion(moduleId, null);
    }

    /**
     * Creates an exclusion of the specified module.
     */
    public static JkDependencyExclusion of(String group, String name) {
        return of(JkCoordinate.GroupAndName.of(group, name));
    }

    /**
     * Creates an exclusion of the specified module.
     */
    public static JkDependencyExclusion of(String groupAndName) {
        return of(JkCoordinate.GroupAndName.of(groupAndName));
    }

    public JkDependencyExclusion withClassierAndType(String classifier, String type) {
        return new JkDependencyExclusion(moduleId, JkCoordinate.JkArtifactSpecification.of(classifier, type));
    }

    public JkDependencyExclusion withType(String type) {
        return withClassierAndType(null, type);
    }

    public JkDependencyExclusion withClassifier(String classifier) {
        return withClassierAndType(classifier, null);
    }

    /**
     * Returns the module id to exclude.
     */
    public JkCoordinate.GroupAndName getModuleId() {
        return moduleId;
    }

    public String getClassifier() {
        return Optional.ofNullable(artifactSpecification).map(spec -> spec.getClassifier()).orElse(null);
    }

    public String getType() {
        return Optional.ofNullable(artifactSpecification).map(spec -> spec.getType()).orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JkDependencyExclusion that = (JkDependencyExclusion) o;
        if (!moduleId.equals(that.moduleId)) return false;
        return Objects.equals(artifactSpecification, that.artifactSpecification);
    }

    @Override
    public int hashCode() {
        int result = moduleId.hashCode();
        result = 31 * result + (artifactSpecification != null ? artifactSpecification.hashCode() : 0);
        return result;
    }
}
