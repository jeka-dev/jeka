package org.jake.depmanagement;

import static org.jake.depmanagement.JakeScope.COMPILE;
import static org.jake.depmanagement.JakeScope.PROVIDED;
import static org.jake.depmanagement.JakeScope.RUNTIME;

import java.util.HashSet;
import java.util.Set;

import org.jake.depmanagement.Dependency.ModuleAndVersionRange;
import org.jake.depmanagement.JakeScope.JakeScopeMapping;
import org.junit.Assert;
import org.junit.Test;

public class DependencyTest {

	@SuppressWarnings("unused")
	@Test
	public void test() {
		Dependency dep;
		dep = Dependency.of(JakeModuleId.of("org.hibernate", "hibernate-core"), JakeVersionRange.of("3.0.1.Final"));
		dep = Dependency.of("org.hibernate", "hibernate-core", "3.0.1.Final");;
		dep = Dependency.of("org.hibernate:hibernate-core:3.0.1.Final");
	}

	@Test
	public void testScope() {
		final ModuleAndVersionRange dep = Dependency.of("org.hibernate:hibernate-core:3.0.+")
				.scope(COMPILE, RUNTIME).mapTo(PROVIDED);
		Assert.assertEquals("org.hibernate", dep.module().group());
		Assert.assertEquals("hibernate-core", dep.module().name());
		final JakeScopeMapping mapping = dep.scopeMapping();
		final Set<JakeScope> set = new HashSet<JakeScope>();
		set.add(PROVIDED);
		Assert.assertEquals(set, mapping.targetScopes(COMPILE));

	}

}
