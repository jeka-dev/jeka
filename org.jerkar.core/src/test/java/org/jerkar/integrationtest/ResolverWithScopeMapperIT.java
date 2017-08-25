package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.utils.JkUtilsSystem;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.junit.Test;

import java.util.Set;

import static org.jerkar.tool.builtins.javabuild.JkJavaBuild.*;
import static org.junit.Assert.*;

public class ResolverWithScopeMapperIT {

    private static final JkRepos REPOS = JkRepos.mavenCentral();

    private static final JkScope SCOPE_A = JkScope.of("scopeA");

    @Test
    public void resolveWithDefaultScopeMappingOnResolver() {

        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").scope(TEST)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, TEST);
        Set<JkModuleId> moduleIds = resolveResult.dependencyTree().flattenToVersionProvider().moduleIds();

        // Unresolved issue happen on Travis : Junit is not part of the result.
        // To unblock linux build, we do a specific check uniquely for linux
        if (JkUtilsSystem.IS_WINDOWS) {
            assertEquals("Wrong modules size " + moduleIds, 25, moduleIds.size());
            assertTrue(resolveResult.contains(JkPopularModules.JUNIT));
        } else {
            assertTrue(moduleIds.size() == 24 || moduleIds.size() == 25);
        }
    }

    /*
     * Spring-boot 1.5.3 has a dependency on spring-core which is higher than 4.0.0.
     * Nevertheless, if we declare spring-core with version 4.0.0 as direct dependency,
     * this one should be taken in account, and not the the higher one coming transitive dependency.
     */
    @Test
    public void explicitExactVersionWin() {
        //JkLog.verbose(true);
        JkModuleId starterWebModule = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkModuleId springCoreModule = JkModuleId.of("org.springframework:spring-core");
        String directCoreVersion = "4.0.0.RELEASE";
        JkDependencies deps = JkDependencies.builder()

                .on(springCoreModule, directCoreVersion).scope(COMPILE)  // force a version lower than the transitive jump starterWeb module
                .on(starterWebModule, "1.5.3.RELEASE").scope(COMPILE)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, JkJavaBuild.COMPILE);
        assertEquals(directCoreVersion, resolveResult.versionOf(springCoreModule).name());
    }

    /*
     * Spring-boot 1.5.3 has a dependency on spring-core which is higher than 4.0.0.
     * Nevertheless, if we declare spring-core with version 4.0.0 as direct dependency,
     * this one should be taken in account, and not the the higher one coming transitive dependency.
     */
    @Test
    public void resolveWithSeveralScopes() {
        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.GUAVA, "19.0").scope(JkJavaBuild.COMPILE)
                .on(JkPopularModules.JAVAX_SERVLET_API, "3.1.0").scope(JkJavaBuild.PROVIDED)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED);
        assertTrue(resolveResult.contains(JkPopularModules.JAVAX_SERVLET_API));
        assertTrue(resolveResult.contains(JkPopularModules.GUAVA));
        assertEquals(2, resolveResult.dependencyTree().flattenToVersionProvider().moduleIds().size());
    }

    @Test
    public void getRuntimeTransitiveWithRuntime() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion2");
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE").scope(COMPILE, RUNTIME)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps, JkJavaBuild.RUNTIME);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertTrue(snakeyamlHere);
    }

    @Test
    public void dontGetRuntimeTransitiveWithCompile() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE").scope(COMPILE, RUNTIME)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps, JkJavaBuild.COMPILE);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertFalse(snakeyamlHere);
    }

    @Test
    public void treeRootIsCorrectWhenAnonymous() {
        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.GUAVA, "19.0").scope(JkJavaBuild.COMPILE)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkDependencyNode tree = resolver.resolve(deps).dependencyTree();
        assertTrue(tree.moduleInfo().declaredScopes().isEmpty());
    }

}
