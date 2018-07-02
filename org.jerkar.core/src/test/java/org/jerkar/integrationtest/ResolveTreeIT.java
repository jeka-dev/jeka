package org.jerkar.integrationtest;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE_AND_RUNTIME;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.RUNTIME;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.jerkar.api.depmanagement.*;
import org.junit.Test;

/**
 * Created by angibaudj on 19-06-17.
 */
public class ResolveTreeIT {

    @Test
    public void treeIsCorrect() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE", COMPILE_AND_RUNTIME)
                .and("org.springframework.boot:spring-boot-starter-test:1.5.+", TEST)
                .and("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0", COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkDependencyNode tree = resolver.resolve(deps).dependencyTree();

        System.out.println(tree.toStringComplete());

        JkDependencyNode.ModuleNodeInfo root = tree.moduleInfo();
        assertTrue(root.declaredScopes().isEmpty());
        assertEquals(holder.moduleId(), tree.moduleInfo().moduleId());
        assertEquals(3, tree.children().size());

        JkDependencyNode starterwebNode = tree.children().get(0);
        assertEquals(JkModuleId.of("org.springframework.boot:spring-boot-starter-web"), starterwebNode.moduleInfo().moduleId());
        assertEquals(2, starterwebNode.moduleInfo().declaredScopes().size());
        assertTrue(starterwebNode.moduleInfo().declaredScopes().contains(COMPILE));
        assertTrue(starterwebNode.moduleInfo().declaredScopes().contains(RUNTIME));

        JkDependencyNode starterNode = starterwebNode.children().get(0);
        assertEquals(2, starterNode.moduleInfo().declaredScopes().size());
        Set<JkScope> scopes = starterNode.moduleInfo().declaredScopes();
        assertTrue(scopes.contains(COMPILE));
        assertTrue(scopes.contains(RUNTIME));


        List<JkDependencyNode> snakeYamlNodes = starterNode.children(JkModuleId.of("org.yaml:snakeyaml"));
        assertEquals(1, snakeYamlNodes.size());
        JkDependencyNode snakeYamlNode = snakeYamlNodes.get(0);
        assertEquals(1, snakeYamlNode.moduleInfo().declaredScopes().size());
        scopes = snakeYamlNode.moduleInfo().declaredScopes();
        assertTrue(scopes.contains(RUNTIME));

        assertEquals(5, starterNode.children().size());

        List<JkDependencyNode> springCoreNodes = starterNode.children(JkModuleId.of("org.springframework:spring-core"));
        assertEquals(1, springCoreNodes.size());
        JkDependencyNode springCoreNode = springCoreNodes.get(0);
        List<JkDependencyNode> commonLoggingNodes = springCoreNode.children(JkModuleId.of("commons-logging:commons-logging"));
        assertEquals(1, commonLoggingNodes.size());

    }

    @Test
    public void treeDistinctDynamicAndResolvedVersion() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        JkModuleId moduleId = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkDependencySet deps = JkDependencySet.of().and(moduleId, "1.4.+", TEST);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkDependencyNode tree = resolver.resolve(deps, TEST).dependencyTree();
        System.out.println(tree.toStrings());
        JkDependencyNode.ModuleNodeInfo moduleNodeInfo = tree.find(moduleId).moduleInfo();
        assertTrue(moduleNodeInfo.declaredVersion().definition().equals("1.4.+"));
        String resolvedVersionName = moduleNodeInfo.resolvedVersion().name();
        assertEquals("1.4.7.RELEASE", resolvedVersionName);
    }

    @Test
    public void treeHandleMultipleVersionWithLastestVersionWinConflictManager() {
        JkModuleId starterWebModule = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkModuleId springCoreModule = JkModuleId.of("org.springframework:spring-core");
        String directCoreVersion = "4.3.6.RELEASE";
        JkDependencySet deps = JkDependencySet.of()
                .and(starterWebModule, "1.5.10.RELEASE", COMPILE)
                .and(springCoreModule, directCoreVersion, COMPILE);  // force a projectVersion lower than the transitive above
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, COMPILE);
        JkDependencyNode tree = resolveResult.dependencyTree();

        JkDependencyNode bootNode = tree.children().get(0);
        JkDependencyNode.ModuleNodeInfo springCoreTransitiveModuleNodeInfo = bootNode.find(springCoreModule).moduleInfo();
        assertEquals("4.3.14.RELEASE", springCoreTransitiveModuleNodeInfo.declaredVersion().definition());
        assertEquals(directCoreVersion, springCoreTransitiveModuleNodeInfo.resolvedVersion().name());  // cause evicted

        // As the spring-core projectVersion is declared as direct dependency and the declared projectVersion is exact (not dynamic)
        // then the resolved projectVersion should the one declared.
        JkDependencyNode.ModuleNodeInfo springCoreDirectModuleNodeInfo = tree.children().get(1).moduleInfo();
        assertEquals(directCoreVersion, springCoreDirectModuleNodeInfo.declaredVersion().definition());
        assertEquals(directCoreVersion, springCoreDirectModuleNodeInfo.resolvedVersion().name());

    }

    @Test
    public void triplePlayAnd() {
        JkDependencySet deps = JkDependencySet.of()
                .and("com.googlecode.playn:playn-core:1.4")
                .and("com.threerings:tripleplay:1.4")
                .withDefaultScope(COMPILE_AND_RUNTIME);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, RUNTIME);
        JkDependencyNode tree = resolveResult.dependencyTree();
        System.out.println(tree.toStringComplete());
        System.out.println(resolveResult.localFiles());
    }

    @Test
    public void versionProvider() {
        JkDependencySet deps = JkDependencySet.of()
                .and("com.google.guava:guava")
                .withVersionProvider(JkVersionProvider.of("com.google.guava:guava", "22.0"))
                .withDefaultScope(COMPILE_AND_RUNTIME);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, RUNTIME);
        JkDependencyNode tree = resolveResult.dependencyTree();
        JkDependencyNode.ModuleNodeInfo moduleNodeInfo = tree.children().get(0).moduleInfo();
        assertEquals("22.0", moduleNodeInfo.declaredVersion().definition());
    }

}
