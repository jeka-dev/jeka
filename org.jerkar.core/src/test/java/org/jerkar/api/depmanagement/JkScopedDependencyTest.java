package org.jerkar.api.depmanagement;

import static org.jerkar.tool.builtins.templates.javabuild.JkJavaBuild.COMPILE;
import static org.jerkar.tool.builtins.templates.javabuild.JkJavaBuild.PROVIDED;
import static org.jerkar.tool.builtins.templates.javabuild.JkJavaBuild.RUNTIME;
import static org.jerkar.tool.builtins.templates.javabuild.JkJavaBuild.TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependency;
import org.jerkar.api.depmanagement.JkExternalModule;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.tool.builtins.templates.javabuild.JkJavaBuild;
import org.junit.Test;

public class JkScopedDependencyTest {


	@Test
	public void testDepWithScopeMapping() {
		final JkExternalModule dep = JkDependency.of("org.hibernate:hibernate-core:3.0.+");
		final JkScope aScope = JkScope.of("aScope");
		final JkScopedDependency scopedDep = JkScopedDependency.of(dep,
				JkScopeMapping.of(aScope, RUNTIME).to(PROVIDED));

		assertTrue(!scopedDep.isInvolvedIn(COMPILE)); // cause RUNTIME inherits from COMPILE
		assertTrue(scopedDep.isInvolvedIn(RUNTIME));
		assertTrue(scopedDep.isInvolvedIn(aScope));
		assertTrue(scopedDep.isInvolvedIn(TEST));

		final Set<JkScope> sampleSet = new HashSet<JkScope>();
		sampleSet.add(JkJavaBuild.PROVIDED);
		assertEquals(sampleSet, scopedDep.scopeMapping().mappedScopes(RUNTIME));

		boolean failed = false;
		try {
			scopedDep.scopeMapping().mappedScopes(JkScope.of("notInvolvedScope"));
		} catch (final IllegalArgumentException e) {
			failed = true;
		}
		assertTrue(failed);
	}

	@Test
	public void testWithScope() {
		final JkExternalModule dep = JkDependency.of("org.hibernate:hibernate-core:3.0.+");
		final JkScopedDependency scopedDep = JkScopedDependency.of(dep, RUNTIME);

		assertTrue(scopedDep.isInvolvedIn(RUNTIME));
		assertTrue(!scopedDep.isInvolvedIn(COMPILE)); // cause RUNTIME inherits from COMPILE
		assertTrue(scopedDep.isInvolvedIn(TEST));

	}

}
