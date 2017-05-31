package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.junit.Ignore;
import org.junit.Test;

import javax.naming.spi.ResolveResult;
import java.util.List;
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
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(TEST);
        assertEquals(25, resolveResult.moduleFiles().size());
        assertTrue(resolveResult.contains(JkPopularModules.JUNIT));
    }

    @Test
    public void resolveWithSeveralScopes() {
        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.GUAVA, "19.0").scope(JkJavaBuild.COMPILE)
                .on(JkPopularModules.JAVAX_SERVLET_API, "3.1.0").scope(JkJavaBuild.PROVIDED)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED);
        assertTrue(resolveResult.contains(JkPopularModules.JAVAX_SERVLET_API));
        assertTrue(resolveResult.contains(JkPopularModules.GUAVA));
        assertEquals(2, resolveResult.moduleFiles().size());
    }

    @Test
    public void treeIsCorrect() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE").scope(COMPILE_AND_RUNTIME)
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").scope(TEST)
                .on("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0").scope(COMPILE)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkDependencyNode tree = resolver.resolve(JkJavaBuild.TEST).dependencyTree();
        JkScopedDependency root = tree.asScopedDependency();
        assertTrue(root.scopes().isEmpty());
        assertEquals(holder.moduleId(), tree.asModuleDependency().moduleId());
        assertEquals(3, tree.children().size());

        JkDependencyNode starterwebNode = tree.children().get(0);
        assertEquals(JkModuleId.of("org.springframework.boot:spring-boot-starter-web"), starterwebNode.asModuleDependency().moduleId());
        assertEquals(2, starterwebNode.asScopedDependency().scopes().size());
        assertTrue(starterwebNode.asScopedDependency().scopes().contains(COMPILE));
        assertTrue(starterwebNode.asScopedDependency().scopes().contains(RUNTIME));

        JkDependencyNode starterNode = starterwebNode.children().get(0);
        assertEquals(2, starterNode.asScopedDependency().scopes().size());
        Set<JkScope> scopes = starterNode.asScopedDependency().scopes();
        assertTrue(scopes.contains(COMPILE));
        assertTrue(scopes.contains(RUNTIME));

        JkDependencyNode snakeYamlNode = starterNode.children().get(4);
        assertEquals(1, snakeYamlNode.asScopedDependency().scopes().size());
        scopes = snakeYamlNode.asScopedDependency().scopes();
        assertTrue(scopes.contains(RUNTIME));

        System.out.println(tree.toStringComplete());
    }

    @Test
    public void getRuntimeTransitiveWithRuntime() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE").scope(COMPILE, RUNTIME)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.RUNTIME);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertTrue(snakeyamlHere);
    }

    @Test
    public void dontGetRuntimeTransitiveWithCompile() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter:1.5.3.RELEASE").scope(COMPILE, RUNTIME)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.COMPILE);
        boolean snakeyamlHere = resolveResult.contains( JkModuleId.of("org.yaml:snakeyaml"));
        assertFalse(snakeyamlHere);
    }


    @Test
    public void treeRootIsCorrectWhenAnonymous() {
        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.GUAVA, "19.0").scope(JkJavaBuild.COMPILE)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkDependencyNode tree = resolver.resolve().dependencyTree();
        JkScopedDependency root = tree.asScopedDependency();
        assertTrue(root.scopes().isEmpty());
    }






}
