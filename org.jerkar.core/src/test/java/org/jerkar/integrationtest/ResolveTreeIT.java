package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.jerkar.tool.builtins.javabuild.JkJavaBuild.*;
import static org.jerkar.tool.builtins.javabuild.JkJavaBuild.COMPILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by angibaudj on 19-06-17.
 */
public class ResolveTreeIT {

    @Test
    public void treeIsCorrect() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE").scope(COMPILE_AND_RUNTIME)
                .on("org.springframework.boot:spring-boot-starter-test:1.5.+").scope(TEST)
                .on("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0").scope(COMPILE)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkDependencyNode tree = resolver.resolve().dependencyTree();

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
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        JkModuleId moduleId = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkDependencies deps = JkDependencies.builder()
                .on(moduleId, "1.5.+").scope(TEST)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkDependencyNode tree = resolver.resolve(JkJavaBuild.TEST).dependencyTree();

        JkDependencyNode.ModuleNodeInfo moduleNodeInfo = tree.find(moduleId).moduleInfo();
        assertTrue(moduleNodeInfo.declaredVersion().definition().equals("1.5.+"));
        String resolvedVersionName = moduleNodeInfo.resolvedVersion().name();
        assertTrue(resolvedVersionName.startsWith("1.5"));
        assertFalse(JkVersionRange.of(resolvedVersionName).isDynamic());
    }

    @Test
    public void treeHandleMultipleVersionWithLastestVersionWinConflictManager() {
        JkModuleId starterWebModule = JkModuleId.of("org.springframework.boot:spring-boot-starter-web");
        JkModuleId springCoreModule = JkModuleId.of("org.springframework:spring-core");
        String directCoreVersion = "4.0.0.RELEASE";
        JkDependencies deps = JkDependencies.builder()
                .on(starterWebModule, "1.5.3.RELEASE").scope(COMPILE)
                .on(springCoreModule, directCoreVersion).scope(COMPILE)  // force a version lower than the transitive above
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.COMPILE);
        JkDependencyNode tree = resolveResult.dependencyTree();

        JkDependencyNode bootNode = tree.children().get(0);
        JkDependencyNode.ModuleNodeInfo springCoreTransitiveModuleNodeInfo = bootNode.find(springCoreModule).moduleInfo();
        assertEquals("4.3.8.RELEASE", springCoreTransitiveModuleNodeInfo.declaredVersion().definition());
        assertEquals(directCoreVersion, springCoreTransitiveModuleNodeInfo.resolvedVersion().name());  // cause evicted

        // As the spring-core version is declared as direct dependency and the declared version is exact (not dynamic)
        // then the resolved version should the one declared.
        JkDependencyNode.ModuleNodeInfo springCoreDirectModuleNodeInfo = tree.children().get(1).moduleInfo();
        assertEquals(directCoreVersion, springCoreDirectModuleNodeInfo.declaredVersion().definition());
        assertEquals(directCoreVersion, springCoreDirectModuleNodeInfo.resolvedVersion().name());

    }

    @Test
    public void treeplePlayAnd() {
        JkDependencies deps = JkDependencies.builder()
                .usingDefaultScopes(JkJavaBuild.COMPILE_AND_RUNTIME)
                .on("com.googlecode.playn", "playn-core", "1.4")
                .on("com.threerings", "tripleplay", "1.4").build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.RUNTIME);
        JkDependencyNode tree = resolveResult.dependencyTree();
        System.out.println(tree.toStringComplete());
        System.out.println(resolveResult.localFiles());

    }
}
