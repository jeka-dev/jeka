package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkModuleId;
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
public class JkJavaProjectPublication {

    public static final JkArtifactId SOURCES_ARTIFACT_ID = JkArtifactId.of("sources", "jar");

    public static final JkArtifactId JAVADOC_ARTIFACT_ID = JkArtifactId.of("javadoc", "jar");

    private final JkJavaProject project;

    private final JkStandardFileArtifactProducer<JkJavaProjectPublication> artifactProducer;

    private final JkMavenPublication<JkJavaProjectPublication> maven;

    private final JkIvyPublication<JkJavaProjectPublication> ivy;

    private final JkRunnables<JkJavaProjectPublication> preActions;

    private final JkRunnables<JkJavaProjectPublication> postActions;

    /**
     * For parent chaining
     */
    public final JkJavaProject __;

    JkJavaProjectPublication(JkJavaProject project) {
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

    public JkJavaProjectPublication apply(Consumer<JkJavaProjectPublication> consumer) {
        consumer.accept(this);
        return this;
    }

    public JkRunnables<JkJavaProjectPublication> getPreActions() {
        return preActions;
    }

    public JkRunnables<JkJavaProjectPublication> getPostActions() {
        return postActions;
    }

    public JkMavenPublication<JkJavaProjectPublication> getMaven() {
        return maven;
    }

    public JkIvyPublication<JkJavaProjectPublication> getIvy() {
        return ivy;
    }

    public JkStandardFileArtifactProducer<JkJavaProjectPublication> getArtifactProducer() {
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
}
