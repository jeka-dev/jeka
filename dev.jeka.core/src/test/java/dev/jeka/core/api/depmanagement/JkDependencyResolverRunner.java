package dev.jeka.core.api.depmanagement;

public class JkDependencyResolverRunner {

    public static void main(String[] args) {
        JkResolveResult resolveResult = JkDependencyResolver.of(JkRepo.ofMavenCentral())
                        .resolve(JkDependencySet.of("dev.jeka.plugins:spring-boot:2.0.0.RC1"));
        resolveResult.assertNoError();
        System.out.println(resolveResult);
    }
}
