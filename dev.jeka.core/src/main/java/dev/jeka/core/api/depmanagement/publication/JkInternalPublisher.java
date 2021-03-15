package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactLocator;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.util.List;

/**
 * Not part of the public API.
 */
public interface JkInternalPublisher {

    String FACTORY_CLASS_NAME = "dev.jeka.core.api.depmanagement.embedded.ivy.IvyInternalPublisherFactory";

    /**
     * Publishes the specified module to the repository mentioned in the publicatioin.
     *
     * @param versionedModule
     *            The module/version to publish.
     * @param publishedArtifacts
     *            The artifacts to publish.
     * @param dependencies
     *            The dependencies of the published module.
     */
    void publishIvy(JkVersionedModule versionedModule, List<JkIvyPublication.JkPublishedArtifact> publishedArtifacts,
                    JkQualifiedDependencies dependencies);

    void publishMaven(JkVersionedModule versionedModule, JkArtifactLocator artifactLocator, JkPomMetadata pomMetadata,
                      JkDependencySet dependencySet);

    static JkInternalPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        final Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(FACTORY_CLASS_NAME);
        if (factoryClass != null) {
            return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", publishRepos, artifactDir);
        }
        return JkInternalClassloader.ofMainEmbeddedLibs().createCrossClassloaderProxy(
                JkInternalPublisher.class, FACTORY_CLASS_NAME, "of", publishRepos, artifactDir);

    }

}
