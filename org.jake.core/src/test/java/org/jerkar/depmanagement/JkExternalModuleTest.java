package org.jerkar.depmanagement;

import org.jerkar.depmanagement.JkDependency;
import org.jerkar.depmanagement.JkExternalModule;
import org.jerkar.depmanagement.JkModuleId;
import org.jerkar.depmanagement.JkVersionRange;
import org.junit.Test;

public class JkExternalModuleTest {

	@SuppressWarnings("unused")
	@Test
	public void testOf() {
		JkDependency dep;
		dep = JkExternalModule.of(JkModuleId.of("org.hibernate", "hibernate-core"), JkVersionRange.of("3.0.1.Final"));
		dep = JkExternalModule.of("org.hibernate", "hibernate-core", "3.0.1.Final");
		dep = JkExternalModule.of("org.hibernate:hibernate-core:3.0.1+");
	}



}
