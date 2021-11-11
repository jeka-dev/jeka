package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.function.JkRunnables;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Responsible to publish artifacts on the repository. If the project does not publish anything, this part can be
 * ignored. Jeka can publish on Maven and Ivy repositories. <p>
 * From here, you can control what to publish and the transitive dependencies, depending on the repo system the project
 * is published. Note that a project can be be published on many repositories of different systems.
 */
public class JkProjectPublication {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    private final JkProject project;

    private final JkStandardFileArtifactProducer<JkProjectPublication> artifactProducer;

    private final JkMavenPublication<JkProjectPublication> maven;

    private final JkIvyPublication<JkProjectPublication> ivy;

    private final JkRunnables<JkProjectPublication> preActions;

    private final JkRunnables<JkProjectPublication> postActions;

    /**
     * For parent chaining
     */
    public final JkProject __;

    JkProjectPublication(JkProject project) {
        this.project = project;
        this.__ = project;
        artifactProducer = JkStandardFileArtifactProducer.ofParent(this)
                .setArtifactFilenameComputation(project::getArtifactPath);
        registerArtifacts();
        JkVersionedModule.ConflictStrategy conflictStrategy = project.getDuplicateConflictStrategy();
        this.maven = JkMavenPublication.of(this)
            .setArtifactLocatorSupplier(() -> artifactProducer)
            .setDependencies(deps -> JkMavenPublication.computeMavenPublishDependencies(
                    project.getConstruction().getCompilation().getDependencies(),
                    project.getConstruction().getRuntimeDependencies(),
                    conflictStrategy));
        this.ivy = JkIvyPublication.of(this)
            .addArtifacts(() -> artifactProducer)
            .setDependencies(deps -> JkIvyPublication.getPublishDependencies(
                    project.getConstruction().getCompilation().getDependencies(),
                    project.getConstruction().getRuntimeDependencies(), conflictStrategy));
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

    public JkStandardFileArtifactProducer<JkProjectPublication> getArtifactProducer() {
        return artifactProducer;
    }

    public void publish() {
        preActions.run();
        if (maven.getModuleId() != null) {
            maven.publish();
        }
        if (ivy.getModuleId() != null) {
            ivy.publish();
        }
        postActions.run();
    }

    /**
     * Specifies if Javadoc and sources jars should be included in pack/publish. Default is true;
     */
    public JkProjectPublication includeJavadocAndSources(boolean includeJavaDoc, boolean includeSources) {
        if (includeJavaDoc) {
            artifactProducer.putArtifact(JAVADOC_ARTIFACT_ID, project.getDocumentation()::createJavadocJar);
        } else {
            artifactProducer.removeArtifact(JAVADOC_ARTIFACT_ID);
        }
        if (includeSources) {
            artifactProducer.putArtifact(SOURCES_ARTIFACT_ID, project.getDocumentation()::createSourceJar);
        } else {
            artifactProducer.removeArtifact(SOURCES_ARTIFACT_ID);
        }
        return this;
    }

    private void registerArtifacts() {
        artifactProducer.putMainArtifact(project.getConstruction()::createBinJar);
        artifactProducer.putArtifact(SOURCES_ARTIFACT_ID, project.getDocumentation()::createSourceJar);
        artifactProducer.putArtifact(JAVADOC_ARTIFACT_ID, project.getDocumentation()::createJavadocJar);
    }

    public JkModuleId getModuleId() {
        return Optional.ofNullable(maven.getModuleId()).orElse(ivy.getModuleId());
    }

    public String getVersion() {
        return Optional.ofNullable(maven.getVersion()).orElse(ivy.getVersion());
    }

    /**
     * Short hand to build all missing artifacts for publication.
     */
    public void pack() {
        artifactProducer.makeAllMissingArtifacts();
    }

    /**
     * Shorthand to get the first declared publication repository.
     */
    public JkRepo findFirstRepo() {
        return getMaven().getRepos().getRepos().stream()
                .filter(repo1 -> !repo1.isLocal())
                .findFirst().orElse(
                        getIvy().getRepos().getRepos().stream()
                                .filter(repo1 -> !repo1.isLocal())
                                .findFirst().orElse(null)
                );
    }
}
