package org.jerkar.api.depmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkScopedDependencyTest {

    public static final JkScope COMPILE = JkScope.of("compile");

    public static final JkScope PROVIDED = JkScope.build("provided").transitive(false).build();

    public static final JkScope RUNTIME = JkScope.build("runtime").extending(COMPILE).build();

    public static final JkScope TEST = JkScope.build("test").extending(RUNTIME, PROVIDED).build();

    public static final JkScope SOURCES = JkScope.build("sources").transitive(false).build();

    public static final JkScope JAVADOC = JkScope.build("javadoc").transitive(false).build();

    @Test
    public void testDepWithScopeMapping() {
        final JkModuleDependency dep = JkModuleDependency.of("org.hibernate:hibernate-core:3.0.+");
        final JkScope aScope = JkScope.of("aScope");
        final JkScopedDependency scopedDep = JkScopedDependency.of(dep,
                JkScopeMapping.of(aScope, RUNTIME).to(PROVIDED));

        assertTrue(!scopedDep.isInvolvedIn(COMPILE)); // cause RUNTIME inherits
        // from COMPILE
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
    public void testIsInvolvedIn() {
        final JkModuleDependency dep = JkModuleDependency.of("org.hibernate:hibernate-core:3.0.+");
        final JkScopedDependency runtimeDep = JkScopedDependency.of(dep, RUNTIME);

        assertTrue(runtimeDep.isInvolvedIn(RUNTIME));
        assertTrue( "COMPILE does not inherit from RUNTIME", !runtimeDep.isInvolvedIn(COMPILE));
        assertTrue(runtimeDep.isInvolvedIn(TEST));

        final JkScopedDependency providedDep = JkScopedDependency.of(dep, PROVIDED);
        assertTrue(!providedDep.isInvolvedIn(RUNTIME));

    }


}
