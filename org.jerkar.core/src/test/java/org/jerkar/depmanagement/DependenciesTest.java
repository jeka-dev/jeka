package org.jerkar.depmanagement;

import static org.jerkar.java.build.JkJavaBuild.COMPILE;
import static org.jerkar.java.build.JkJavaBuild.RUNTIME;

import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkModuleId;
import org.jerkar.depmanagement.JkScopeMapping;
import org.jerkar.depmanagement.JkScopedDependency;
import org.jerkar.depmanagement.JkVersionRange;
import org.jerkar.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

public class DependenciesTest {

    @Test
    public void testBuilder() {
        final JkScopeMapping run2run = JkScopeMapping.of(RUNTIME).to(RUNTIME);
        final JkDependencies deps = JkDependencies.builder()
                .usingDefaultScopeMapping(run2run)
                .on("hibernate:hjmlm:1212.0")
                .on("spring:spring:6.3").scope(COMPILE)
                .on(secondaryDeps())
                .on("klklkl:lklk:mlml")
                .on("hhhhh:ll:ppp")
                .on(JkModuleId.of("lmlmlm:mùmùmù"), JkVersionRange.of("5454")).build();
        final JkScopedDependency springDependency = deps.get(JkModuleId.of("spring:spring"));
        Assert.assertEquals(JkUtilsIterable.setOf(COMPILE), springDependency.scopes());
        final JkScopedDependency hibernateDep = deps.get(JkModuleId.of("hibernate:hjmlm"));
        Assert.assertEquals(run2run, hibernateDep.scopeMapping());
        final JkScopedDependency llDep = deps.get(JkModuleId.of("hhhhh:ll"));
        Assert.assertEquals(run2run, llDep.scopeMapping());
    }

    @Test
    public void testEquals() {
        final JkScopeMapping run2runA = JkScopeMapping.of(RUNTIME).to(COMPILE);
        final JkScopeMapping run2runB = JkScopeMapping.of(RUNTIME).to(COMPILE);
        Assert.assertEquals(run2runA, run2runB);
    }

    private JkDependencies secondaryDeps() {
        return JkDependencies.builder()
                .on("454545:5445:54545")
                .on("lkll:llljk:poo").build();
    }



}
