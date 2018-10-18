package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.system.JkException;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.jerkar.api.java.project.JkJavaProjectMaker.*;

public class JkJavaProjectPublishTasks {

    private final JkJavaProjectMaker maker;

    private JkRepoSet publishRepos = JkRepoSet.ofLocal();

    private final Set<JkArtifactId> unpublishedArtifactIds = new LinkedHashSet<>();

    JkJavaProjectPublishTasks(JkJavaProjectMaker maker) {
        this.maker = maker;
    }

    public void publish() {
        final JkPublisher publisher = JkPublisher.of(this.publishRepos);
        if (publisher.hasMavenPublishRepo()) {
            publishMaven();
        }
        if (publisher.hasIvyPublishRepo()) {
            publishIvy();
        }
    }

    public void publishMaven() {
        JkException.throwIf(maker.project.getVersionedModule() == null, "No versionedModule has been set on "
                + maker.project + ". Can't publish.");
        JkPublisher.of(publishRepos, maker.getOutLayout().outputPath())
                .publishMaven(maker.project.getVersionedModule(), maker, unpublishedArtifactIds,
                        maker.getDefaultedDependencies(), maker.project.getMavenPublicationInfo());
    }

    public void publishIvy() {
        JkException.throwIf(maker.project.getVersionedModule() == null, "No versionedModule has been set on "
                + maker.project + ". Can't publish.");
        final JkDependencySet dependencies = maker.getDefaultedDependencies();
        final JkIvyPublication publication = JkIvyPublication.of(maker.getMainArtifactPath(), JkJavaDepScopes.COMPILE)
                .andOptional(maker.getArtifactPath(SOURCES_ARTIFACT_ID), JkJavaDepScopes.SOURCES)
                .andOptional(maker.getArtifactPath(JAVADOC_ARTIFACT_ID), JkJavaDepScopes.JAVADOC)
                .andOptional(maker.getArtifactPath(TEST_ARTIFACT_ID), JkJavaDepScopes.TEST)
                .andOptional(maker.getArtifactPath(TEST_SOURCE_ARTIFACT_ID), JkJavaDepScopes.SOURCES);
        final JkVersionProvider resolvedVersions = maker.getDependencyResolver()
                .resolve(dependencies, dependencies.getInvolvedScopes()).getResolvedVersionProvider();
        JkPublisher.of(publishRepos, maker.getOutLayout().outputPath())
                .publishIvy(maker.project.getVersionedModule(), publication, dependencies,
                        JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, Instant.now(), resolvedVersions);
    }

    public JkRepoSet getPublishRepos() {
        return this.publishRepos;
    }

    public JkJavaProjectPublishTasks setPublishRepos(JkRepoSet publishRepos) {
        this.publishRepos = publishRepos;
        return this;
    }

    public Set<JkArtifactId> getUnpublishedArtifactIds() {
        return unpublishedArtifactIds;
    }

}
