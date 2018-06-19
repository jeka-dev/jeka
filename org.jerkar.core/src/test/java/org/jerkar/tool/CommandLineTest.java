package org.jerkar.tool;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class CommandLineTest {

    @Test
    public void test() {
        final CommandLine tested = args("base* verify sonar#done jacoco#* -foo=1 -bar=2* -jacoco#foo=4*");

        // Test methods
        assertEquals(3, tested.getMasterMethods().size());
        assertEquals(1, tested.getSubProjectMethods().size());
        assertEquals("base", tested.getMasterMethods().get(0).methodName);
        assertEquals(false, tested.getMasterMethods().get(0).isMethodPlugin());
        assertEquals(true, tested.getMasterMethods().get(2).isMethodPlugin());
        assertEquals("done", tested.getMasterMethods().get(2).methodName);
        assertEquals("sonar", tested.getMasterMethods().get(2).pluginName);

    }

    private static CommandLine args(String string) {
        return CommandLine.parse(string.split(" "));
    }

}
