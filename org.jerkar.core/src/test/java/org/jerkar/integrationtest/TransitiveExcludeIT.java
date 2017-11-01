package org.jerkar.integrationtest;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.jerkar.api.depmanagement.JkDepExclude;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyNode;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolutionParameters;
import org.jerkar.api.depmanagement.JkResolveResult;
import org.junit.Test;

/**
 * Created by angibaudj on 27-07-17.
 */
public class TransitiveExcludeIT {

    @Test
    public void handleNonTransitive() {

        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").transitive(false)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps);
        List<JkDependencyNode> nodes = resolveResult.dependencyTree().flatten();
        assertEquals(1, nodes.size());
    }

    @Test
    public void handleExcludes() {
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE")
                        .excludeLocally("org.springframework.boot:spring-boot-test")
                        .excludeLocally("org.springframework.boot:spring-boot-test-autoconfigure")
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps);
        List<JkDependencyNode> nodes = resolveResult.dependencyTree().flatten();
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));
    }

    @Test
    public void handleGlobalExcludes() {
        JkDepExclude exclude = JkDepExclude.of("org.springframework.boot:spring-boot-test").scopes(COMPILE);
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").scope(COMPILE)
                .excludeGlobally(exclude)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(deps, COMPILE);  // works with non empty scopes resolution
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));

        resolveResult = resolver.resolve(deps);  // works also with empty socpes resolution
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));

        // Test with JkDepExclude without scope specified of the exclusion

        exclude = JkDepExclude.of("org.springframework.boot:spring-boot-test");
        deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").scope(COMPILE)
                .excludeGlobally(exclude)
                .build();
        resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING));
        resolveResult = resolver.resolve(deps);  // works with non empty scopes resolution
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));
        resolveResult = resolver.resolve(deps);  // works also with empty socpes resolution
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));
    }


}
