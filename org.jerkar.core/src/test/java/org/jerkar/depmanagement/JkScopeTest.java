package org.jerkar.depmanagement;

import static org.jerkar.java.build.JkJavaBuild.COMPILE;
import static org.jerkar.java.build.JkJavaBuild.RUNTIME;
import static org.jerkar.java.build.JkJavaBuild.TEST;

import org.jerkar.depmanagement.JkScope;
import org.jerkar.utils.JkUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

public class JkScopeTest {

	@Test
	public void testIsInOrIsInheritedByAnyOf() {
		Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(RUNTIME)));
		Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(RUNTIME, JkScope.of("aScope"))));
		Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(TEST)));
		Assert.assertTrue(COMPILE.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(COMPILE)));
		Assert.assertTrue(RUNTIME.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(COMPILE)));
		Assert.assertTrue(TEST.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(COMPILE)));
		Assert.assertTrue( !RUNTIME.isInOrIsExtendingAnyOf(JkUtilsIterable.setOf(JkScope.of("anotherScope"))));
	}

}
