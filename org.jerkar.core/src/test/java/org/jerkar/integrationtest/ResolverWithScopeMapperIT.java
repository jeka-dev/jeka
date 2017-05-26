package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResolverWithScopeMapperIT {

    private static final JkRepos REPOS = JkRepos.mavenCentral();

    @Test
    @Ignore
    // TODO fixit
    public void resolveSpringbootTestStarter() {
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").scope(JkJavaBuild.TEST)
                .usingDefaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps);
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.TEST);
        assertEquals(25, resolveResult.moduleFiles().size());
        assertTrue(resolveResult.contains(JkPopularModules.JUNIT));
    }

    @Test
    public void resolveWithDefaultScopeMappingOnResolver() {
        JkDependencies deps = JkDependencies.builder()
                .on("org.springframework.boot:spring-boot-starter-test:1.5.3.RELEASE").scope(JkJavaBuild.TEST)
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.managed(JkRepos.mavenCentral(), deps)
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING));
        JkResolveResult resolveResult = resolver.resolve(JkJavaBuild.TEST);
        assertEquals(25, resolveResult.moduleFiles().size());
        assertTrue(resolveResult.contains(JkPopularModules.JUNIT));
    }



}
