package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DependencySetResolutionIT {

    private static final String SPRINGBOOT_TEST = "org.springframework.boot:spring-boot-test";

    private static final String SPRINGBOOT_TEST_AND_VERSION = SPRINGBOOT_TEST + ":1.5.3.RELEASE";

    // Autoconfigure is a transitive dependency of springboot-test
    private static final String AUTOCONFIGURE = "org.springframework.boot:spring-boot-test-autoconfigure";

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
        assertTrue(resolveResult.contains(JkModuleId.of(SPRINGBOOT_TEST)));
        assertTrue(resolveResult.contains(JkModuleId.of(AUTOCONFIGURE)));
    }

    @Test
    public void resolve_transitiveDependencyLocallyExcluded_ok() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of(SPRINGBOOT_TEST_AND_VERSION).andExclude(AUTOCONFIGURE));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        assertTrue(resolveResult.contains(JkModuleId.of(SPRINGBOOT_TEST)));
        assertFalse(resolveResult.contains(JkModuleId.of(AUTOCONFIGURE)));
    }
}
