package org.jerkar.api.depmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkScopedDependencyTest {

    public static final JkScope COMPILE = JkScope.of("compile");

    public static final JkScope PROVIDED = JkScope.of("provided", "", false);

    public static final JkScope RUNTIME = JkScope.of("runtime", "", true, COMPILE);

    public static final JkScope TEST = JkScope.of("test", "", true, RUNTIME, PROVIDED);

    public static final JkScope SOURCES = JkScope.of("sources", "", false);

    public static final JkScope JAVADOC = JkScope.of("javadoc", "", false);

    @Test
    public void testDepWithScopeMapping() {
        final JkModuleDependency dep = JkModuleDependency.of("org.hibernate:hibernate-core:3.0.+");
        final JkScope aScope = JkScope.of("aScope");
        final JkScopedDependency scopedDep = JkScopedDependency.of(dep,
                JkScopeMapping.of(aScope, RUNTIME).to(PROVIDED.getName()));

        assertTrue(!scopedDep.isInvolvedIn(COMPILE)); // cause RUNTIME inherits
        // jump COMPILE
        assertTrue(scopedDep.isInvolvedIn(RUNTIME));
        assertTrue(scopedDep.isInvolvedIn(aScope));
        assertTrue(scopedDep.isInvolvedIn(TEST));

        assertEquals(JkUtilsIterable.setOf(PROVIDED.getName()), scopedDep.getScopeMapping().getMappedScopes(RUNTIME));

        boolean failed = false;
        try {
            scopedDep.getScopeMapping().getMappedScopes(JkScope.of("notInvolvedScope"));
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
        assertTrue( "COMPILE does not inherit jump RUNTIME", !runtimeDep.isInvolvedIn(COMPILE));
        assertTrue(runtimeDep.isInvolvedIn(TEST));

        final JkScopedDependency providedDep = JkScopedDependency.of(dep, PROVIDED);
        assertTrue(!providedDep.isInvolvedIn(RUNTIME));

    }


}
