package org.jake.depmanagement;

import org.junit.Test;

public class JakeExternalModuleTest {

	@SuppressWarnings("unused")
	@Test
	public void testOf() {
		JakeDependency dep;
		dep = JakeExternalModule.of(JakeModuleId.of("org.hibernate", "hibernate-core"), JakeVersionRange.of("3.0.1.Final"));
		dep = JakeExternalModule.of("org.hibernate", "hibernate-core", "3.0.1.Final");
		dep = JakeExternalModule.of("org.hibernate:hibernate-core:3.0.1+");
	}



}
