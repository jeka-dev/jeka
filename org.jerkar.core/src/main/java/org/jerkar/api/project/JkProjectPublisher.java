package org.jerkar.api.project;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.depmanagement.JkJavaDepScopes;

@Deprecated // Experimental !!!!
public class JkProjectPublisher {

    public static JkProjectPublisher of(JkDependencyResolver dependencyResolver) {
        return new JkProjectPublisher(JkPublisher.local(), dependencyResolver);
    }

    private JkPublisher publisher = JkPublisher.local();

    //private final JkDependencyResolver dependencyResolver;

    private JkProjectPublisher(JkPublisher publisher,
                               JkDependencyResolver dependencyResolver) {
        this.publisher = publisher;
        //this.dependencyResolver = dependencyResolver;
    }

    /** Publishes the produced artifact to the defined repositories.  */
    public void publish(JkArtifactProducer artifactProducer, JkDependencies dependencies, JkVersionedModule versionedModule) {
        Map<String, String> options = new HashMap<String, String>();
        final JkVersionProvider resolvedVersions = dependencies.explicitVersions();
        if (publisher.hasMavenPublishRepo()) {
            final JkMavenPublication publication = JkMavenPublication.of(artifactProducer);
            final JkDependencies deps = versionedModule.version().isSnapshot() ? dependencies
                    .resolvedWith(resolvedVersions) : dependencies;
                    publisher.publishMaven(versionedModule, publication, deps);
        }
        if (publisher.hasIvyPublishRepo()) {
            final Date date = new Date();
            publisher.publishIvy(versionedModule, JkIvyPublication.of(artifactProducer), dependencies,
                    JkJavaDepScopes.COMPILE, JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, date, resolvedVersions);
        }
    }

}
