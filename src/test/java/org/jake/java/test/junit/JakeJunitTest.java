package org.jake.java.test.junit;

import org.junit.Test;

public class JakeJunitTest {

	@Test
	public void testSystemOutRedirect() {

		// Just print something in the console to see if it appears during test execution
		System.out.println("system out : This text should appear during test execution");
		System.err.println("system err : This text should appear during test execution");
	}

}
