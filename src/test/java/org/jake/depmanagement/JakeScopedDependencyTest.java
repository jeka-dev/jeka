package org.jake.depmanagement;

import static org.jake.java.build.JakeBuildJava.COMPILE;
import static org.jake.java.build.JakeBuildJava.PROVIDED;
import static org.jake.java.build.JakeBuildJava.RUNTIME;
import static org.jake.java.build.JakeBuildJava.TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.jake.java.build.JakeBuildJava;
import org.jake.utils.JakeUtilsIterable;
import org.junit.Test;

public class JakeScopedDependencyTest {


	@Test
	public void testDepWithScopeMapping() {
		final JakeExternalModule dep = JakeDependency.of("org.hibernate:hibernate-core:3.0.+");
		final JakeScope aScope = JakeScope.of("aScope");
		final JakeScopedDependency scopedDep = JakeScopedDependency.of(dep,
				JakeScopeMapping.from(aScope, RUNTIME).to(PROVIDED));

		assertTrue(scopedDep.isInvolving(COMPILE)); // cause RUNTIME inherits from COMPILE
		assertTrue(scopedDep.isInvolving(RUNTIME));
		assertTrue(scopedDep.isInvolving(aScope));
		assertTrue(!scopedDep.isInvolving(TEST));

		final Set<JakeScope> sampleSet = new HashSet<JakeScope>();
		sampleSet.add(JakeBuildJava.PROVIDED);
		assertEquals(sampleSet, scopedDep.scopeMapping().mappedScopes(RUNTIME));
		assertEquals(sampleSet, scopedDep.scopeMapping().mappedScopes(COMPILE)); // cause RUNTIME inherits from COMPILE

		boolean failed = false;
		try {
			scopedDep.scopeMapping().mappedScopes(JakeScope.of("notInvolvedScope"));
		} catch (final IllegalArgumentException e) {
			failed = true;
		}
		assertTrue(failed);


	}

	@Test
	public void testWithScope() {
		final JakeExternalModule dep = JakeDependency.of("org.hibernate:hibernate-core:3.0.+");
		final JakeScopedDependency scopedDep = JakeScopedDependency.of(dep, RUNTIME);

		assertTrue(scopedDep.isInvolving(RUNTIME));
		assertTrue(scopedDep.isInvolving(COMPILE)); // cause RUNTIME inherits from COMPILE
		assertTrue(!scopedDep.isInvolving(TEST));

		assertEquals(JakeUtilsIterable.setOf(COMPILE), scopedDep.scopeMapping().mappedScopes(COMPILE));
		assertEquals(JakeUtilsIterable.setOf(RUNTIME), scopedDep.scopeMapping().mappedScopes(RUNTIME));

		boolean failed = false;
		try {
			scopedDep.scopeMapping().mappedScopes(TEST);
		} catch (final IllegalArgumentException e) {
			failed = true;
		}
		assertTrue(failed);

	}

}
