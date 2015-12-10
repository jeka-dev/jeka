package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
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
    public static JkIvyPublication of(File file, String type, JkScope... jkScopes) {
        return new JkIvyPublication(new HashSet<JkIvyPublication.Artifact>()).and(file,
                type, jkScopes);
    }

    /**
     * Creates a publication for a single artifact embodied by the specified file and
     * to published for the specified scopes.
     * @see #of(File, String, JkScope...)
     */
    public static JkIvyPublication of(File file, JkScope... jkScopes) {
        return new JkIvyPublication(new HashSet<JkIvyPublication.Artifact>()).and(file,
                jkScopes);
    }

    private final Set<Artifact> artifacts;

    private JkIvyPublication(Set<Artifact> artifacts) {
        super();
        this.artifacts = artifacts;
    }

    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact.
     * @see #of(File, String, JkScope...)
     */
    public JkIvyPublication and(File file, String type, JkScope... jkScopes) {
        return and(null, file, type, jkScopes);
    }

    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact and giving it the specified name (otherwise the name it the file name).
     * @see #of(File, String, JkScope...)
     */
    public JkIvyPublication and(String name, File file, String type, JkScope... jkScopes) {
        final Set<Artifact> artifacts = new HashSet<JkIvyPublication.Artifact>(this.artifacts);
        artifacts.add(new Artifact(name, file, type, JkUtilsIterable.setOf(jkScopes)));
        return new JkIvyPublication(artifacts);
    }

    /**
     * Returns a {@link JkIvyPublication} identical to this one but adding the specified
     * artifact.
     * @see #andIf(boolean, File, String, JkScope...)
     */
    public JkIvyPublication and(File file, JkScope... jkScopes) {
        return and(file, null, jkScopes);
    }

    /**
     * Same as {@link #and(File, JkScope...)} but effective only if the specified condition
     * is <code>true</code>.
     */
    public JkIvyPublication andIf(boolean condition, File file, JkScope... jkScopes) {
        if (condition) {
            return and(file, jkScopes);
        }
        return this;
    }

    /**
     * Same as {@link #and(File, String, JkScope...)} but effective only if the specified condition
     * is <code>true</code>.
     */
    public JkIvyPublication andIf(boolean condition, File file, String type, JkScope... jkScopes) {
        if (condition) {
            return and(file, type, jkScopes);
        }
        return this;
    }

    /**
     * Same as {@link #and(File, JkScope...)} but effective only if the specified file exists.
     */
    public JkIvyPublication andOptional(File file, JkScope... jkScopes) {
        if (file.exists()) {
            return and(file, null, jkScopes);
        }
        return this;
    }

    /**
     * Same as {@link #and(File, String, JkScope...)} but effective only if the specified file
     * exists.
     */
    public JkIvyPublication andOptional(File file, String type, JkScope... jkScopes) {
        if (file.exists()) {
            return and(file, type, jkScopes);
        }
        return this;
    }

    /**
     * Same as {@link #andOptional(File, JkScope...)} but effective only if the specified condition is true.
     */
    public JkIvyPublication andOptionalIf(boolean condition, File file, JkScope... jkScopes) {
        if (condition) {
            return andOptional(file, jkScopes);
        }
        return this;
    }

    /**
     * Same as {@link #andOptional(File, type, JkScope...)} but effective only if the specified condition is true.
     */
    public JkIvyPublication andOptionalIf(boolean condition, File file, String type,
            JkScope... jkScopes) {
        if (condition) {
            return andOptional(file, type, jkScopes);
        }
        return this;
    }

    @Override
    public Iterator<Artifact> iterator() {
        return this.artifacts.iterator();
    }

    static class Artifact implements Serializable {

        private static final long serialVersionUID = 1L;

        private Artifact(String name, File file, String type, Set<JkScope> jkScopes) {
            super();
            this.file = file;
            this.extension = file.getName().contains(".") ? JkUtilsString.substringAfterLast(
                    file.getName(), ".") : null;
                    this.type = type;
                    this.jkScopes = jkScopes;
                    this.name = name;
        }

        public final File file;

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
