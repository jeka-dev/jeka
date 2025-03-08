package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;

class JkDependencyResolverRunner {

    public static void main(String[] args) {
        JkRepo repo = JkRepo.ofMavenCentral().copy();
        repo.setHttpHeaders("x-toto", "tototototo");
        JkResolveResult resolveResult = JkDependencyResolver.of()
                .addRepos(repo)
                .resolve(JkDependencySet.of("org.springframework.boot:spring-boot-starter:1.5.0.RELEASE"));
        resolveResult.assertNoError();
        System.out.println(resolveResult.getDependencyTree());
    }
}
