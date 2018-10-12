package org.jerkar.integrationtest;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.jerkar.api.depmanagement.*;
import org.junit.Test;

/**
 * Created by angibaudj on 27-07-17.
 */
public class TransitiveExcludeIT {

    @Test
    public void handleNonTransitive() {

        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").isTransitive(false));
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet())
                .withParams(JkResolutionParameters.of(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps);
        List<JkDependencyNode> nodes = resolveResult.getDependencyTree().toFlattenList();
        assertEquals(1, nodes.size());
    }

    @Test
    public void handleExcludes() {
        JkDependencySet deps = JkDependencySet.of()
                .and(JkModuleDependency.of("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE")
                        .andExclude("org.springframework.boot:spring-boot-test")
                        .andExclude("org.springframework.boot:spring-boot-test-autoconfigure"));
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet())
                .withParams(JkResolutionParameters.of(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps);
        List<JkDependencyNode> nodes = resolveResult.getDependencyTree().toFlattenList();
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));
    }

    @Test
    public void handleGlobalExcludes() {
        JkDepExclude exclude = JkDepExclude.of("org.springframework.boot:spring-boot-test").withScopes(COMPILE);
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE", COMPILE)
                .withGlobalExclusion(exclude);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet())
                .withParams(JkResolutionParameters.of(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, COMPILE);  // works with non empty scopes resolution
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));

        resolveResult = resolver.resolve(deps);  // works also with empty socpes resolution
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));

        // Test with JkDepExclude without scope specified of the exclusion

        exclude = JkDepExclude.of("org.springframework.boot:spring-boot-test");
        deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE", COMPILE)
                .withGlobalExclusion(exclude);
        resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral().toSet())
                .withParams(JkResolutionParameters.of(DEFAULT_SCOPE_MAPPING));
        resolveResult = resolver.resolve(deps);  // works with non empty scopes resolution
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));
        resolveResult = resolver.resolve(deps);  // works also with empty socpes resolution
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));
    }


}
