package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;

public class JkDependencyResolverRunner {

    public static void main(String[] args) {
        JkResolveResult resolveResult = JkDependencyResolver.ofParent(JkRepo.ofMavenCentral())
                        .resolve(JkDependencySet.of("dev.jeka.plugins:spring-boot:2.0.0.RC1"));
        resolveResult.assertNoError();
        System.out.println(resolveResult);
    }
}
