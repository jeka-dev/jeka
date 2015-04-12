package org.jerkar.publishing;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jerkar.depmanagement.JkScope;
import org.jerkar.publishing.JkIvyPublication.Artifact;
import org.jerkar.utils.JkUtilsIterable;

public class JkIvyPublication implements Iterable<Artifact> {

    public static JkIvyPublication of(File file, String type, JkScope ...jakeScopes) {
        return new JkIvyPublication(new HashSet<JkIvyPublication.Artifact>(), null, null).and(file, type, jakeScopes);
    }

    public static JkIvyPublication of(File file, JkScope...jakeScopes) {
        return new JkIvyPublication(new HashSet<JkIvyPublication.Artifact>(), null, null).and(file, jakeScopes);
    }

    private final Set<Artifact> artifacts;

    public final Status status;

    public final String branch;

    private JkIvyPublication(Set<Artifact> artifacts, Status status, String branch) {
        super();
        this.artifacts = artifacts;
        this.status = status;
        this.branch = branch;
    }

    public JkIvyPublication and(File file, String type, JkScope...jakeScopes) {
        final Set<Artifact> artifacts = new HashSet<JkIvyPublication.Artifact>(this.artifacts);
        artifacts.add(new Artifact(file, type, JkUtilsIterable.setOf(jakeScopes)));
        return new JkIvyPublication(artifacts, this.status, this.branch);
    }

    public JkIvyPublication and(File file, JkScope... jakeScopes) {
        return and(file, null, jakeScopes );
    }

    public JkIvyPublication andIf(boolean condition, File file, JkScope... jakeScopes) {
        if (condition) {
            return and(file, jakeScopes);
        }
        return this;
    }

    public JkIvyPublication andIf(boolean condition, File file, String type, JkScope... jakeScopes) {
        if (condition) {
            return and(file, type, jakeScopes);
        }
        return this;
    }


    public JkIvyPublication andOptional(File file, JkScope... jakeScopes) {
        if (file.exists()) {
            return and(file, null, jakeScopes );
        }
        return this;
    }

    public JkIvyPublication andOptional(File file, String type, JkScope... jakeScopes) {
        if (file.exists()) {
            return and(file, type, jakeScopes);
        }
        return this;
    }


    public JkIvyPublication andOptionalIf(boolean condition, File file, JkScope... jakeScopes) {
        if (condition) {
            return andOptional(file, jakeScopes);
        }
        return this;
    }

    public JkIvyPublication andOptionalIf(boolean condition, File file, String type, JkScope... jakeScopes) {
        if (condition) {
            return andOptional(file, type, jakeScopes);
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

    public static class Artifact {

        private Artifact(File file, String type, Set<JkScope> jakeScopes) {
            super();
            this.file = file;
            this.type = type;
            this.jakeScopes = jakeScopes;
        }

        public final File file;

        public final String type;

        public final Set<JkScope> jakeScopes;

    }

    public static class Status {

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
