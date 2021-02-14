package dev.jeka.core.integrationtest;


import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class ResolverWithScopeMapperIT {

    @Test
    public void resolveWithDefaultScopeMappingOnResolver() {
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE");
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        Set<JkModuleId> moduleIds = resolveResult.getDependencyTree().getResolvedVersions().getModuleIds();

        // To unblock linux build, we do a specific check uniquely for linux
        if (JkUtilsSystem.IS_WINDOWS) {
            assertEquals("Wrong modules size " + moduleIds, 24, moduleIds.size());
        } else {
            assertTrue(moduleIds.size() == 24 || moduleIds.size() == 25);
        }
    }

    @Test
    public void resolveWithJunit() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.JUNIT.version("4.12"));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        Set<JkModuleId> moduleIds = resolveResult.getDependencyTree().getResolvedVersions().getModuleIds();
        assertEquals("Wrong modules size " + moduleIds, 2, moduleIds.size());
    }

    /*
     * Spring-boot 1.5.3 has a dependency on spring-core which is higher than 4.0.0.
     * Nevertheless, if we declare spring-core with version 4.0.0 as direct dependency,
     * this one should be taken in account, and not the the higher one coming transitive dependency.
     */
    @Test
    public void explicitExactVersionWin() {
        JkModuleId starterWebModule = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkModuleId springCoreModule = JkModuleId.of("org.springframework:spring-core");
        String directCoreVersion = "4.0.0.RELEASE";
        JkDependencySet deps = JkDependencySet.of()
                .and(springCoreModule.version(directCoreVersion))  // force a version lower than the transitive jump starterWeb module
                .and(starterWebModule.version("1.5.3.RELEASE"));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        assertEquals(directCoreVersion, resolveResult.getVersionOf(springCoreModule).getValue());
    }




    @Test
    public void getRuntimeTransitiveWithRuntime() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion2");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE", JkTransitivity.RUNTIME);
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertTrue(snakeyamlHere);
    }

    @Test
    public void dontGetRuntimeTransitiveWithCompile() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE", JkTransitivity.COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertFalse(snakeyamlHere);
    }

    @Test
    public void treeRootIsCorrectWhenAnonymous() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.GUAVA.version("19.0"));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolvedDependencyNode tree = resolver.resolve(deps).getDependencyTree();
        assertTrue(tree.getModuleInfo().getDeclaredScopes().isEmpty());
    }

}
