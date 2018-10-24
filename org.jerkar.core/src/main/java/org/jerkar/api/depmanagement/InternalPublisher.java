package org.jerkar.api.depmanagement;

import java.time.Instant;

interface InternalPublisher {

    void publishIvy(JkVersionedModule versionedModule, JkIvyPublication publication,
                    JkDependencySet dependencies, JkScopeMapping defaultMapping, Instant deliveryDate,
                    JkVersionProvider resolvedVersion);

    void publishMaven(JkVersionedModule versionedModule, JkMavenPublication publication,
            JkDependencySet dependencies);

}
