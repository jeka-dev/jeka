package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolutionParameters;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static dev.jeka.core.api.depmanagement.JkPopularLibs.GUAVA;
import static dev.jeka.core.api.depmanagement.JkPopularLibs.JAVAX_SERVLET_API;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QualifiedDependenciesResolutionIT {

    @Test
    public void resolve_qualifierWith2MasterConfigurations_ok() {
        JkQualifiedDependencySet deps = JkQualifiedDependencySet.of()
                .and("compile, runtime", "com.github.djeang:vincer-dom:1.3.0");
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolvedDependencyNode tree = resolver.resolve(deps, JkResolutionParameters.of()).getDependencyTree();
        assertEquals(1, tree.getChildren().size());
    }

    @Test
    public void resolve_computedIdeDependencies_ok() {
        JkDependencySet compile = JkDependencySet.of()
                .and(GUAVA.toCoordinate("19.0"))
                .and (JAVAX_SERVLET_API.toCoordinate(JkVersion.UNSPECIFIED))
                .andVersionProvider(JkVersionProvider.of(JAVAX_SERVLET_API, "4.0.1"));
        JkDependencySet runtime = compile.minus(JAVAX_SERVLET_API);
        JkProjectDependencies projectDependencies = JkProjectDependencies.of(compile, runtime, JkDependencySet.of());
        JkQualifiedDependencySet qdeps = JkQualifiedDependencySet.computeIdeDependencies(projectDependencies);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(qdeps, JkResolutionParameters.of());
        assertTrue(resolveResult.contains(JAVAX_SERVLET_API));
        assertTrue(resolveResult.contains(GUAVA));
        assertEquals(2, resolveResult.getDependencyTree().getResolvedVersions().getModuleIds().size());
    }

    @Test
    public void resolve_moduleAndFilesDependencies_resultTreePreservesOrder() throws URISyntaxException {
        JkCoordinate holder = JkCoordinate.of("mygroup:myname:myversion");
        Path dep0File = Paths.get(QualifiedDependenciesResolutionIT.class.getResource("dep0").toURI());
        Path dep1File = Paths.get(QualifiedDependenciesResolutionIT.class.getResource( "dep1").toURI());
        Path dep2File = Paths.get(QualifiedDependenciesResolutionIT.class.getResource( "dep2").toURI());
        JkQualifiedDependencySet deps = JkQualifiedDependencySet.of()
                .and("test", JkFileSystemDependency.of(dep0File))
                .and("compile, runtime", "org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE")
                .and("test", JkFileSystemDependency.of(dep1File))
                .and("compile", "com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0")
                .and("compile", JkFileSystemDependency.of(dep2File));
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder);
        JkResolvedDependencyNode tree = resolver.resolve(deps, JkResolutionParameters.of()).getDependencyTree();

        System.out.println(tree.toStringTree());

        JkResolvedDependencyNode.JkModuleNodeInfo root = tree.getModuleInfo();
        assertTrue(root.getDeclaredConfigurations().isEmpty());
        assertEquals(holder.getModuleId(), tree.getModuleInfo().getModuleId());
        assertEquals(5, tree.getChildren().size());

        JkResolvedDependencyNode file0Node = tree.getChildren().get(0);
        List<Path> expected = new LinkedList<>();
        expected.add(dep0File);
        assertEquals(expected, file0Node.getResolvedFiles());

        JkResolvedDependencyNode starterwebNode = tree.getChildren().get(1);
        assertEquals(JkModuleId.of("org.springframework.boot:spring-boot-starter-web"),
                starterwebNode.getModuleInfo().getModuleId());

        JkResolvedDependencyNode file1Node = tree.getChildren().get(2);
        List<Path> expected1 = new LinkedList<>();
        expected1.add(dep1File);
        assertEquals(expected1, file1Node.getResolvedFiles());

        JkResolvedDependencyNode jsonRpcNode = tree.getChildren().get(3);
        assertEquals(JkModuleId.of("com.github.briandilley.jsonrpc4j:jsonrpc4j"), jsonRpcNode.getModuleInfo().getModuleId());

        JkResolvedDependencyNode file2Node = tree.getChildren().get(4);
        List<Path> expected2 = new LinkedList<>();
        expected2.add(dep2File);
        assertEquals(expected2, file2Node.getResolvedFiles());

        // Now check that file dependencies with Test Scope are not present in compile

        tree = resolver.resolve(deps, JkResolutionParameters.of()).getDependencyTree();  // intilay was resolve on compile
        System.out.println(tree.toStringTree());

        root = tree.getModuleInfo();
        assertTrue(root.getDeclaredConfigurations().isEmpty());
        assertEquals(holder.getModuleId(), tree.getModuleInfo().getModuleId());
        assertEquals(5, tree.getChildren().size());
    }

    @Test
    public void resolve_manyModules_resultTreeIsCorrect() {
        JkCoordinate holder = JkCoordinate.of("mygroup:myname:myversion");
        JkQualifiedDependencySet deps = JkQualifiedDependencySet.of()
                .and("compile, runtime", "org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE")
                .and("test", "org.springframework.boot:spring-boot-starter-test:1.5.+")
                .and("compile", "com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0");
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder);
        JkResolvedDependencyNode tree = resolver.resolve(deps, JkResolutionParameters.of()).getDependencyTree();

        System.out.println(tree.toStringTree());

        JkResolvedDependencyNode.JkModuleNodeInfo root = tree.getModuleInfo();
        assertTrue(root.getDeclaredConfigurations().isEmpty());
        assertEquals(holder.getModuleId(), tree.getModuleInfo().getModuleId());
        assertEquals(3, tree.getChildren().size());

        JkResolvedDependencyNode starterwebNode = tree.getChildren().get(0);
        assertEquals(JkModuleId.of("org.springframework.boot:spring-boot-starter-web"),
                starterwebNode.getModuleInfo().getModuleId());
        assertEquals(2, starterwebNode.getModuleInfo().getDeclaredConfigurations().size());
        assertTrue(starterwebNode.getModuleInfo().getDeclaredConfigurations().contains("compile"));
        assertTrue(starterwebNode.getModuleInfo().getDeclaredConfigurations().contains("runtime"));

        JkResolvedDependencyNode starterNode = starterwebNode.getChildren().get(0);
        assertEquals(2, starterNode.getModuleInfo().getDeclaredConfigurations().size());
        Set<String> scopes = starterNode.getModuleInfo().getDeclaredConfigurations();
        assertTrue(scopes.contains("compile"));
        assertTrue(scopes.contains("runtime"));

        List<JkResolvedDependencyNode> snakeYamlNodes = starterNode.getChildren(JkModuleId.of("org.yaml:snakeyaml"));
        assertEquals(1, snakeYamlNodes.size());
        JkResolvedDependencyNode snakeYamlNode = snakeYamlNodes.get(0);
        assertEquals(1, snakeYamlNode.getModuleInfo().getDeclaredConfigurations().size());
        scopes = snakeYamlNode.getModuleInfo().getDeclaredConfigurations();
        assertTrue(scopes.contains("runtime"));

        assertEquals(5, starterNode.getChildren().size());

        List<JkResolvedDependencyNode> springCoreNodes = starterNode.getChildren(JkModuleId.of("org.springframework:spring-core"));
        assertEquals(1, springCoreNodes.size());
        JkResolvedDependencyNode springCoreNode = springCoreNodes.get(0);
        List<JkResolvedDependencyNode> commonLoggingNodes = springCoreNode.getChildren(JkModuleId.of("commons-logging:commons-logging"));
        assertEquals(1, commonLoggingNodes.size());
    }

    @Test
    public void resolve_manyModules_artifactCountIsCorrect() {
        JkCoordinate holder = JkCoordinate.of("mygroup:myname:myversion");
        JkQualifiedDependencySet deps = JkQualifiedDependencySet.of()
                .and("comple, runtime", "org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE")
                .and("test", "org.springframework.boot:spring-boot-starter-test:1.5.+")
                .and("compile", "com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0");
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps, JkResolutionParameters.of());
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        for (Path file : resolveResult.getFiles()) {
            if (!tree.getResolvedFiles().contains(file)) {
                System.out.println(file);
            }
        }
    }


}
