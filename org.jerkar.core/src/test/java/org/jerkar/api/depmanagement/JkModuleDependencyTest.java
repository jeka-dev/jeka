package org.jerkar.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

public class JkModuleDependencyTest {

	@Test
	public void testOf() {
		final JkModuleDependency dep = JkModuleDependency
				.of("group:name:version:sources")
				.transitive(true)
				.ext("zip");
		Assert.assertEquals("sources", dep.classifier());

		final JkModuleDependency dep2 = JkModuleDependency
				.of("group:name:version:sources@zip");
		Assert.assertEquals("zip", dep2.extension());
		Assert.assertFalse(dep2.transitive());

	}

}
