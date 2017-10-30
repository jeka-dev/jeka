package org.jerkar.api.depmanagement;


import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;

@SuppressWarnings("javadoc")
public class JkIvyPublisherRunner {

    public static void main(String[] args) throws IOException {
        JkLog.verbose(true);
        // JkUtilsTool.loadUserSystemProperties();
        testPublishIvy();
        // testPublishMaven();
    }

    public static void testPublishIvy() throws IOException {
        final IvyPublisher jkIvyResolver = IvyPublisher.of(ivyRepos().withSha1Checksum()
                .withMd5Checksum(), Paths.get("build/output/test-out"));
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

    public static void testPublishMaven() throws IOException {
        final IvyPublisher jkIvyPublisher = IvyPublisher.of(mavenRepos().withMd5AndSha1Checksum()
                .withUniqueSnapshot(false), Paths.get("build/output/test-out"));
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

    private static Path sampleJarfile() {
        final URL url = JkIvyPublisherRunner.class.getResource("myArtifactSample.jar");
        try {
            return Paths.get(url.toURI());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path sampleJarSourcefile() {
        final URL url = JkIvyPublisherRunner.class.getResource("myArtifactSample-source.jar");
        try {
            return Paths.get(url.toURI().getPath());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static JkPublishRepos ivyRepos() throws IOException {
        final Path baseDir = Paths.get("build/output/testIvyRepo");
        Files.createDirectories(baseDir);
        return JkPublishRepos.ivy(baseDir);
    }

    private static JkPublishRepos mavenRepos() throws IOException {
        final Path baseDir = Paths.get( "build/output/mavenRepo");
        Files.createDirectories(baseDir);
        return JkPublishRepos.ivy(baseDir);
    }

}
