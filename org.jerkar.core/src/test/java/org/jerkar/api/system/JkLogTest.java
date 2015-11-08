package org.jerkar.api.system;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public class JkLogTest {

	@Test
	public void test() {
		JkLog.verbose(true);
		assertTrue(JkLog.verbose());
		JkLog.silent(true);
		assertTrue(JkLog.silent());
	}

	@After
	public void resetJkLog() {
		JkLog.verbose(false);
		JkLog.silent(false);
	}

}
