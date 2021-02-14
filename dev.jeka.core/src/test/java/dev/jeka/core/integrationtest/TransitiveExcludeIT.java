package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Created by angibaudj on 27-07-17.
 */
public class TransitiveExcludeIT {

    private static final String BOOT_TEST = "org.springframework.boot:spring-boot-test";

    private static final String BOOT_TEST_AND_VERSION = BOOT_TEST + ":1.5.3.RELEASE";





    @Test
    public void handleGlobalExcludes() {
        JkDependencyExclusion exclude = JkDependencyExclusion.of(BOOT_TEST);
        JkDependencySet deps = JkDependencySet.of()
                .and(BOOT_TEST_AND_VERSION)
                .andGlobalExclusion(exclude);
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(deps);  // works with non empty scopes resolution
        assertFalse(resolveResult.contains(JkModuleId.of(BOOT_TEST)));

        // Test with JkDependencyExclusion without scope specified of the exclusion

        exclude = JkDependencyExclusion.of(BOOT_TEST);
        deps = JkDependencySet.of()
                .and(BOOT_TEST_AND_VERSION)
                .andGlobalExclusion(exclude);
        resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral());
        resolveResult = resolver.resolve(deps);  // works with non empty scopes resolution
        assertFalse(resolveResult.contains(JkModuleId.of(BOOT_TEST)));
        resolveResult = resolver.resolve(deps);  // works also with empty socpes resolution
        assertFalse(resolveResult.contains(JkModuleId.of(BOOT_TEST)));
    }

}
