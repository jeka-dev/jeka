package org.jerkar.api.java.project;

import java.util.Date;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkIvyPublication;
import org.jerkar.api.depmanagement.JkMavenPublication;
import org.jerkar.api.depmanagement.JkPublisher;
import org.jerkar.api.depmanagement.JkVersionProvider;

@Deprecated // Experimental !!!!
public class JkJavaProjectPublisher {

    public static JkJavaProjectPublisher of(JkJavaProject javaProject) {
        return new JkJavaProjectPublisher(javaProject, JkPublisher.local(), true, true, true);
    }

    private final JkJavaProject project;

    private JkPublisher publisher = JkPublisher.local();

    private final boolean publishSources;

    private final boolean publishTests;

    private final boolean publishJavadoc;

    private JkJavaProjectPublisher(JkJavaProject project, JkPublisher publisher, boolean publishSources, boolean publishTests, boolean publishJavadoc) {
        this.project = project;
        this.publisher = publisher;
        this.publishSources = publishSources;
        this.publishTests = publishTests;
        this.publishJavadoc = publishJavadoc;
    }

    /** Publishes the produced artifact to the defined repositories.  */
    public void publish() {
        final JkDependencies dependencies = project.depResolver().resolver().dependenciesToResolve();
        final JkVersionProvider resolvedVersions = project.depResolver().resolver().resolve().resolvedVersionProvider();
        if (publisher.hasMavenPublishRepo()) {
            final JkMavenPublication publication = mavenPublication();
            final JkDependencies deps = project.module().version().isSnapshot() ? dependencies
                    .resolvedWith(resolvedVersions) : dependencies;
                    publisher.publishMaven(project.module(), publication, deps);
        }
        if (publisher.hasIvyPublishRepo()) {
            final Date date = new Date();
            publisher.publishIvy(project.module(), ivyPublication(), dependencies, JkJavaDepScopes.COMPILE,
                    JkJavaDepScopes.DEFAULT_SCOPE_MAPPING, date, resolvedVersions);
        }
    }


    private JkMavenPublication mavenPublication() {
        return JkMavenPublication
                .of(project.packager().jarFile())
                .andIf(publishSources, project.packager().jarSourceFile(), "sources")
                // .andOptional(project.javadocMaker().zipFile(), "javadoc")
                .andOptionalIf(publishTests, project.packager().jarTestFile(), "test")
                .andOptionalIf(publishTests && publishSources, project.packager().jarTestSourceFile(),
                        "testSources");
    }

    private JkIvyPublication ivyPublication() {
        return JkIvyPublication.of(project.packager().jarFile(), JkJavaDepScopes.COMPILE)
                .andIf(publishSources, project.packager().jarSourceFile(), "source", JkJavaDepScopes.SOURCES)
                // .andOptional(project.javadocMaker().zipFile(), "javadoc", JkJavaDepScopes.JAVADOC)
                .andOptionalIf(publishTests, project.packager().jarTestFile(), "jar", JkJavaDepScopes.TEST)
                .andOptionalIf(publishTests && publishSources, project.packager().jarTestSourceFile(), "source", JkJavaDepScopes.SOURCES);
    }

}
