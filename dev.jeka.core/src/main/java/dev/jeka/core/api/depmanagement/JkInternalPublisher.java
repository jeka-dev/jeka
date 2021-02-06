package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.tooling.JkIvyConfigurationMapping;
import dev.jeka.core.api.depmanagement.tooling.JkIvyPublication;
import dev.jeka.core.api.depmanagement.tooling.JkMavenPublication;
import dev.jeka.core.api.depmanagement.tooling.JkQualifiedDependencies;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.UnaryOperator;

/**
 * Not part of the public API
 */
public interface JkInternalPublisher {

    String FACTORY_CLASS_NAME = "dev.jeka.core.api.depmanagement.embedded.ivy.IvyInternalPublisherFactory";

    void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
                    JkQualifiedDependencies dependencies,
                    JkIvyConfigurationMapping defaultMapping, Instant deliveryDate);

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
