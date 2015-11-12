package org.jerkar.api.depmanagement;

import static org.jerkar.api.depmanagement.JkScopedDependencyTest.COMPILE;
import static org.jerkar.api.depmanagement.JkScopedDependencyTest.RUNTIME;

import java.io.File;

import org.jerkar.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

/**
 * @formatter:off
 * @author djeang
 *
 */
public class JkDependenciesTest {

    @Test
    public void testBuilder() {
	final JkScopeMapping run2run = JkScopeMapping.of(RUNTIME).to(RUNTIME);
	final JkDependencies deps = JkDependencies.builder().usingDefaultScopeMapping(run2run)
		.on("hibernate:hjmlm:1212.0").on("spring:spring:6.3").scope(COMPILE).on(secondaryDeps())
		.on("klklkl:lklk:mlml").on("hhhhh:ll:ppp")
		.on(JkModuleId.of("lmlmlm", "mùmùmù"), JkVersionRange.of("5454")).build();
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

    public void onFile() {
	final File depFile1 = new File("/my/file1.jar");
	final File depFile2 = new File("/my/file2.zip");
	JkDependencies.builder().on(depFile1, depFile2).build();
    }

    public void onModule() {
	JkDependencies.builder().on("myGroup:otherModule:2.2.0").on("myGroup:moduleB:2.3:client").build();
    }

}
