package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.*;
import org.junit.Test;

import java.util.List;

import static dev.jeka.core.api.depmanagement.tooling.JkScope.COMPILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by angibaudj on 27-07-17.
 */
public class TransitiveExcludeIT {

    private static final String BOOT_TEST = "org.springframework.boot:spring-boot-test";

    private static final String BOOT_TEST_AND_VERSION = BOOT_TEST + ":1.5.3.RELEASE";

    @Test
    public void handleNonTransitive() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of(BOOT_TEST_AND_VERSION).withTransitivity(JkTransitivity.NONE));
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        List<JkDependencyNode> nodes = resolveResult.getDependencyTree().toFlattenList();
        assertEquals(1, nodes.size());
    }

    @Test
    public void handleExcludes() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of(BOOT_TEST_AND_VERSION)
                        .andExclude(BOOT_TEST)
                        .andExclude("org.springframework.boot:spring-boot-test-autoconfigure"));
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);
        assertFalse(resolveResult.contains(JkModuleId.of(BOOT_TEST)));
    }

    @Test
    public void handleGlobalExcludes() {

        JkDepExclude exclude = JkDepExclude.of(BOOT_TEST).withScopes(COMPILE);
        JkDependencySet deps = JkDependencySet.of()
                .and(BOOT_TEST_AND_VERSION, COMPILE)
                .withGlobalExclusion(exclude);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps, COMPILE);  // works with non empty scopes resolution
        assertFalse(resolveResult.contains(JkModuleId.of(BOOT_TEST)));

        // Test with JkDepExclude without scope specified of the exclusion

        exclude = JkDepExclude.of(BOOT_TEST);
        deps = JkDependencySet.of()
                .and(BOOT_TEST_AND_VERSION, COMPILE)
                .withGlobalExclusion(exclude);
        resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        resolveResult = resolver.resolve(deps);  // works with non empty scopes resolution
        assertFalse(resolveResult.contains(JkModuleId.of(BOOT_TEST)));
        resolveResult = resolver.resolve(deps);  // works also with empty socpes resolution
        assertFalse(resolveResult.contains(JkModuleId.of(BOOT_TEST)));
    }

}
