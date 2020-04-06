package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.function.Consumer;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.COMPILE_AND_RUNTIME;
import static dev.jeka.core.api.depmanagement.JkScopedDependencyTest.TEST;


public class JkPublisherRunner {

    public static void main(String[] args) {

        JkVersionedModule versionedModule = JkVersionedModule.of("org.myorg:mylib:1.2.6");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.slf4j:slf4j-simple", COMPILE_AND_RUNTIME)
                .and("junit:junit:4.11", TEST);
       // JkMavenPublication mavenPublication = JkMavenPublication.of()Paths.get("org.myorg.mylib.jar"))

        JkMavenPomMetadata mavenPublicationInfo = JkMavenPomMetadata.of()
                .getProjectInfo()
                    .setName("My sample Project")
                    .setDescription("A project to demonstrate publishing on Jeka")
                    .setUrl("https://jeka.dev").__
                .addGithubDeveloper("djeang", "myemail@gmail.com")
                .addApache2License();

        Consumer<Path> creator = path -> JkPathFile.of(path).createIfNotExist(); // Use a fake creator
        JkArtifactBasicProducer artifactProducer = JkArtifactBasicProducer.of()
                .putMainArtifact(creator)
                .putArtifact(JkArtifactId.of("sources", "jar"), creator)
                .putArtifact(JkArtifactId.of("javadoc", "jar"), creator);


        JkRepo repo = JkRepo.ofMaven(Paths.get("mavenrepo"))
                .withOptionalCredentials("myUserName", "myPassword")
                .with(JkRepo.JkPublishConfig.of()
                            .withUniqueSnapshot(false)
                            .withNeedSignature(true)
                            .withFilter(mod -> // only accept SNAPSHOT and MILESTONE
                                mod.getVersion().isSnapshot() || mod.getVersion().getValue().endsWith("MILESTONE")
                            ));

        JkPublisher publisher = JkPublisher.of(repo);
        publisher.publishMaven(versionedModule, JkMavenPublication.of(artifactProducer, JkMavenPomMetadata.of()), deps);
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
                .with(JkRepo.JkPublishConfig.of()
                        .withNeedSignature(true)
                        .withFilter(mod -> // only accept SNAPSHOT and MILESTONE
                                mod.getVersion().isSnapshot() || mod.getVersion().getValue().endsWith("MILESTONE")
                        ));

        JkPublisher publisher = JkPublisher.of(repo);
        publisher.publishIvy(versionedModule, publication, deps, JkJavaDepScopes.DEFAULT_SCOPE_MAPPING,
                Instant.now(), JkVersionProvider.of());
    }
}
