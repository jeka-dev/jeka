package org.jerkar.api.java.project;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jerkar.api.depmanagement.*;

@Deprecated // Experimental !!!!
public class JkJavaProjectPublisher {

    public static JkJavaProjectPublisher of(JkDependencyResolver dependencyResolver) {
        return new JkJavaProjectPublisher(JkPublisher.local(), true, true, true,
                dependencyResolver);
    }

    private JkPublisher publisher = JkPublisher.local();

    private final boolean publishSources;

    private final boolean publishTests;

    private final boolean publishJavadoc;

    private final JkDependencyResolver dependencyResolver;


    private JkJavaProjectPublisher(JkPublisher publisher, boolean publishSources,
                                   boolean publishTests, boolean publishJavadoc,
                                   JkDependencyResolver dependencyResolver) {
        this.publisher = publisher;
        this.publishSources = publishSources;
        this.publishTests = publishTests;
        this.publishJavadoc = publishJavadoc;
        this.dependencyResolver = dependencyResolver;
    }

    /** Publishes the produced artifact to the defined repositories.  */
    public void publish(JkJavaProject project, JkVersionedModule versionedModule) {
        Map<String, String> options = new HashMap<String, String>();
        final JkDependencies dependencies = project.getDependencies(options);
        final JkVersionProvider resolvedVersions = project.getDependencies(options).overridedVersions();
        if (publisher.hasMavenPublishRepo()) {
            final JkMavenPublication publication = mavenPublication(project, versionedModule);
            final JkDependencies deps = versionedModule.version().isSnapshot() ? dependencies
                    .resolvedWith(resolvedVersions) : dependencies;
                    publisher.publishMaven(versionedModule, publication, deps);
        }
        if (publisher.hasIvyPublishRepo()) {
            final Date date = new Date();
            publisher.publishIvy(versionedModule, ivyPublication(project, versionedModule), dependencies, JkJavaDepScopes.COMPILE,
                    JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, date, resolvedVersions);
        }
    }


    private JkMavenPublication mavenPublication(JkJavaProject project, JkVersionedModule versionedModule) {
        return JkMavenPublication
                .of(project.getMainJar())
                .andIf(publishSources, project.getJar("sources"), "sources")
                .andOptional(project.getJar("javadoc"), "javadoc")
                .andOptionalIf(publishTests, project.getJar("test"), "test");
    }

    private JkIvyPublication ivyPublication(JkJavaProject project, JkVersionedModule versionedModule) {
        return JkIvyPublication.of(project.getMainJar(), JkJavaDepScopes.COMPILE)
                .andIf(publishSources, project.getJar("sources"), "source", JkJavaDepScopes.SOURCES)
                // .andOptional(project.javadocMaker().zipFile(), "javadoc", JkJavaDepScopes.JAVADOC)
                .andOptionalIf(publishTests, project.getJar("test"), "jar", JkJavaDepScopes.TEST);
    }

}
