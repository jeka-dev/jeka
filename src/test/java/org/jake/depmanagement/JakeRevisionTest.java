package org.jake.depmanagement;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JakeRevisionTest {

	@Test
	public void test() {
		assertTrue(JakeVersion.of("1.0.1").isGreaterThan(JakeVersion.of("1.0.0")));
	}

}
