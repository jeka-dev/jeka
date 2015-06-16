package org.jerkar.api.depmanagement;

import org.jerkar.api.depmanagement.JkDependency;
import org.jerkar.api.depmanagement.JkExternalModule;
import org.junit.Assert;
import org.junit.Test;

public class JkDependencyTest {


	@Test
	public void test() {
		final JkExternalModule dep = JkDependency.of("org.hibernate:hibernate-core:3.0.+");
		Assert.assertEquals("org.hibernate", dep.moduleId().group());
		Assert.assertEquals("hibernate-core", dep.moduleId().name());
	}

}
