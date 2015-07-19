package org.jerkar.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

public class JkDependencyTest {


	@Test
	public void test() {
		final JkModuleDependency dep = JkModuleDependency.of("org.hibernate:hibernate-core:3.0.+");
		Assert.assertEquals("org.hibernate", dep.moduleId().group());
		Assert.assertEquals("hibernate-core", dep.moduleId().name());
	}

}
