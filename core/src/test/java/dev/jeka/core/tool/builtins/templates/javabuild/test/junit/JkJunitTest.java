package dev.jeka.core.tool.builtins.templates.javabuild.test.junit;

import org.junit.jupiter.api.Test;

class JkJunitTest {

    @Test
    void testSystemOutRedirect() {

        // Just print something in the console to see if it appears during test
        // execution
        System.out.println("ofSystem out : This text should appear during test execution");
        System.err.println("ofSystem err : This text should appear during test execution");
    }

}
