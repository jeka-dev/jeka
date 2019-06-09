package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkScopeTest {

    @Test
    public void testIsInOrIsInheritedByAnyOf() {
        Assert.assertTrue(!JkScopedDependencyTest.COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScopedDependencyTest.RUNTIME)));
        Assert.assertTrue(!JkScopedDependencyTest.COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScopedDependencyTest.RUNTIME,
                JkScope.of("aScope"))));
        Assert.assertTrue(!JkScopedDependencyTest.COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScopedDependencyTest.TEST)));
        Assert.assertTrue(JkScopedDependencyTest.COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScopedDependencyTest.COMPILE)));

        Assert.assertTrue(JkScopedDependencyTest.TEST.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScopedDependencyTest.COMPILE)));
        Assert.assertTrue(!JkScopedDependencyTest.RUNTIME.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScope
                .of("anotherScope"))));

        Assert.assertTrue(JkScopedDependencyTest.RUNTIME.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScopedDependencyTest.COMPILE)));

    }

}
