package org.jerkar.api.depmanagement;

import static org.jerkar.api.depmanagement.JkScopedDependencyTest.COMPILE;
import static org.jerkar.api.depmanagement.JkScopedDependencyTest.RUNTIME;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

/**
 * @formatter:off
 * @author djeang
 * 
 */
@SuppressWarnings("javadoc")
public class JkDependenciesTest {

    @Test
    public void testBuilder() {
        final JkScopeMapping run2run = JkScopeMapping.of(RUNTIME).to(RUNTIME);
        final JkDependencies deps = JkDependencies.builder()
                .on("hibernate:hjmlm:1212.0").on("spring:spring:6.3").scope(COMPILE)
                .on(secondaryDeps()).on("klklkl:lklk:mlml").on("hhhhh:ll:ppp")
                .on(JkModuleId.of("lmlmlm", "mùmùmù"), JkVersionRange.of("5454")).build().withDefaultScope(run2run);
        final JkScopedDependency springDependency = deps.get(JkModuleId.of("spring", "spring"));
        Assert.assertEquals(JkUtilsIterable.setOf(COMPILE), springDependency.scopes());
        final JkScopedDependency hibernateDep = deps.get(JkModuleId.of("hibernate", "hjmlm"));
        Assert.assertEquals(run2run, hibernateDep.scopeMapping());
        final JkScopedDependency llDep = deps.get(JkModuleId.of("hhhhh", "ll"));
        Assert.assertEquals(run2run, llDep.scopeMapping());
    }

    @Test
    public void testEquals() {
        final JkScopeMapping run2runA = JkScopeMapping.of(RUNTIME).to(COMPILE);
        final JkScopeMapping run2runB = JkScopeMapping.of(RUNTIME).to(COMPILE);
        Assert.assertEquals(run2runA, run2runB);
    }

    private JkDependencies secondaryDeps() {
        return JkDependencies.builder().on("454545:5445:54545").on("lkll:llljk:poo").build();
    }

    @Test
    public void onProject() throws IOException {
        Path root = Files.createTempDirectory("jerkartestproject");
        JkJavaProject javaProject = new JkJavaProject(root);
        JkDependencies dependencies = JkDependencies.of().and(javaProject);
        JkComputedDependency computedDependency = (JkComputedDependency) dependencies.list().get(0).dependency();
        Assert.assertEquals(root, computedDependency.ideProjectBaseDir());
    }

}
