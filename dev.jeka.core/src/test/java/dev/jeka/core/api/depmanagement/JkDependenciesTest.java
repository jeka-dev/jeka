package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @formatter:off
 * @author djeang
 * 
 */
public class JkDependenciesTest {

    @Test
    public void testAnd() {
        final JkScopeMapping run2run = JkScopeMapping.of(JkScopedDependencyTest.RUNTIME).to(JkScopedDependencyTest.RUNTIME.getName());
        final JkDependencySet deps = JkDependencySet.of()
                .and("hibernate:hjmlm:1212.0")
                .and("spring:spring:6.3", JkScopedDependencyTest.COMPILE)
                .and(secondaryDeps())
                .and("klklkl:lklk:mlml")
                .and("hhhhh:ll:ppp").withDefaultScope(run2run);
        final JkScopedDependency springDependency = deps.get(JkModuleId.of("spring", "spring"));
        Assert.assertEquals(JkUtilsIterable.setOf(JkScopedDependencyTest.COMPILE), springDependency.getScopes());
        final JkScopedDependency hibernateDep = deps.get(JkModuleId.of("hibernate", "hjmlm"));
        Assert.assertEquals(run2run, hibernateDep.getScopeMapping());
        final JkScopedDependency llDep = deps.get(JkModuleId.of("hhhhh", "ll"));
        Assert.assertEquals(run2run, llDep.getScopeMapping());
    }

    @Test
    public void testEquals() {
        final JkScopeMapping run2runA = JkScopeMapping.of(JkScopedDependencyTest.RUNTIME).to(JkScopedDependencyTest.COMPILE.getName());
        final JkScopeMapping run2runB = JkScopeMapping.of(JkScopedDependencyTest.RUNTIME).to(JkScopedDependencyTest.COMPILE.getName());
        Assert.assertEquals(run2runA, run2runB);
    }

    private JkDependencySet secondaryDeps() {
        return JkDependencySet.of().and("454545:5445:54545").and("lkll:llljk:poo");
    }

    @Test
    public void onProject() throws IOException {
        Path root = Files.createTempDirectory("jekatestproject");
        JkJavaProject javaProject = JkJavaProject.of()
            .setBaseDir(root);
        JkDependencySet dependencies = JkDependencySet.of()
                .and(javaProject);
        JkComputedDependency computedDependency = (JkComputedDependency) dependencies.toList().get(0).getDependency();
        Assert.assertEquals(root, computedDependency.getIdeProjectBaseDir());
    }

}
