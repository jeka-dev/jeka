package org.jake.depmanagement;

import static org.jake.java.build.JakeBuildJava.COMPILE;
import static org.jake.java.build.JakeBuildJava.RUNTIME;

import org.jake.utils.JakeUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

public class DependenciesTest {

	@Test
	public void test() {
		final JakeDependencies deps = JakeDependencies.builder()
				.defaultScope(JakeScopeMapping.of(RUNTIME, RUNTIME))
				.on("hibernate:hjmlm:1212.0")
				.on("spring:spring:6.3").scope(COMPILE)
				.on(secondaryDeps())
				.on("klklkl:lklk:mlml")
				.on("hhhhh:ll:ppp")
				.on(JakeModuleId.of("lmlmlm:mùmùmù"), JakeVersionRange.of("5454")).build();
		final JakeScopedDependency springDependency = deps.get(JakeModuleId.of("spring:spring"));
		Assert.assertEquals(JakeUtilsIterable.setOf(COMPILE), springDependency.scopes());

	}

	private JakeDependencies secondaryDeps() {
		return JakeDependencies.builder()
				.on("454545:5445:54545")
				.on("lkll:llljk:poo").build();
	}



}
