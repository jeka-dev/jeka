package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResolverWithoutScopeMapperIT {

    private static final JkRepos REPOS = JkRepos.mavenCentral();

    @Test
    public void resolveCompile() {
        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.APACHE_COMMONS_DBCP, "1.4").scope(JkJavaBuild.COMPILE)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(REPOS, deps);
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.COMPILE);
        assertTrue(resolveResult.contains(JkModuleId.of("commons-pool")));
        assertEquals(1, resolveResult.moduleFiles().size());

        deps = JkDependencies.builder()
                .on(JkPopularModules.HIBERNATE_CORE, "5.2.10.Final").scope(JkJavaBuild.COMPILE)
                .build();
        resolver = JkDependencyResolver.managed(REPOS, deps);
        resolveResult = resolver.resolve(JkJavaBuild.COMPILE);
        assertEquals(8, resolveResult.moduleFiles().size());
    }

    @Test
    public void resolveInheritedScopes() {
        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.APACHE_COMMONS_DBCP, "1.4").scope(JkJavaBuild.COMPILE)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(REPOS, deps);

        // runtime classpath should embed the dependency as well cause 'RUNTIME' scope extends 'COMPILE'
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.RUNTIME);
        assertEquals(1, resolveResult.moduleFiles().size());
        assertTrue(resolveResult.contains(JkModuleId.of("commons-pool")));

        // test classpath should embed the dependency as well
        resolveResult = resolver.resolve(JkJavaBuild.TEST);
        assertTrue(resolveResult.contains(JkModuleId.of("commons-pool")));
        assertEquals(1, resolveResult.moduleFiles().size());
    }

    @Test
    public void resolveWithOptionals() {
        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.SPRING_ORM, "4.3.8.RELEASE").mapScope(JkJavaBuild.COMPILE).to("compile", "master", "optional")
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps);
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.COMPILE);
        assertEquals(37, resolveResult.moduleFiles().size());
    }

    @Test
    public void resolveSpringbootTestStarter() {
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").mapScope(JkJavaBuild.TEST).to("master", "runtime")
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps);
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.TEST);
        assertEquals(25, resolveResult.moduleFiles().size());
        assertTrue(resolveResult.contains(JkPopularModules.JUNIT));
    }



}
