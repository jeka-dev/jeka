package org.jake.depmanagement;

import static org.jake.java.build.JakeBuildJava.COMPILE;
import static org.jake.java.build.JakeBuildJava.RUNTIME;
import static org.jake.java.build.JakeBuildJava.TEST;

import org.jake.utils.JakeUtilsIterable;
import org.junit.Assert;
import org.junit.Test;

public class JakeScopeTest {

	@Test
	public void testIsInOrIsInheritedByAnyOf() {
		Assert.assertTrue(COMPILE.isInOrIsInheritedByAnyOf(JakeUtilsIterable.setOf(RUNTIME)));
		Assert.assertTrue(COMPILE.isInOrIsInheritedByAnyOf(JakeUtilsIterable.setOf(RUNTIME, JakeScope.of("aScope"))));
		Assert.assertTrue(COMPILE.isInOrIsInheritedByAnyOf(JakeUtilsIterable.setOf(TEST)));
		Assert.assertTrue(COMPILE.isInOrIsInheritedByAnyOf(JakeUtilsIterable.setOf(COMPILE)));
		Assert.assertTrue( !RUNTIME.isInOrIsInheritedByAnyOf(JakeUtilsIterable.setOf(JakeScope.of("anotherScope"))));
	}

}
