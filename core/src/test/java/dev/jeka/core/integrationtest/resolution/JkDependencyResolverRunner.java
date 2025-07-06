package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLog;
import org.junit.jupiter.api.*;

import java.util.Base64;
import java.util.List;


@Disabled
class JkDependencyResolverRunner {

    @BeforeAll
    public static void beforeAll() {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.setVerbosity(JkLog.Verbosity.DEBUG);
        JkInfo.setLogIvyVerboseMessages(true);
    }

    @AfterAll
    public static void afterAll() {
        JkLog.setVerbosity(JkLog.Verbosity.INFO);
        JkLog.restoreToInitialState();
        JkInfo.setLogIvyVerboseMessages(false);
    }

    @Test
    void getSpringboot_MvnCentral() {
        JkRepo repo = JkRepo.ofMavenCentral().copy();
        repo.setHttpHeaders("x-toto", "tototototo");
        JkResolveResult resolveResult = JkDependencyResolver.of()
                .addRepos(repo)
                .resolve(JkDependencySet.of("org.springframework.boot:spring-boot-starter:1.5.0.RELEASE"));
        resolveResult.assertNoError();
        System.out.println(resolveResult.getDependencyTree());
    }

    @Test
    void getSpringboot_localNexus_withAuth() {
        JkRepo repo = JkRepo.of("http://localhost:8081/repository/maven-central");
        //repo.setCredentials(JkRepo.JkRepoCredentials.of("admin", "admin"));
        String encoded = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        repo.setHttpHeaders("Authorization", "Basic " + encoded);
        JkResolveResult resolveResult = JkDependencyResolver.of()
                .addRepos(repo)
                .resolve(JkDependencySet.of("org.springframework.boot:spring-boot-starter:1.5.2.RELEASE"));
        resolveResult.assertNoError();
        System.out.println(resolveResult.getDependencyTree());
    }

    @Test
    @Disabled("Ivy bug prevent to resolve this dependency")
    void jfrogAndMavenCentral_resolveSnapshot() {
        JkRepo embabelRepo = JkRepo.of("https://repo.embabel.com/artifactory/libs-snapshot").toReadonly();
        JkRepo springMilestoneRepo = JkRepo.of("http://repo.spring.io/milestone");
        JkRepoSet repos = JkRepoSet.of(embabelRepo).and(JkRepo.ofMavenCentral()).and(springMilestoneRepo);

        JkDependencySet deps = JkDependencySet.of()
                .and("com.embabel.agent:embabel-agent-parent:1.0.0-SNAPSHOT@pom")
                .and("com.embabel.agent:embabel-agent-starter:0.1.0-SNAPSHOT");

        JkResolveResult resolveResult = JkDependencyResolver.of()
                .addRepos(repos)
                .resolve(deps);
        resolveResult.assertNoError();
    }

    @Test
    @Disabled("snapshot versions")
    void bomAndExplicitVersionDefined_explicitVersionTakePrecedence() {
        JkRepo embabelRepo = JkRepo.of("https://repo.embabel.com/artifactory/libs-snapshot").toReadonly();
        JkRepo springMilestoneRepo = JkRepo.of("http://repo.spring.io/milestone");
        JkRepoSet repos = JkRepoSet.of(embabelRepo).and(JkRepo.ofMavenCentral()).and(springMilestoneRepo);

        JkVersionProvider versionProvider = JkVersionProvider.of()
                .andBom("com.embabel.agent:embabel-agent-parent:1.0.0-SNAPSHOT")
                .and("com.embabel.agent:embabel-agent-starter", "0.1.0-SNAPSHOT");

        JkDependencySet deps = JkDependencySet.of()
                .and("com.embabel.agent:embabel-agent-starter")
                .andVersionProvider(versionProvider);

        JkResolveResult resolveResult = JkDependencyResolver.of()
                .addRepos(repos)
                .resolve(deps);
        JkResolvedDependencyNode node = resolveResult.getDependencyTree()
                .getChild(JkModuleId.of("com.embabel.agent:embabel-agent-starter"));
        JkVersion resolvedVersion = node.getModuleInfo().getResolvedVersion();
        Assertions.assertEquals("0.1.0-SNAPSHOT", resolvedVersion.toString());
    }

    @Test
    @Disabled("snapshot versions")
    void providedVersions_notOverrideTransitiveDepVersions() {
        JkRepo embabelRepo = JkRepo.of("https://repo.embabel.com/artifactory/libs-snapshot").toReadonly();
        JkRepo springMilestoneRepo = JkRepo.of("http://repo.spring.io/milestone");
        JkRepoSet repos = JkRepoSet.of(embabelRepo).and(JkRepo.ofMavenCentral()).and(springMilestoneRepo);

        JkVersionProvider versionProvider = JkVersionProvider.of()
                .andBom("com.embabel.agent:embabel-agent-parent:1.0.0-SNAPSHOT")
                .and("com.embabel.agent:embabel-agent-starter", "0.1.0-SNAPSHOT");

        JkDependencySet deps = JkDependencySet.of()
                .and("com.embabel.agent:embabel-agent-starter")
                .andVersionProvider(versionProvider);

        JkResolveResult resolveResult = JkDependencyResolver.of()
                .addRepos(repos)
                .resolve(deps);
        JkResolvedDependencyNode node = resolveResult.getDependencyTree()
                .getChild(JkModuleId.of("com.embabel.agent:embabel-agent-starter"));
        JkResolvedDependencyNode transitiveNode =
                node.getChild(JkModuleId.of("com.embabel.agent:embabel-agent-autoconfigure"));
        JkVersion resolvedVersion = transitiveNode.getModuleInfo().getResolvedVersion();
        Assertions.assertEquals("0.1.0-SNAPSHOT", resolvedVersion.toString());
    }


}
