package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.function.UnaryOperator;

public class JkJavaProjectMakerPublishingStep {

    private final JkJavaProject project;

    private JkRepoSet publishRepos = JkRepoSet.of();

    private UnaryOperator<Path> signer;

    private final JkRunnables postActions = JkRunnables.noOp();

    private JkMavenPublicationInfo mavenPublicationInfo;

    private JkVersionedModule versionedModule;

    /**
     * For parent chaining
     */
    public final JkJavaProject.JkSteps __;

    JkJavaProjectMakerPublishingStep(JkJavaProject project, JkJavaProject.JkSteps parent) {
        this.project = project;
        this.__ = parent;
    }

    /**
     * Returns the module name and version of this project. This information is used for naming produced artifact files,
     * publishing. It is also consumed by tools as SonarQube.
     */
    public JkVersionedModule getVersionedModule() {
        return versionedModule;
    }

    /**
     * Sets the specified module name and version for this project.
     * @see #getVersionedModule()
     */
    public JkJavaProjectMakerPublishingStep setVersionedModule(JkVersionedModule versionedModule) {
        JkUtilsAssert.notNull(versionedModule, "Can't set null value for versioned module.");
        this.versionedModule = versionedModule;
        return this;
    }

    /**
     * @see #setVersionedModule(JkVersionedModule)
     */
    public JkJavaProjectMakerPublishingStep setVersionedModule(String groupAndName, String version) {
        return setVersionedModule(JkModuleId.of(groupAndName).withVersion(version));
    }

    /**
     * Publishes all defined artifacts.
     */
    public void publish() {
        JkRepoSet repos = this.publishRepos;
        if (repos == null) {
            repos = JkRepoSet.ofLocal();
            JkLog.warn("No publish repo has been mentioned. Publishing on local...");
        }
        publishMaven(repos);
        publishIvy(repos);
        postActions.run();
    }

    public void publishLocal() {
        publishMaven(JkRepo.ofLocal().toSet());
        postActions.run();
    }

    public JkRunnables getPostActions() {
        return postActions;
    }

    public JkMavenPublicationInfo getMavenPublicationInfo() {
        return this.mavenPublicationInfo;
    }

    public JkJavaProjectMakerPublishingStep setMavenPublicationInfo(JkMavenPublicationInfo mavenPublicationInfo) {
        this.mavenPublicationInfo = mavenPublicationInfo;
        return this;
    }

    private void publishMaven(JkRepoSet repos) {
        JkException.throwIf(versionedModule == null, "No versioned module has been set on "
                + project + ". Can't publish.");
        JkMavenPublication publication = JkMavenPublication.of(project.getArtifactProducer(), Collections.emptySet())
                .with(mavenPublicationInfo).withSigner(signer);
        JkPublisher.of(repos, project.getOutputDir()).withSigner(this.signer)
                .publishMaven(versionedModule, publication, project.getDependencyManagement().getScopeDefaultedDependencies());
    }

    private void publishIvy(JkRepoSet repos) {
        if (!repos.hasIvyRepo()) {
            return;
        }
        JkException.throwIf(versionedModule == null, "No versionedModule has been set on "
                + project + ". Can't publish.");
        JkLog.startTask("Preparing Ivy publication");
        final JkDependencySet dependencies = project.getDependencyManagement().getScopeDefaultedDependencies();
        JkArtifactProducer artifactProducer = project.getArtifactProducer();
        final JkIvyPublication publication = JkIvyPublication.of(artifactProducer.getMainArtifactPath(), JkJavaDepScopes.COMPILE.getName())
                .andOptional(artifactProducer.getArtifactPath(JkJavaProject.SOURCES_ARTIFACT_ID), JkJavaDepScopes.SOURCES.getName())
                .andOptional(artifactProducer.getArtifactPath(JkJavaProject.JAVADOC_ARTIFACT_ID), JkJavaDepScopes.JAVADOC.getName());
        final JkVersionProvider resolvedVersions = project.getDependencyManagement().getResolver()
                .resolve(dependencies, dependencies.getInvolvedScopes()).getResolvedVersionProvider();
        JkLog.endTask();
        JkPublisher.of(repos, project.getOutputDir())
                .publishIvy(versionedModule, publication, dependencies,
                        JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, Instant.now(), resolvedVersions);
    }

    public JkRepoSet getPublishRepos() {
        return this.publishRepos;
    }

    public JkJavaProjectMakerPublishingStep setPublishRepos(JkRepoSet publishRepos) {
        JkUtilsAssert.notNull(publishRepos, "publish repos cannot be null.");
        this.publishRepos = publishRepos;
        return this;
    }

    public JkJavaProjectMakerPublishingStep addPublishRepo(JkRepo publishRepo) {
        this.publishRepos = this.publishRepos.and(publishRepo);
        return this;
    }

    public void setSigner(UnaryOperator<Path> signer) {
        this.signer = signer;
    }
}
