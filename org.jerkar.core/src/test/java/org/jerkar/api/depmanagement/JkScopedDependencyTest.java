package org.jerkar.api.depmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class JkScopedDependencyTest {

	public static final JkScope PROVIDED = JkScope.of("provided").transitive(false);

	public static final JkScope COMPILE = JkScope.of("compile");

	public static final JkScope RUNTIME = JkScope.of("runtime").extending(COMPILE);

	public static final JkScope TEST = JkScope.of("test").extending(RUNTIME, PROVIDED);

	public static final JkScope SOURCES = JkScope.of("sources").transitive(false);

	public static final JkScope JAVADOC = JkScope.of("javadoc").transitive(false);


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
		sampleSet.add(PROVIDED);
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
