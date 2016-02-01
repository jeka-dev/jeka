package org.jerkar.tool.builtins.templates.javabuild.test.junit;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkJunitTest {

    @Test
    public void testSystemOutRedirect() {

        // Just print something in the console to see if it appears during test
        // execution
        System.out.println("system out : This text should appear during test execution");
        System.err.println("system err : This text should appear during test execution");
    }

}
