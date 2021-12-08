package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.publication.JkIvyPublication;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.function.JkRunnables;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Responsible to publish artifacts on the repository. If the project does not publish anything, this part can be
 * ignored. Jeka can publish on Maven and Ivy repositories. <p>
 * From here, you can control what to publish and the transitive dependencies, depending on the repo system the project
 * is published. Note that a project can be be published on many repositories of different systems.
 */
public class JkProjectPublication {

    private final JkProject project;

    private Supplier<String> moduleIdSupplier = () -> null;

    private Supplier<String> versionSupplier = () -> null;

    private JkMavenPublication<JkProjectPublication> maven;

    private JkIvyPublication<JkProjectPublication> ivy;

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
        JkVersionedModule.ConflictStrategy conflictStrategy = project.getDuplicateConflictStrategy();
        JkArtifactLocator artifactLocator = project.getArtifactProducer();
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
        if (maven != null) {
            return maven;
        }
        maven = JkMavenPublication.of(this)
                .setArtifactLocatorSupplier(() -> project.getArtifactProducer())
                .setVersion(this.versionSupplier)
                .setModuleId(this.moduleIdSupplier)
                .setDependencies(deps -> JkMavenPublication.computeMavenPublishDependencies(
                        project.getConstruction().getCompilation().getDependencies(),
                        project.getConstruction().getRuntimeDependencies(),
                        project.getDuplicateConflictStrategy()));
        return maven;
    }

    public JkIvyPublication<JkProjectPublication> getIvy() {
        if (ivy != null) {
            return ivy;
        }
        ivy = JkIvyPublication.of(this)
                .addArtifacts(() -> project.getArtifactProducer())
                .setVersion(this.versionSupplier)
                .setModuleId(this.moduleIdSupplier)
                .setDependencies(deps -> JkIvyPublication.getPublishDependencies(
                        project.getConstruction().getCompilation().getDependencies(),
                        project.getConstruction().getRuntimeDependencies(), project.getDuplicateConflictStrategy()));
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
        if (moduleIdSupplier.get() != null) {
            return JkModuleId.of(moduleIdSupplier.get());
        }
        if (maven != null && maven.getModuleId() != null) {
            return maven.getModuleId();
        } else if (ivy != null && ivy.getModuleId() != null) {
            return ivy.getModuleId();
        }
        return null;
    }

    public String getVersion() {
        if (versionSupplier != null && versionSupplier.get() != null) {
            return versionSupplier.get();
        }
        if (maven != null && maven.getVersion() != null) {
            return maven.getVersion();
        } else if (ivy != null && ivy.getVersion() != null) {
            return ivy.getVersion();
        }
        return null;
    }

    public JkProjectPublication setModuleId(Supplier<String> moduleIdSupplier) {
        this.moduleIdSupplier = moduleIdSupplier;
        return this;
    }

    public JkProjectPublication setModuleId(String moduleId) {
        return setModuleId(() -> moduleId);
    }

    public JkProjectPublication setVersion(String version) {
        return setVersion(() -> version);
    }

    public JkProjectPublication setVersion(Supplier<String> versionSupplier) {
        this.versionSupplier = versionSupplier;
        return this;
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
