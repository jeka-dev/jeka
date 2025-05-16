package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;


@Disabled
class JkDependencyResolverRunner {

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
    void searchVersion_localNexus_withAuth() {
        JkRepo repo = JkRepo.of("http://localhost:8081/repository/maven-central");
        //repo.setCredentials(JkRepo.JkRepoCredentials.of("admin", "admin"));
        String encoded = Base64.getEncoder().encodeToString("admin:admin".getBytes());
        repo.setHttpHeaders("Authorization", "Basic " + encoded);
        List<String> versions = JkDependencyResolver.of()
                .addRepos(repo)
                .searchVersions("org.springframework.boot:spring-boot-starter");
        versions.forEach(System.out::println);
    }
}
