package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.function.JkRunnables;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Responsible to publish artifacts on the repository. If the project does not publish anything, this part can be
 * ignored. Jeka can publish on Maven and Ivy repositories. <p>
 * From here, you can control what to publish and the transitive dependencies, depending on the repo system the project
 * is published. Note that a project can be published on many repositories of different systems.
 */
public class JkProjectPublication {

    private final JkProject project;

    private final JkMavenPublication maven;

    private final JkIvyPublication<JkProjectPublication> ivy;

    private final JkRunnables<JkProjectPublication> preActions;

    private final JkRunnables<JkProjectPublication> postActions;

    private boolean publishMaven = true;

    private boolean publishIvy = false;

    /**
     * For parent chaining
     */
    public final JkProject __;

    JkProjectPublication(JkProject project) {
        this.project = project;
        this.__ = project;
        JkCoordinate.ConflictStrategy conflictStrategy = project.getDuplicateConflictStrategy();
        JkArtifactLocator artifactLocator = project.getArtifactProducer();
        maven = JkMavenPublication.of(this)
                .setArtifactLocatorSupplier(() -> project.getArtifactProducer())
                .configureDependencies(deps -> JkMavenPublication.computeMavenPublishDependencies(
                        project.getCompilation().getDependencies(),
                        project.getPackaging().getRuntimeDependencies(),
                        project.getDuplicateConflictStrategy()))
                .setBomResolutionRepos(() -> project.getDependencyResolver().getRepos());
        ivy = JkIvyPublication.of(this)
                .addArtifacts(() -> project.getArtifactProducer())
                .configureDependencies(deps -> JkIvyPublication.getPublishDependencies(
                        project.getCompilation().getDependencies(),
                        project.getPackaging().getRuntimeDependencies(), project.getDuplicateConflictStrategy()));
        this.preActions = JkRunnables.ofParent(this);
        this.postActions = JkRunnables.ofParent(this);
    }

    public JkProjectPublication apply(Consumer<JkProjectPublication> consumer) {
        consumer.accept(this);
        return this;
    }

    public JkRunnables<JkProjectPublication> getPreActions() {
        return preActions;
    }

    public JkRunnables<JkProjectPublication> getPostActions() {
        return postActions;
    }

    public JkMavenPublication<JkProjectPublication> getMaven() {
        return maven;
    }

    public JkIvyPublication<JkProjectPublication> getIvy() {
        return ivy;
    }

    public void publish() {
        preActions.run();
        if (publishMaven) {
            maven.publish();
        }
        if (publishIvy) {
            ivy.publish();
        }
        postActions.run();
    }

    public void publishLocal() {
        preActions.run();
        if (publishMaven) {
            maven.publishLocal();
        }
        if (publishIvy) {
            ivy.publishLocal();
        }
        postActions.run();
    }

    public boolean isPublishMaven() {
        return publishMaven;
    }

    public JkProjectPublication setPublishMaven(boolean publishMaven) {
        this.publishMaven = publishMaven;
        return this;
    }

    public boolean isPublishIvy() {
        return publishIvy;
    }

    public JkProjectPublication setPublishIvy(boolean publishIvy) {
        this.publishIvy = publishIvy;
        return this;
    }

    public JkModuleId getModuleId() {
        return Optional.ofNullable(maven.getModuleId()).orElseGet(ivy::getModuleId);
    }

    public JkVersion getVersion() {
        return Optional.ofNullable(maven.getVersion()).orElseGet(ivy::getVersion);
    }

    public JkProjectPublication setModuleId(String moduleId) {
        this.maven.setModuleId(moduleId);
        this.ivy.setModuleId(moduleId);
        return this;
    }

    public JkProjectPublication setVersion(String version) {
        return setVersion(() -> version);
    }

    public JkProjectPublication setVersion(Supplier<String> versionSupplier) {
        this.maven.setVersion(versionSupplier);
        this.ivy.setVersion(versionSupplier);
        return this;
    }

    public JkProjectPublication setRepos(JkRepoSet repos) {
        this.maven.setPublishRepos(repos);
        this.ivy.setRepos(repos);
        return this;
    }

    public JkProjectPublication setDefaultSigner(UnaryOperator<Path> signer) {
        this.maven.setDefaultSigner(signer);
        this.ivy.setDefaultSigner(signer);
        return this;
    }

    /**
     * Shorthand to get the first declared publication repository.
     */
    public JkRepo findFirstNonLocalRepo() {
        return getMaven().getPublishRepos().getRepos().stream()
                .filter(repo1 -> !repo1.isLocal())
                .findFirst().orElse(
                        getIvy().getRepos().getRepos().stream()
                                .filter(repo1 -> !repo1.isLocal())
                                .findFirst().orElse(null)
                );
    }
}
