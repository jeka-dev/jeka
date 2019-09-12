package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ResolveWithDuplicateDependenciesTest {

    @Test
    public void testDuplicateDeclaredDependencies() {
        JkScopedDependency oldDep = JkScopedDependency.of(JkModuleDependency.of("com.google.guava:guava:21.0"),
                JkJavaDepScopes.PROVIDED);
        JkScopedDependency newDep = JkScopedDependency.of(JkModuleDependency.of("com.google.guava:guava:28.1-jre"),
                JkJavaDepScopes.PROVIDED);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral(), JkRepo.ofMavenJCenter());

        JkResolveResult result = resolver.resolve(
                JkDependencySet.of(Arrays.asList(oldDep, newDep))
                        .withVersionProvider(JkVersionProvider.of().and("com.google.guava:guava", "28.1-jre")),
                JkJavaDepScopes.SCOPES_FOR_COMPILATION).assertNoError();
        JkDependencyNode guavaNode = result.getDependencyTree().getChild(JkModuleId.of("com.google.guava:guava"));
        Assert.assertEquals("28.1-jre", guavaNode.getModuleInfo().getDeclaredVersion());
    }
}
