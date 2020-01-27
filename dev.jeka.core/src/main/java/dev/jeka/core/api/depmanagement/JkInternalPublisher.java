package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkInternalEmbeddedClassloader;
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
            JkDependencySet dependencies, JkScopeMapping defaultMapping, Instant deliveryDate,
            JkVersionProvider resolvedVersion);

    void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication,
            JkDependencySet dependencies, UnaryOperator<Path> signer);

    static JkInternalPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        final Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(FACTORY_CLASS_NAME);
        if (factoryClass != null) {
            return JkUtilsReflect.invokeStaticMethod(factoryClass, "of", publishRepos, artifactDir);
        }
        return JkInternalEmbeddedClassloader.createCrossClassloaderProxy(
                JkInternalPublisher.class, FACTORY_CLASS_NAME, "of", publishRepos, artifactDir);

    }

}
