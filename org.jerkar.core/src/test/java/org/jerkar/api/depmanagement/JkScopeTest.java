package org.jerkar.api.depmanagement;

import static org.jerkar.api.depmanagement.JkScopedDependencyTest.COMPILE;
import static org.jerkar.api.depmanagement.JkScopedDependencyTest.RUNTIME;
import static org.jerkar.api.depmanagement.JkScopedDependencyTest.TEST;

import org.jerkar.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkScopeTest {

    @Test
    public void testIsInOrIsInheritedByAnyOf() {
        Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(RUNTIME)));
        Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(RUNTIME,
                JkScope.of("aScope"))));
        Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(TEST)));
        Assert.assertTrue(COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(COMPILE)));
        Assert.assertTrue(RUNTIME.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(COMPILE)));
        Assert.assertTrue(TEST.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(COMPILE)));
        Assert.assertTrue(!RUNTIME.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScope
                .of("anotherScope"))));
    }

}
