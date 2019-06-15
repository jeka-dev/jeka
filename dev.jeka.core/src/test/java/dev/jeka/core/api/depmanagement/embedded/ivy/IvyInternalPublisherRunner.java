package dev.jeka.core.api.depmanagement.embedded.ivy;


import dev.jeka.core.api.depmanagement.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;


@SuppressWarnings("javadoc")
public class IvyInternalPublisherRunner {

    public static void main(String[] args) throws IOException {
        //JkEvent.verbose(true);
        // JkUtilsTool.loadUserSystemProperties();
        testPublishIvy();
        // testPublishMaven();
    }

    public static void testPublishIvy() throws IOException {
        final IvyInternalPublisher jkIvyInternalPublisher = IvyInternalPublisher.of(ivyRepo().toSet(), Paths.get("jeka/output/test-out"));
        final JkVersionedModule versionedModule = JkVersionedModule.of(
                JkModuleId.of("mygroup", "mymodule"), JkVersion.of("myVersion"));
        final JkIvyPublication ivyPublication = JkIvyPublication.of(sampleJarfile(),
                JkScopedDependencyTest.COMPILE.getName(), JkScopedDependencyTest.TEST.getName());
        final JkModuleId spring = JkModuleId.of("org.springframework", "spring-jdbc");
        final JkDependencySet deps = JkDependencySet.of().and(spring, "3.0.+", JkScopedDependencyTest.COMPILE);
        jkIvyInternalPublisher.publishIvy(versionedModule, ivyPublication, deps, null, Instant.now(),
                JkVersionProvider.of(spring, "3.0.8"));
    }

    public static void testPublishMaven() throws IOException {
        final IvyInternalPublisher jkIvyPublisher = IvyInternalPublisher.of(mavenRepo().with(JkRepo.JkPublishConfig.of()
                .withUniqueSnapshot(false)).toSet(), Paths.get("jeka/output/test-out"));
        final JkVersionedModule versionedModule = JkVersionedModule.of(
                JkModuleId.of("mygroup2", "mymodule2"), JkVersion.of("0.0.12-SNAPSHOT"));
        final JkMavenPublication publication = JkMavenPublication.of(sampleJarfile())
                .and(sampleJarSourcefile(), "sources").and(sampleJarSourcefile(), "other");
        final JkModuleId spring = JkModuleId.of("org.springframework:spring-jdbc");
        final JkDependencySet deps = JkDependencySet.of().and(spring, "2.0.+", JkScopedDependencyTest.COMPILE);
        final JkVersionProvider versionProvider = JkVersionProvider.of(spring, "2.0.5");
        jkIvyPublisher.publishMaven(versionedModule, publication,
                deps.withVersionProvider(versionProvider), null);
    }

    private static Path sampleJarfile() {
        final URL url = IvyInternalPublisherRunner.class.getResource("myArtifactSample.jar");
        try {
            return Paths.get(url.toURI());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path sampleJarSourcefile() {
        final URL url = IvyInternalPublisherRunner.class.getResource("myArtifactSample-source.jar");
        try {
            return Paths.get(url.toURI().getPath());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static JkRepo ivyRepo() throws IOException {
        final Path baseDir = Paths.get("jeka/output/testIvyRepo");
        Files.createDirectories(baseDir);
        return JkRepo.ofIvy(baseDir);
    }

    private static JkRepo mavenRepo() throws IOException {
        final Path baseDir = Paths.get( "jeka/output/mavenRepo");
        Files.createDirectories(baseDir);
        return JkRepo.ofMaven(baseDir);
    }

}
