package org.jerkar.api.depmanagement;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;

@SuppressWarnings("javadoc")
public class JkIvyPublisherRunner {

    public static void main(String[] args) {
        JkLog.verbose(true);
        // JkUtilsTool.loadUserSystemProperties();
        testPublishIvy();
        // testPublishMaven();
    }

    public static void testPublishIvy() {
        final IvyPublisher jkIvyResolver = IvyPublisher.of(ivyRepos().withSha1Checksum()
                .withMd5Checksum(), new File("build/output/test-out"));
        final JkVersionedModule versionedModule = JkVersionedModule.of(
                JkModuleId.of("mygroup", "mymodule"), JkVersion.name("myVersion"));
        final JkIvyPublication ivyPublication = JkIvyPublication.of(sampleJarfile(),
                JkScopedDependencyTest.COMPILE, JkScopedDependencyTest.TEST);
        final JkModuleId spring = JkModuleId.of("org.springframework", "spring-jdbc");
        final JkDependencies deps = JkDependencies.builder().on(spring, "3.0.+")
                .scope(JkScopedDependencyTest.COMPILE).build();
        jkIvyResolver.publishIvy(versionedModule, ivyPublication, deps, null, Instant.now(),
                JkVersionProvider.of(spring, "3.0.8"));
    }

    public static void testPublishMaven() {
        final IvyPublisher jkIvyPublisher = IvyPublisher.of(mavenRepos().withMd5AndSha1Checksum()
                .withUniqueSnapshot(false), new File("build/output/test-out"));
        final JkVersionedModule versionedModule = JkVersionedModule.of(
                JkModuleId.of("mygroup2", "mymodule2"), JkVersion.name("0.0.12-SNAPSHOT"));
        final JkMavenPublication publication = JkMavenPublication.of(sampleJarfile())
                .and(sampleJarSourcefile(), "sources").and(sampleJarSourcefile(), "other");
        final JkModuleId spring = JkModuleId.of("org.springframework", "spring-jdbc");
        final JkDependencies deps = JkDependencies.builder().on(spring, "2.0.+")
                .scope(JkScopedDependencyTest.COMPILE).build();
        final JkVersionProvider versionProvider = JkVersionProvider.of(spring, "2.0.5");
        jkIvyPublisher.publishMaven(versionedModule, publication,
                deps.resolvedWith(versionProvider));
    }

    private static File sampleJarfile() {
        final URL url = JkIvyPublisherRunner.class.getResource("myArtifactSample.jar");
        try {
            return new File(url.toURI().getPath());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static File sampleJarSourcefile() {
        final URL url = JkIvyPublisherRunner.class.getResource("myArtifactSample-source.jar");
        try {
            return new File(url.toURI().getPath());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static JkPublishRepos ivyRepos() {
        final File baseDir = new File(JkUtilsFile.workingDir(), "build/output/testIvyRepo");
        baseDir.mkdir();
        return JkPublishRepos.ivy(baseDir);
    }

    private static JkPublishRepos mavenRepos() {
        final File baseDir = new File(JkUtilsFile.workingDir(), "build/output/mavenRepo");
        baseDir.mkdir();
        return JkPublishRepos.maven(baseDir);
    }

}
