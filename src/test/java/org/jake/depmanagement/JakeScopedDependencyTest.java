package org.jake.depmanagement;

import static org.jake.depmanagement.JakeScope.COMPILE;
import static org.jake.depmanagement.JakeScope.PROVIDED;
import static org.jake.depmanagement.JakeScope.RUNTIME;

import java.util.HashSet;
import java.util.Set;

import org.jake.depmanagement.JakeScope.JakeScopeMapping;
import org.junit.Assert;
import org.junit.Test;

public class JakeScopedDependencyTest {


	@Test
	public void testScope() {
		final JakeExternalModule dep = JakeDependency.of("org.hibernate:hibernate-core:3.0.+");
		final JakeScopedDependency scopedDep = dep.scope(JakeScopeMapping.from(COMPILE, RUNTIME).to(PROVIDED));
		Assert.assertEquals("org.hibernate", dep.moduleId().group());
		Assert.assertEquals("hibernate-core", dep.moduleId().name());
		final JakeScopeMapping mapping = scopedDep.scope();
		final Set<JakeScope> sampleSet = new HashSet<JakeScope>();
		sampleSet.add(PROVIDED);
		Assert.assertEquals(sampleSet, mapping.targetScopes(COMPILE));

	}

}
