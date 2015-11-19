package org.jerkar.api.depmanagement;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jerkar.api.depmanagement.JkIvyPublication.Artifact;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;

public final class JkIvyPublication implements Iterable<Artifact>, Serializable {

    private static final long serialVersionUID = 1L;

    public static JkIvyPublication of(File file, String type, JkScope... jkScopes) {
        return new JkIvyPublication(new HashSet<JkIvyPublication.Artifact>(), null, null).and(file,
                type, jkScopes);
    }

    public static JkIvyPublication of(File file, JkScope... jkScopes) {
        return new JkIvyPublication(new HashSet<JkIvyPublication.Artifact>(), null, null).and(file,
                jkScopes);
    }

    private final Set<Artifact> artifacts;

    private final Status status;

    private final String branch;

    private JkIvyPublication(Set<Artifact> artifacts, Status status, String branch) {
        super();
        this.artifacts = artifacts;
        this.status = status;
        this.branch = branch;
    }

    public Status status() {
        return status;
    }

    public String branch() {
        return branch;
    }

    public JkIvyPublication and(File file, String type, JkScope... jkScopes) {
        return and(null, file, type, jkScopes);
    }

    public JkIvyPublication and(String name, File file, String type, JkScope... jkScopes) {
        final Set<Artifact> artifacts = new HashSet<JkIvyPublication.Artifact>(this.artifacts);
        artifacts.add(new Artifact(name, file, type, JkUtilsIterable.setOf(jkScopes)));
        return new JkIvyPublication(artifacts, this.status, this.branch);
    }

    public JkIvyPublication and(File file, JkScope... jkScopes) {
        return and(file, null, jkScopes);
    }

    public JkIvyPublication andIf(boolean condition, File file, JkScope... jkScopes) {
        if (condition) {
            return and(file, jkScopes);
        }
        return this;
    }

    public JkIvyPublication andIf(boolean condition, File file, String type, JkScope... jkScopes) {
        if (condition) {
            return and(file, type, jkScopes);
        }
        return this;
    }

    public JkIvyPublication andOptional(File file, JkScope... jkScopes) {
        if (file.exists()) {
            return and(file, null, jkScopes);
        }
        return this;
    }

    public JkIvyPublication andOptional(File file, String type, JkScope... jkScopes) {
        if (file.exists()) {
            return and(file, type, jkScopes);
        }
        return this;
    }

    public JkIvyPublication andOptionalIf(boolean condition, File file, JkScope... jkScopes) {
        if (condition) {
            return andOptional(file, jkScopes);
        }
        return this;
    }

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

    public JkIvyPublication status(Status status) {
        return new JkIvyPublication(this.artifacts, status, this.branch);
    }

    public JkIvyPublication branch(String branch) {
        return new JkIvyPublication(this.artifacts, this.status, branch);
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
