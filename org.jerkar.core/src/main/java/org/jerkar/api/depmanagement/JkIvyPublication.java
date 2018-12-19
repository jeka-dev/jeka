package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.jerkar.api.depmanagement.JkIvyPublication.Artifact;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Information required to publish a module in an Ivy repository.
 *
 * @author Jerome Angibaud.
 */
public final class JkIvyPublication implements Iterable<Artifact>, Serializable {

    private static final long serialVersionUID = 1L;

    private final Set<Artifact> artifacts;

    private JkIvyPublication(Set<Artifact> artifacts) {
        super();
        this.artifacts = artifacts;
    }


    /**
     * Creates a publication for a single artifact embodied by the specified file and
     * to published as the specified type and scopes. Here, scopes maps directly to
     * Ivy configurations (scope = configuration).
     */
    public static JkIvyPublication ofType(Path file, String type, String... scopes) {
        return new JkIvyPublication(new HashSet<>()).and(file,
                type, scopes);
    }

    /**
     * Creates an Ivy publication from the specified artifact producer.
     */
    public static JkIvyPublication of(JkArtifactProducer artifactProducer) {
       JkIvyPublication result =  JkIvyPublication.of(artifactProducer.getArtifactPath(artifactProducer.getMainArtifactId()),
               JkJavaDepScopes.COMPILE.getName());
       for (JkArtifactId extraFileId : artifactProducer.getArtifactIds()) {
            Path file = artifactProducer.getArtifactPath(extraFileId);
            result = result.andOptional(file, extraFileId.getClassifier(), scopeFor(extraFileId.getClassifier()));
       }
       return result;
    }

    private static String scopeFor(String classifier) {
        if ("sources".equals(classifier)) {
            return JkJavaDepScopes.SOURCES.getName();
        }
        if ("test".equals(classifier)) {
            return JkJavaDepScopes.TEST.getName();
        }
        if ("test-sources".equals(classifier)) {
            return JkJavaDepScopes.SOURCES.getName();
        }
        if ("javadoc".equals(classifier)) {
            return JkJavaDepScopes.JAVADOC.getName();
        }
        return classifier;
    }

    /**
     * Creates a publication for a single artifact embodied by the specified file and
     * to published for the specified scopes.
     * @see #ofType(Path, String, String...)
     */
    public static JkIvyPublication of(Path file, String... scopes) {
        return new JkIvyPublication(new HashSet<>()).and(file, scopes);
    }


    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact.
     * @see #ofType(Path, String, String...)
     */
    public JkIvyPublication and(Path file, String type, String... scopes) {
        return and(null, file, type, scopes);
    }

    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact and giving it the specified name (otherwise the value it the file of).
     * @see #ofType(Path, String, String...)
     */
    public JkIvyPublication and(String name, Path file, String type, String... scopes) {
        final Set<Artifact> artifacts = new HashSet<>(this.artifacts);
        artifacts.add(new Artifact(name, file, type, JkUtilsIterable.setOf(scopes).stream().map(JkScope::of).collect(Collectors.toSet())));
        return new JkIvyPublication(artifacts);
    }

    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact.
     */
    public JkIvyPublication and(Path file, String... scopes) {
        return and(file, null, scopes);
    }

    /**
     * Same as {@link #and(Path, String...)} but effective only if the specified file exists.
     */
    public JkIvyPublication andOptional(Path file, String... scopes) {
        if (Files.exists(file)) {
            return and(file, null, scopes);
        }
        return this;
    }

    /**
     * Same as {@link #and(Path, String, String...)} but effective only if the specified file
     * exists.
     */
    public JkIvyPublication andOptionalWithType(Path file, String type, String... scopes) {
        if (Files.exists(file)) {
            return and(file, type, scopes);
        }
        return this;
    }

    @Override
    public Iterator<Artifact> iterator() {
        return this.artifacts.iterator();
    }

    static class Artifact implements Serializable {

        private static final long serialVersionUID = 1L;

        private Artifact(String name, Path path, String type, Set<JkScope> jkScopes) {
            super();
            this.file = path.toFile();
            this.extension = path.getFileName().toString().contains(".") ? JkUtilsString.substringAfterLast(
                    path.getFileName().toString(), ".") : null;
                    this.type = type;
                    this.jkScopes = jkScopes;
                    this.name = name;
        }

        public final File file;  // path not serializable

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
