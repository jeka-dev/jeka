package org.jerkar.depmanagement;

import org.jerkar.depmanagement.JkDependency;
import org.jerkar.depmanagement.JkExternalModule;
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
