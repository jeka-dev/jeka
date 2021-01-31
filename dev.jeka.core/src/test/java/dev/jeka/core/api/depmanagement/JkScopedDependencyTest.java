package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("javadoc")
public class JkScopedDependencyTest {

    public static final JkScope COMPILE = JkScope.of("compile");

    public static final JkScope PROVIDED = JkScope.of("provided", "", false);

    public static final JkScope RUNTIME = JkScope.of("runtime", "", true);

    public static final JkScope TEST = JkScope.of("test", "", true);

    public static final JkScope SOURCES = JkScope.of("sources", "", false);

    public static final JkScope JAVADOC = JkScope.of("javadoc", "", false);

    @Test
    public void testDepWithScopeMapping() {
        final JkModuleDependency dep = JkModuleDependency.of("org.hibernate:hibernate-core:3.0.+");
        final JkScope aScope = JkScope.of("aScope");
        final JkScopedDependency scopedDep = JkScopedDependency.of(dep,
                JkScopeMapping.of(aScope, RUNTIME).to(PROVIDED.getName()));


        assertEquals(JkUtilsIterable.setOf(PROVIDED.getName()), scopedDep.getScopeMapping().getMappedScopes(RUNTIME));

        boolean failed = false;
        try {
            scopedDep.getScopeMapping().getMappedScopes(JkScope.of("notInvolvedScope"));
        } catch (final IllegalArgumentException e) {
            failed = true;
        }
        assertTrue(failed);
    }

}
