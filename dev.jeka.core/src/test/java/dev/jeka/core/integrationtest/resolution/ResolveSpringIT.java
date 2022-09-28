package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.JkCoordinate.GroupAndName;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class ResolveSpringIT {

    private static final String SPRINGBOOT_STARTER = "org.springframework.boot:spring-boot-starter:1.5.13.RELEASE";

    private static final GroupAndName SLF4J_API = GroupAndName.of("org.slf4j:slf4j-api");

    @Test
    public void resolveCompile() {
        //JkLog.setHierarchicalConsoleConsumer();
        final JkResolveResult result = resolver().resolve(JkDependencySet.of(SPRINGBOOT_STARTER));
        System.out.println(result.getDependencyTree().toStringTree());
        result.getFiles().forEach(System.out::println);
        final List<JkResolvedDependencyNode> slf4japiNodes = result.getDependencyTree().toFlattenList().stream()
                .filter(node -> node.getModuleInfo().getGroupAndName().equals(SLF4J_API)).collect(Collectors.toList());
        for (final JkResolvedDependencyNode slf4japiNode : slf4japiNodes) {
            System.out.println("---------------------");
            System.out.println(slf4japiNode);
            slf4japiNode.getResolvedFiles().forEach(System.out::println);
        }

        // Does not contains test-jars
        Assert.assertFalse(result.getFiles().getEntries().stream().anyMatch(path -> path.getFileName().toString().endsWith("-tests.jar")));
    }

    private JkDependencyResolver resolver() {
        return JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
    }

    @Test
    public void resolveSpringbootTestStarter() {
        final JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE", JkTransitivity.RUNTIME);
        final JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        final Set<GroupAndName> moduleIds = resolveResult.getDependencyTree().getResolvedVersions().getGroupAndNames();

        // According presence or absence of cache it could be 24 or 25
        assertTrue("Wrong modules size " + moduleIds,  moduleIds.size() >= 24);
        assertTrue("Wrong modules size " + moduleIds,  moduleIds.size() <= 25);
    }
}
