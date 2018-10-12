package org.jerkar.api.depmanagement;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jerkar.api.depmanagement.JkIvyPublication.Artifact;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Informations required to publish a module in an Ivy repository.
 *
 * @author Jerome Angibaud.
 */
public final class JkIvyPublication implements Iterable<Artifact>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a publication for a single artifact embodied by the specified file and
     * to published as the specified type and scopes. Here, scopes maps directly to
     * Ivy configurations (scope = configuration).
     */
    public static JkIvyPublication of(Path file, String type, JkScope... jkScopes) {
        return new JkIvyPublication(new HashSet<>()).and(file,
                type, jkScopes);
    }

    /**
     * Creates an Ivy publication from the specified artifact producer.
     */
    public static JkIvyPublication of(JkArtifactProducer artifactProducer) {
       JkIvyPublication result =  JkIvyPublication.of(artifactProducer.getArtifactPath(artifactProducer.getMainArtifactId()), JkJavaDepScopes.COMPILE);
       for (JkArtifactId extraFileId : artifactProducer.getArtifactIds()) {
            Path file = artifactProducer.getArtifactPath(extraFileId);
            result = result.andOptional(file, extraFileId.getClassifier(), scopeFor(extraFileId.getClassifier()));
       }
       return result;
    }

    private static JkScope scopeFor(String classifier) {
        if ("sources".equals(classifier)) {
            return JkJavaDepScopes.SOURCES;
        }
        if ("test".equals(classifier)) {
            return JkJavaDepScopes.TEST;
        }
        if ("test-sources".equals(classifier)) {
            return JkJavaDepScopes.SOURCES;
        }
        if ("javadoc".equals(classifier)) {
            return JkJavaDepScopes.JAVADOC;
        }
        return JkScope.of(classifier);
    }

    /**
     * Creates a publication for a single artifact embodied by the specified file and
     * to published for the specified scopes.
     * @see #of(Path, String, JkScope...)
     */
    public static JkIvyPublication of(Path file, JkScope... jkScopes) {
        return new JkIvyPublication(new HashSet<>()).and(file, jkScopes);
    }

    private final Set<Artifact> artifacts;

    private JkIvyPublication(Set<Artifact> artifacts) {
        super();
        this.artifacts = artifacts;
    }

    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact.
     * @see #of(Path, String, JkScope...)
     */
    public JkIvyPublication and(Path file, String type, JkScope... jkScopes) {
        return and(null, file, type, jkScopes);
    }

    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact and giving it the specified name (otherwise the value it the file of).
     * @see #of(Path, String, JkScope...)
     */
    public JkIvyPublication and(String name, Path file, String type, JkScope... jkScopes) {
        final Set<Artifact> artifacts = new HashSet<>(this.artifacts);
        artifacts.add(new Artifact(name, file, type, JkUtilsIterable.setOf(jkScopes)));
        return new JkIvyPublication(artifacts);
    }

    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact.
     */
    public JkIvyPublication and(Path file, JkScope... jkScopes) {
        return and(file, null, jkScopes);
    }

    /**
     * Same as {@link #and(Path, JkScope...)} but effective only if the specified file exists.
     */
    public JkIvyPublication andOptional(Path file, JkScope... jkScopes) {
        if (Files.exists(file)) {
            return and(file, null, jkScopes);
        }
        return this;
    }

    /**
     * Same as {@link #and(Path, String, JkScope...)} but effective only if the specified file
     * exists.
     */
    public JkIvyPublication andOptional(Path file, String type, JkScope... jkScopes) {
        if (Files.exists(file)) {
            return and(file, type, jkScopes);
        }
        return this;
    }




    @Override
    public Iterator<Artifact> iterator() {
        return this.artifacts.iterator();
    }

    static class Artifact implements Serializable {

        private static final long serialVersionUID = 1L;

        private Artifact(String name, Path file, String type, Set<JkScope> jkScopes) {
            super();
            this.file = file;
            this.extension = file.getFileName().toString().contains(".") ? JkUtilsString.substringAfterLast(
                    file.getFileName().toString(), ".") : null;
                    this.type = type;
                    this.jkScopes = jkScopes;
                    this.name = name;
        }

        public final Path file;

        public final String type;

        public final Set<JkScope> jkScopes;

        public final String name;

        public final String extension;

    }

    static class Status {

        public static final Status INTEGRATION = new Status("integration", true);

        public static final Status MILESTONE = new Status("milestone", false);

        public static final Status RELEASE = new Status("release", false);

        public static final Status of(String name) {
            return new Status(name, false);
        }

        public static final Status ofIntegration(String name) {
            return new Status(name, true);
        }

        private final String name;

        private final boolean integration;

        private Status(String name, boolean integration) {
            super();
            this.name = name;
            this.integration = integration;
        }

        public String name() {
            return name;
        }

        public boolean integration() {
            return integration;
        }

    }

}
