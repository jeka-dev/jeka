package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class DependencySetResolutionIT {

    private static final String SPRINGBOOT_TEST = "org.springframework.boot:spring-boot-test";

    private static final String SPRINGBOOT_TEST_AND_VERSION = SPRINGBOOT_TEST + ":1.5.3.RELEASE";

    // Commons-login is a transitive dependency of springboot-test
    private static final String COMMONS_LOGIN = "commons-logging:commons-logging";

    private static final String COMMONS_LOGIN_102 = "commons-logging:commons-logging:1.0.2";

    @Test
    public void resolve_unspecifiedVersionButPresentInProvider_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and("com.google.guava:guava")
                .withVersionProvider(JkVersionProvider.of("com.google.guava:guava", "22.0"));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        JkResolvedDependencyNode.JkModuleNodeInfo moduleNodeInfo = tree.getChildren().get(0).getModuleInfo();
        assertEquals("22.0", moduleNodeInfo.getDeclaredVersion().getValue());
    }

    @Test
    public void resolve_dependencyDeclaredAsNonTransitive_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of(SPRINGBOOT_TEST_AND_VERSION).withTransitivity(JkTransitivity.NONE));
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        List<JkResolvedDependencyNode> nodes = resolveResult.getDependencyTree().toFlattenList();
        assertEquals(1, nodes.size());
    }

    @Test
    public void resolve_transitiveDependency_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of(SPRINGBOOT_TEST_AND_VERSION));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        System.out.println(resolveResult.getFiles().toString());
        assertTrue(resolveResult.contains(JkModuleId.of(SPRINGBOOT_TEST)));
        assertTrue(resolveResult.contains(JkModuleId.of(COMMONS_LOGIN)));
    }

    @Test
    public void resolve_transitiveDependencyLocallyExcluded_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of(SPRINGBOOT_TEST_AND_VERSION).andExclusion(COMMONS_LOGIN));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        assertTrue(resolveResult.contains(JkModuleId.of(SPRINGBOOT_TEST)));
        assertFalse(resolveResult.contains(JkModuleId.of(COMMONS_LOGIN)));
    }

    @Test
    public void resolve_transitiveDependencyGloballyExcluded_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of(SPRINGBOOT_TEST_AND_VERSION))
                .andGlobalExclusion(COMMONS_LOGIN);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        assertTrue(resolveResult.contains(JkModuleId.of(SPRINGBOOT_TEST)));
        assertFalse(resolveResult.contains(JkModuleId.of(COMMONS_LOGIN)));
    }

    @Test
    public void resolve_moduleWithMainAndExtraArtifact_bothArtifactsArePresentInResult() {
        JkModuleDependency lwjgl= JkModuleDependency.of("org.lwjgl:lwjgl:3.1.1");
        JkModuleDependency lwjglLinux = lwjgl.withClassifier("natives-linux");
        JkDependencySet deps = JkDependencySet.of()
                .and(lwjglLinux)
                .and(COMMONS_LOGIN_102)
                .and(lwjgl);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolvedDependencyNode treeRoot = resolveResult.getDependencyTree();
        System.out.println(resolveResult.getFiles());
        System.out.println(treeRoot.toStringTree());

        // Even if there is 2 declared dependencies on lwjgl, as it is the same module (with different artifact),
        // it should results in a single node.
        // The classpath order will also place all artifacts of a same module sequentially.
        assertEquals(2, treeRoot.getChildren().size());
        assertEquals(3, treeRoot.getResolvedFiles().size());

        JkResolvedDependencyNode lwjglNode = treeRoot.getChildren().get(0);
        List<Path> lwjglFiles = lwjglNode.getNodeInfo().getFiles();
        System.out.println(lwjglFiles);
        assertEquals(2, lwjglFiles.size());

    }

}
