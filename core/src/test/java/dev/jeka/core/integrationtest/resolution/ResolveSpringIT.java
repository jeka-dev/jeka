package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResolveSpringIT {

    private static final String SPRINGBOOT_STARTER = "org.springframework.boot:spring-boot-starter:1.5.13.RELEASE";

    private static final JkModuleId SLF4J_API = JkModuleId.of("org.slf4j:slf4j-api");

    @Test
    void resolveCompile() {
        //JkLog.setHierarchicalConsoleConsumer();
        final JkResolveResult result = resolver().resolve(JkDependencySet.of(SPRINGBOOT_STARTER));
        System.out.println(result.getDependencyTree().toStringTree());
        result.getFiles().forEach(System.out::println);
        final List<JkResolvedDependencyNode> slf4japiNodes = result.getDependencyTree().toFlattenList().stream()
                .filter(node -> node.getModuleInfo().getModuleId().equals(SLF4J_API)).collect(Collectors.toList());
        for (final JkResolvedDependencyNode slf4japiNode : slf4japiNodes) {
            System.out.println("---------------------");
            System.out.println(slf4japiNode);
            slf4japiNode.getResolvedFiles().forEach(System.out::println);
        }

        // Does not contains test-jars
        Assertions.assertFalse(result.getFiles().getEntries().stream().anyMatch(path -> path.getFileName().toString().endsWith("-tests.jar")));
    }

    private JkDependencyResolver resolver() {
        return JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
    }

    @Test
    void resolveSpringbootTestStarter() {
        final JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE", JkTransitivity.RUNTIME);
        final JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        final Set<JkModuleId> moduleIds = resolveResult.getDependencyTree().getResolvedVersions().getModuleIds();

        // According presence or absence of cache it could be 24 or 25
        assertTrue(moduleIds.size() >= 24, "Wrong modules size " + moduleIds);
        assertTrue(moduleIds.size() <= 25, "Wrong modules size " + moduleIds);
    }
}
