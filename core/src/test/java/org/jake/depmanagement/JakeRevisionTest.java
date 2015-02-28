package org.jake.depmanagement;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JakeRevisionTest {

	@Test
	public void test() {
		assertTrue(JakeVersion.named("1.0.1").isGreaterThan(JakeVersion.named("1.0.0")));
	}

}
