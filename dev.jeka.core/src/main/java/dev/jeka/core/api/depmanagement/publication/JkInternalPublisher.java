package dev.jeka.core.api.depmanagement.publication;

import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.UnaryOperator;

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
     * @param publication
     *            The artifacts to publish.
     * @param dependencies
     *            The dependencies of the published module.
     * @param defaultMapping
     *            The default scope mapping of the published module
     * @param deliveryDate
     *            The delivery date.
     */
    void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
                    JkQualifiedDependencies dependencies,
                    JkIvyConfigurationMappingSet defaultMapping, Instant deliveryDate);

    void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication,
                      JkQualifiedDependencies dependencies, UnaryOperator<Path> signer);

    static JkInternalPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        final Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(FACTORY_CLASS_NAME);
        if (factoryClass != null) {
            return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", publishRepos, artifactDir);
        }
        return JkInternalClassloader.ofMainEmbeddedLibs().createCrossClassloaderProxy(
                JkInternalPublisher.class, FACTORY_CLASS_NAME, "of", publishRepos, artifactDir);

    }

}
