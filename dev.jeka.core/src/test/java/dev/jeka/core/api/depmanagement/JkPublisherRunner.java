package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.function.Consumer;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.COMPILE_AND_RUNTIME;
import static dev.jeka.core.api.depmanagement.JkScopedDependencyTest.TEST;


public class JkPublisherRunner {

    public static void main(String[] args) throws IOException {

        JkVersionedModule versionedModule = JkVersionedModule.of("org.myorg:mylib:1.2.6");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.slf4j:slf4j-simple", COMPILE_AND_RUNTIME)
                .and("junit:junit:4.11", TEST);
       // JkMavenPublication mavenPublication = JkMavenPublication.of()Paths.get("org.myorg.mylib.jar"))

        JkPublishedPomMetadata mavenPublicationInfo = JkPublishedPomMetadata.of()
                .getProjectInfo()
                    .setName("My sample Project")
                    .setDescription("A project to demonstrate publishing on Jeka")
                    .setUrl("https://jeka.dev").__
                .addGithubDeveloper("djeang", "myemail@gmail.com")
                .addApache2License();

        Consumer<Path> creator = path -> JkPathFile.of(path).createIfNotExist(); // Use a fake creator
        JkArtifactBasicProducer artifactProducer = JkArtifactBasicProducer.of(Files.createTempDirectory("jeka"), "myproject")
                .putMainArtifact(creator)
                .putArtifact(JkArtifactId.of("sources", "jar"), creator)
                .putArtifact(JkArtifactId.of("javadoc", "jar"), creator);

        JkRepo repo = JkRepo.ofMaven(Paths.get("mavenrepo"))
                .setCredentials("myUserName", "myPassword")
                .getPublishConfig()
                    .setUniqueSnapshot(false)
                    .setSignatureRequired(true)
                    .setVersionFilter(version -> version.isSnapshot() || version.getValue().endsWith("MILESTONE")).__;

        JkPublisher publisher = JkPublisher.of(repo);
        publisher.publishMaven(versionedModule, JkMavenPublication.of(artifactProducer, JkPublishedPomMetadata.of()), deps);
    }

    public static void main2(String[] args) {
        JkLog.setHierarchicalConsoleConsumer();
        JkVersionedModule versionedModule = JkVersionedModule.of("org.myorg:mylib:1.2.6-SNAPSHOT");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.slf4j:slf4j-simple", COMPILE_AND_RUNTIME)
                .and("junit:junit:4.11", TEST);

        JkIvyPublication publication = JkIvyPublication.of(Paths.get("org.myorg.mylib.jar"), "master")
                .and(Paths.get("org.myorg.mylib-sources.jar"));


        JkRepo repo = JkRepo.ofIvy(Paths.get("ivyrepo"))
            .getPublishConfig()
                .setSignatureRequired(true)
                .setVersionFilter(version ->  version.isSnapshot() || version.getValue().endsWith("MILESTONE")).__;

        JkPublisher publisher = JkPublisher.of(repo);
        publisher.publishIvy(versionedModule, publication, deps, JkJavaDepScopes.DEFAULT_SCOPE_MAPPING,
                Instant.now(), JkVersionProvider.of());
    }
}
