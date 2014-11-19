package org.jake.depmanagement;

import org.junit.Assert;
import org.junit.Test;

public class JakeDependencyTest {


	@Test
	public void test() {
		final JakeExternalModule dep = JakeDependency.of("org.hibernate:hibernate-core:3.0.+");
		Assert.assertEquals("org.hibernate", dep.moduleId().group());
		Assert.assertEquals("hibernate-core", dep.moduleId().name());
	}

}
