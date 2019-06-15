package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.UnaryOperator;

/**
 * Not part of the public API
 */
public interface JkInternalPublisher {

    static final String FACTORY_CLASS_NAME = "dev.jeka.core.api.depmanagement.embedded.ivy.IvyInternalPublisherFactory";

    void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
                    JkDependencySet dependencies, JkScopeMapping defaultMapping, Instant deliveryDate,
                    JkVersionProvider resolvedVersion);

    void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication,
                      JkDependencySet dependencies, UnaryOperator<Path> signer);

    static JkInternalPublisher of(JkRepoSet publishRepos, Path artifactDir) {
        final JkInternalPublisher ivyPublisher;
        Class<?> factoryClass = JkClassLoader.ofCurrent().loadIfExist(FACTORY_CLASS_NAME);
        if (factoryClass != null) {
            ivyPublisher = JkUtilsReflect.invokeStaticMethod(factoryClass, "of", publishRepos, artifactDir);
        } else {
            throw new IllegalStateException("Use embedded class loader");
        }
        return ivyPublisher;
    }

}
