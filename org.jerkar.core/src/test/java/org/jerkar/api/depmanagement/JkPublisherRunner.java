package org.jerkar.api.depmanagement;

import dev.jeka.core.api.crypto.pgp.JkPgp;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Paths;
import java.time.Instant;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE_AND_RUNTIME;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;

public class JkPublisherRunner {

    public static void main(String[] args) {

        JkVersionedModule versionedModule = JkVersionedModule.of("org.myorg:mylib:1.2.6");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.slf4j:slf4j-simple", COMPILE_AND_RUNTIME)
                .and("junit:junit:4.11", TEST);
        JkMavenPublication mavenPublication = JkMavenPublication.of(Paths.get("org.myorg.mylib.jar"))

                // the following are optional but required to publish on public repositories.
                .and(Paths.get("org.myorg.mylib-sources.jar"), "sources")
                .and(Paths.get("org.myorg.mylib-javadoc.jar"), "javadoc")
                .withChecksums("sha-2", "md5")
                .withSigner(JkPgp.of(Paths.get("myPubring"), Paths.get("mySecretRing"), "mypassword").getSigner("toto"))
                .with(JkMavenPublicationInfo.of("My sample project",
                        "A project to demonstrate publishing on Jerkar",
                        "http://project.jerkar.org")
                        .andApache2License()
                        .andDeveloper("djeang", "myemail@gmail.com", "jerkar.org", "http://project.jerkar.org/"));

        JkRepo repo = JkRepo.ofMaven(Paths.get("mavenrepo"))
                .withOptionalCredentials("myUserName", "myPassword")
                .with(JkRepo.JkPublishConfig.of()
                            .withUniqueSnapshot(false)
                            .withNeedSignature(true)
                            .withFilter(mod -> // only accept SNAPSHOT and MILESTONE
                                mod.getVersion().isSnapshot() || mod.getVersion().getValue().endsWith("MILESTONE")
                            ));

        JkPublisher publisher = JkPublisher.of(repo);
        publisher.publishMaven(versionedModule, mavenPublication, deps);
    }

    public static void main2(String[] args) {
        JkLog.registerHierarchicalConsoleHandler();
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
