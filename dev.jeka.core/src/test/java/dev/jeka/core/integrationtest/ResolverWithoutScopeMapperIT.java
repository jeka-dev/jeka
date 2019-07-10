package dev.jeka.core.integrationtest;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static dev.jeka.core.api.depmanagement.JkScopedDependencyTest.COMPILE;
import static dev.jeka.core.api.depmanagement.JkScopedDependencyTest.RUNTIME;
import static dev.jeka.core.api.depmanagement.JkScopedDependencyTest.TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import dev.jeka.core.api.depmanagement.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkResolutionParameters;
import dev.jeka.core.api.depmanagement.JkResolveResult;
import dev.jeka.core.api.depmanagement.JkScopeMapping;

public class ResolverWithoutScopeMapperIT {

    private static final JkRepoSet REPOS = JkRepo.ofMavenCentral().toSet();

    @Test
    public void resolveCompile() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.APACHE_COMMONS_DBCP, "1.4", COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of(REPOS)
                .withParams(JkResolutionParameters.of(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, COMPILE);
        assertTrue(resolveResult.contains(JkModuleId.of("commons-pool")));
        assertEquals(2, resolveResult.getDependencyTree().getResolvedVersions().getModuleIds().size());

        deps = JkDependencySet.of()
                .and(JkPopularModules.HIBERNATE_CORE, "5.2.10.Final", COMPILE);
        resolver = JkDependencyResolver.of(REPOS)
                .withParams(JkResolutionParameters.of(DEFAULT_SCOPE_MAPPING));
        resolveResult = resolver.resolve(deps, COMPILE);
        System.out.println(resolveResult.getDependencyTree().toStringTree());
        assertEquals(10, resolveResult.getDependencyTree().getResolvedVersions().getModuleIds().size());
    }

    @Test
    public void resolveInheritedScopes() {
        final JkDependencySet deps = JkDependencySet.of().and(JkPopularModules.APACHE_COMMONS_DBCP, "1.4", COMPILE);
        final JkDependencyResolver resolver = JkDependencyResolver.of(REPOS)
                .withParams(JkResolutionParameters.of(DEFAULT_SCOPE_MAPPING));

        // runtime classpath should embed the dependency as well cause 'RUNTIME' scope extends 'COMPILE'
        JkResolveResult resolveResult = resolver.resolve(deps, RUNTIME);
        assertEquals(2, resolveResult.getDependencyTree().getResolvedVersions().getModuleIds().size());
        assertTrue(resolveResult.contains(JkModuleId.of("commons-pool")));
        assertTrue(resolveResult.contains(JkModuleId.of("commons-dbcp")));

        // test classpath should embed the dependency as well
        resolveResult = resolver.resolve(deps, TEST);
        assertTrue(resolveResult.contains(JkModuleId.of("commons-pool")));
        assertTrue(resolveResult.contains(JkModuleId.of("commons-dbcp")));
        assertEquals(2, resolveResult.getDependencyTree().getResolvedVersions().getModuleIds().size());
    }

    @Test
    public void resolveWithOptionals() {
        final JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.SPRING_ORM, "4.3.8.RELEASE", JkScopeMapping.of(COMPILE).to("compile", "master", "optional"));
        final JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet());
        final JkResolveResult resolveResult = resolver.resolve(deps, COMPILE);
        System.out.println(resolveResult.getDependencyTree().toStringTree());
        assertEquals(37, resolveResult.getDependencyTree().getResolvedVersions().getModuleIds().size());
    }

    @Test
    public void resolveSpringbootTestStarter() {
        final JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE", JkScopeMapping.of(TEST).to("master", "runtime"));
        final JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet());
        final JkResolveResult resolveResult = resolver.resolve(deps, TEST);
        final Set<JkModuleId> moduleIds = resolveResult.getDependencyTree().getResolvedVersions().getModuleIds();
        assertEquals("Wrong modules size " + moduleIds, 24, moduleIds.size());

    }

}
