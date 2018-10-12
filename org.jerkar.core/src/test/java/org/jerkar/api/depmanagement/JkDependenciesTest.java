package org.jerkar.api.depmanagement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

import static org.jerkar.api.depmanagement.JkScopedDependencyTest.*;

/**
 * @formatter:off
 * @author djeang
 * 
 */
public class JkDependenciesTest {

    @Test
    public void testAnd() {
        final JkScopeMapping run2run = JkScopeMapping.of(RUNTIME).to(RUNTIME);
        final JkDependencySet deps = JkDependencySet.of()
                .and("hibernate:hjmlm:1212.0")
                .and("spring:spring:6.3", COMPILE)
                .and(secondaryDeps())
                .and("klklkl:lklk:mlml")
                .and("hhhhh:ll:ppp").withDefaultScope(run2run);
        final JkScopedDependency springDependency = deps.get(JkModuleId.of("spring", "spring"));
        Assert.assertEquals(JkUtilsIterable.setOf(COMPILE), springDependency.getScopes());
        final JkScopedDependency hibernateDep = deps.get(JkModuleId.of("hibernate", "hjmlm"));
        Assert.assertEquals(run2run, hibernateDep.getScopeMapping());
        final JkScopedDependency llDep = deps.get(JkModuleId.of("hhhhh", "ll"));
        Assert.assertEquals(run2run, llDep.getScopeMapping());
    }

    @Test
    public void testEquals() {
        final JkScopeMapping run2runA = JkScopeMapping.of(RUNTIME).to(COMPILE);
        final JkScopeMapping run2runB = JkScopeMapping.of(RUNTIME).to(COMPILE);
        Assert.assertEquals(run2runA, run2runB);
    }

    private JkDependencySet secondaryDeps() {
        return JkDependencySet.of().and("454545:5445:54545").and("lkll:llljk:poo");
    }

    @Test
    public void onProject() throws IOException {
        Path root = Files.createTempDirectory("jerkartestproject");
        JkJavaProject javaProject = JkJavaProject.ofMavenLayout(root);
        JkDependencySet dependencies = JkDependencySet.of().and(javaProject);
        JkComputedDependency computedDependency = (JkComputedDependency) dependencies.toList().get(0).getDependency();
        Assert.assertEquals(root, computedDependency.getIdeProjectBaseDir());
    }

}
