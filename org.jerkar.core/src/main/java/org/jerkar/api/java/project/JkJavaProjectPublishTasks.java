package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.system.JkException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.function.UnaryOperator;

import static org.jerkar.api.java.project.JkJavaProjectMaker.*;

public class JkJavaProjectPublishTasks {

    private final JkJavaProjectMaker maker;

    private JkRepoSet publishRepos = JkRepoSet.ofLocal();

    private UnaryOperator<Path> signer;

    JkJavaProjectPublishTasks(JkJavaProjectMaker maker) {
        this.maker = maker;
    }

    /**
     * Publishes all defined artifacts.
     */
    public void publish() {
        publishMaven(this.publishRepos);
        publishIvy();
    }

    public void publishLocal() {
        publishMaven(JkRepo.ofLocal().toSet());
    }

    private void publishMaven(JkRepoSet repos) {
        JkJavaProject project = maker.project;
        JkException.throwIf(project.getVersionedModule() == null, "No versionedModule has been set on "
                + project + ". Can't publish.");
        JkMavenPublication publication = JkMavenPublication.of(maker, Collections.emptySet())
                .with(project.getMavenPublicationInfo());
        JkPublisher.of(repos, maker.getOutLayout().getOutputPath())
                .publishMaven(project.getVersionedModule(), publication, maker.getDefaultedDependencies());
    }

    private void publishIvy() {
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
        JkPublisher.of(publishRepos, maker.getOutLayout().getOutputPath())
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

    public void setSigner(UnaryOperator<Path> signer) {
        this.signer = signer;
    }
}
