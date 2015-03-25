package org.jake.depmanagement;

import static org.jake.java.build.JakeJavaBuild.COMPILE;
import static org.jake.java.build.JakeJavaBuild.RUNTIME;
import static org.jake.java.build.JakeJavaBuild.TEST;

import org.jake.utils.JakeUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

public class JakeScopeTest {

	@Test
	public void testIsInOrIsInheritedByAnyOf() {
		Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JakeUtilsIterable.setOf(RUNTIME)));
		Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JakeUtilsIterable.setOf(RUNTIME, JakeScope.of("aScope"))));
		Assert.assertTrue(!COMPILE.isInOrIsExtendingAnyOf(JakeUtilsIterable.setOf(TEST)));
		Assert.assertTrue(COMPILE.isInOrIsExtendingAnyOf(JakeUtilsIterable.setOf(COMPILE)));
		Assert.assertTrue(RUNTIME.isInOrIsExtendingAnyOf(JakeUtilsIterable.setOf(COMPILE)));
		Assert.assertTrue(TEST.isInOrIsExtendingAnyOf(JakeUtilsIterable.setOf(COMPILE)));
		Assert.assertTrue( !RUNTIME.isInOrIsExtendingAnyOf(JakeUtilsIterable.setOf(JakeScope.of("anotherScope"))));
	}

}
