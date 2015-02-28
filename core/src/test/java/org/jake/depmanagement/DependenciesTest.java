package org.jake.depmanagement;

import static org.jake.java.build.JakeJavaBuild.COMPILE;
import static org.jake.java.build.JakeJavaBuild.RUNTIME;

import org.jake.utils.JakeUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

public class DependenciesTest {

    @Test
    public void testBuilder() {
        final JakeScopeMapping run2run = JakeScopeMapping.of(RUNTIME).to(RUNTIME);
        final JakeDependencies deps = JakeDependencies.builder()
                .usingDefaultScopeMapping(run2run)
                .on("hibernate:hjmlm:1212.0")
                .on("spring:spring:6.3").scope(COMPILE)
                .on(secondaryDeps())
                .on("klklkl:lklk:mlml")
                .on("hhhhh:ll:ppp")
                .on(JakeModuleId.of("lmlmlm:mùmùmù"), JakeVersionRange.of("5454")).build();
        final JakeScopedDependency springDependency = deps.get(JakeModuleId.of("spring:spring"));
        Assert.assertEquals(JakeUtilsIterable.setOf(COMPILE), springDependency.scopes());
        final JakeScopedDependency hibernateDep = deps.get(JakeModuleId.of("hibernate:hjmlm"));
        Assert.assertEquals(run2run, hibernateDep.scopeMapping());
        final JakeScopedDependency llDep = deps.get(JakeModuleId.of("hhhhh:ll"));
        Assert.assertEquals(run2run, llDep.scopeMapping());
    }

    @Test
    public void testEquals() {
        final JakeScopeMapping run2runA = JakeScopeMapping.of(RUNTIME).to(COMPILE);
        final JakeScopeMapping run2runB = JakeScopeMapping.of(RUNTIME).to(COMPILE);
        Assert.assertEquals(run2runA, run2runB);
    }

    private JakeDependencies secondaryDeps() {
        return JakeDependencies.builder()
                .on("454545:5445:54545")
                .on("lkll:llljk:poo").build();
    }



}
