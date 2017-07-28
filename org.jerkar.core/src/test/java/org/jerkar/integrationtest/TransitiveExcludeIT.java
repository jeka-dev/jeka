package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.utils.JkUtilsSystem;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.jerkar.tool.builtins.javabuild.JkJavaBuild.TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by angibaudj on 27-07-17.
 */
public class TransitiveExcludeIT {

    @Test
    public void handleNonTransitive() {

        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").transitive(false)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve();
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
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve();
        List<JkDependencyNode> nodes = resolveResult.dependencyTree().flatten();
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));
    }

    @Test
    public void handleGlobalExcludes() {
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE")
                .excludeGlobally("org.springframework.boot:spring-boot-test")
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.COMPILE);  // Does not work with empty scopes
        List<JkDependencyNode> nodes = resolveResult.dependencyTree().flatten();
        assertFalse(resolveResult.contains(JkModuleId.of("org.springframework.boot:spring-boot-test")));
    }


}
