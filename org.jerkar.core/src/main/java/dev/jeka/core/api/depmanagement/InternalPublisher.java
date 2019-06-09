package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.UnaryOperator;

interface InternalPublisher {

    void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
                    JkDependencySet dependencies, JkScopeMapping defaultMapping, Instant deliveryDate,
                    JkVersionProvider resolvedVersion);

    void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication,
                      JkDependencySet dependencies, UnaryOperator<Path> signer);

}
