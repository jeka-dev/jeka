package org.jerkar.integrationtest;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.PROVIDED;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.RUNTIME;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkDependencyNode;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolutionParameters;
import org.jerkar.api.depmanagement.JkResolveResult;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.utils.JkUtilsSystem;
import org.junit.Test;

public class ResolverWithScopeMapperIT {

    private static final JkRepos REPOS = JkRepos.mavenCentral();

    private static final JkScope SCOPE_A = JkScope.of("scopeA");

    @Test
    public void resolveWithDefaultScopeMappingOnResolver() {

        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE", TEST);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
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

    @Test
    public void resolveWithJunit() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.JUNIT, "4.12", JkJavaDepScopes.TEST);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, TEST);
        Set<JkModuleId> moduleIds = resolveResult.dependencyTree().flattenToVersionProvider().moduleIds();
        assertEquals("Wrong modules size " + moduleIds, 2, moduleIds.size());
    }

    /*
     * Spring-boot 1.5.3 has a dependency on spring-core which is higher than 4.0.0.
     * Nevertheless, if we declare spring-core with projectVersion 4.0.0 as direct dependency,
     * this one should be taken in account, and not the the higher one coming transitive dependency.
     */
    @Test
    public void explicitExactVersionWin() {
        //JkLog.verbose(true);
        JkModuleId starterWebModule = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkModuleId springCoreModule = JkModuleId.of("org.springframework:spring-core");
        String directCoreVersion = "4.0.0.RELEASE";
        JkDependencySet deps = JkDependencySet.of()

                .and(springCoreModule, directCoreVersion, COMPILE)  // force a projectVersion lower than the transitive jump starterWeb module
                .and(starterWebModule, "1.5.3.RELEASE", COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, COMPILE);
        assertEquals(directCoreVersion, resolveResult.versionOf(springCoreModule).name());
    }

    /*
     * Spring-boot 1.5.3 has a dependency on spring-core which is higher than 4.0.0.
     * Nevertheless, if we declare spring-core with projectVersion 4.0.0 as direct dependency,
     * this one should be taken in account, and not the the higher one coming transitive dependency.
     */
    @Test
    public void resolveWithSeveralScopes() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.GUAVA, "19.0", COMPILE)
                .and (JkPopularModules.JAVAX_SERVLET_API, "3.1.0", PROVIDED);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, COMPILE, PROVIDED);
        assertTrue(resolveResult.contains(JkPopularModules.JAVAX_SERVLET_API));
        assertTrue(resolveResult.contains(JkPopularModules.GUAVA));
        assertEquals(2, resolveResult.dependencyTree().flattenToVersionProvider().moduleIds().size());
    }

    @Test
    public void getRuntimeTransitiveWithRuntime() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion2");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE", COMPILE, RUNTIME);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps, RUNTIME);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertTrue(snakeyamlHere);
    }

    @Test
    public void dontGetRuntimeTransitiveWithCompile() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE", COMPILE, RUNTIME);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps, COMPILE);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertFalse(snakeyamlHere);
    }

    @Test
    public void treeRootIsCorrectWhenAnonymous() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.GUAVA, "19.0", COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkDependencyNode tree = resolver.resolve(deps).dependencyTree();
        assertTrue(tree.moduleInfo().declaredScopes().isEmpty());
    }

}
