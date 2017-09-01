package org.jerkar.api.project;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.depmanagement.JkJavaDepScopes;

@Deprecated // Experimental !!!!
public class JkProjectPublisher {

    public static JkProjectPublisher of(JkDependencyResolver dependencyResolver) {
        return new JkProjectPublisher(JkPublisher.local(), true, true, true,
                dependencyResolver);
    }

    private JkPublisher publisher = JkPublisher.local();

    private final boolean publishSources;

    private final boolean publishTests;

    private final boolean publishJavadoc;

    private final JkDependencyResolver dependencyResolver;


    private JkProjectPublisher(JkPublisher publisher, boolean publishSources,
                               boolean publishTests, boolean publishJavadoc,
                               JkDependencyResolver dependencyResolver) {
        this.publisher = publisher;
        this.publishSources = publishSources;
        this.publishTests = publishTests;
        this.publishJavadoc = publishJavadoc;
        this.dependencyResolver = dependencyResolver;
    }

    /** Publishes the produced artifact to the defined repositories.  */
    public void publish(JkArtifactProducer artifactProducer, JkDependencies dependencies, JkVersionedModule versionedModule) {
        Map<String, String> options = new HashMap<String, String>();
        final JkVersionProvider resolvedVersions = dependencies.overridedVersions();
        if (publisher.hasMavenPublishRepo()) {
            final JkMavenPublication publication = mavenPublication(artifactProducer, versionedModule);
            final JkDependencies deps = versionedModule.version().isSnapshot() ? dependencies
                    .resolvedWith(resolvedVersions) : dependencies;
                    publisher.publishMaven(versionedModule, publication, deps);
        }
        if (publisher.hasIvyPublishRepo()) {
            final Date date = new Date();
            publisher.publishIvy(versionedModule, ivyPublication(artifactProducer, versionedModule), dependencies,
                    JkJavaDepScopes.COMPILE, JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, date, resolvedVersions);
        }
    }

    private JkMavenPublication mavenPublication(JkArtifactProducer artifactProducer, JkVersionedModule versionedModule) {
        JkMavenPublication result = JkMavenPublication.of(artifactProducer.artifactFile(artifactProducer.mainArtifactFileId()));
        for (JkArtifactFileId extraFileId : artifactProducer.artifactFileIds()) {
            if (okForClassifier(extraFileId.classifier())) {
                File file = artifactProducer.artifactFile(extraFileId);
                result = result.andOptional(file, extraFileId.classifier());
            }
        }
        return result;
    }

    private JkIvyPublication ivyPublication(JkArtifactProducer artifactProducer, JkVersionedModule versionedModule) {
        JkIvyPublication result =  JkIvyPublication.of(artifactProducer.artifactFile(artifactProducer.mainArtifactFileId()), JkJavaDepScopes.COMPILE);
        for (JkArtifactFileId extraFileId : artifactProducer.artifactFileIds()) {
            if (okForClassifier(extraFileId.classifier())) {
                File file = artifactProducer.artifactFile(extraFileId);
                result = result.andOptional(file, extraFileId.classifier(), scopeFor(extraFileId.classifier()));
            }
        }
        return result;
    }

    private boolean okForClassifier(String classifier) {
        if ("sources".equals(classifier) && !publishSources) {
            return false;
        }
        if ("test".equals(classifier) && !publishTests) {
            return false;
        }
        if ("test-sources".equals(classifier) && ! (publishTests && publishSources)) {
            return false;
        }
        if ("javadoc".equals(classifier) && !publishJavadoc) {
            return false;
        }
        return true;
    }

    private static JkScope scopeFor(String classifier) {
        if ("sources".equals(classifier)) {
            return JkJavaDepScopes.SOURCES;
        }
        if ("test".equals(classifier)) {
            return JkJavaDepScopes.TEST;
        }
        if ("test-sources".equals(classifier)) {
            return JkJavaDepScopes.SOURCES;
        }
        if ("javadoc".equals(classifier)) {
            return JkJavaDepScopes.JAVADOC;
        }
        return JkScope.of(classifier);
    }



}
