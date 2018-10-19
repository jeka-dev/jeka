package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.system.JkException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.jerkar.api.java.project.JkJavaProjectMaker.*;

public class JkJavaProjectPublishTasks {

    private final JkJavaProjectMaker maker;

    private JkRepoSet publishRepos = JkRepoSet.ofLocal();

    private final Set<JkArtifactId> unpublishedArtifactIds = new LinkedHashSet<>();

    private UnaryOperator<Path> signer;

    JkJavaProjectPublishTasks(JkJavaProjectMaker maker) {
        this.maker = maker;
    }

    /**
     * Publishes all artifacts produced by the project maker expects those defined in {@link #unpublishedArtifactIds}
     * @param localOnly If true, the publication occurs in local repository only.
     */
    public void publish(boolean localOnly) {
        final JkPublisher publisher = JkPublisher.of(this.publishRepos);
        if (publisher.hasMavenPublishRepo()) {
            publishMaven(localOnly);
        }
        if (publisher.hasIvyPublishRepo()) {
            publishIvy(localOnly);
        }
    }

    public void publishMaven(boolean localOnly) {
        JkJavaProject project = maker.project;
        JkException.throwIf(project.getVersionedModule() == null, "No versionedModule has been set on "
                + project + ". Can't publish.");
        JkRepoSet repos = localOnly ? JkRepo.ofLocal().toSet() : publishRepos;
        JkMavenPublication publication = JkMavenPublication.of(maker, unpublishedArtifactIds)
                .with(project.getMavenPublicationInfo());
        JkPublisher.of(repos, maker.getOutLayout().outputPath())
                .publishMaven(project.getVersionedModule(), publication, maker.getDefaultedDependencies());
    }

    public void publishIvy(boolean localOnly) {
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
        JkRepoSet repos = localOnly ? JkRepo.ofLocal().toSet() : publishRepos;
        JkPublisher.of(repos, maker.getOutLayout().outputPath())
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

    public void setSigner(UnaryOperator<Path> signer) {
        this.signer = signer;
    }
}
