package org.jake.java.test;

import org.junit.Test;

public class JakeJunitTest {

	@Test
	public void testSystemOutRedirect() {

		// Just print something in the console to see if it appears during test execution
		System.out.println("system out : ------------------------------------------------------");
		System.err.println("system err : ------------------------------------------------------");
	}

}
