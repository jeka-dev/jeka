package dev.jeka.core.tool;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CmdLineArgsTest {

    @Test
    public void splitByKbeanContext_ok() {
        List<CmdLineArgs> split = new CmdLineArgs("boo", "bar=2",  "project:", "version=1.0").splitByKbeanContext();
        assertEquals(2, split.size());
        assertEquals(2, split.get(0).get().length);

        assertEquals("boo", split.get(0).get()[0]);
        assertEquals("bar=2", split.get(0).get()[1]);

        assertEquals("project:", split.get(1).get()[0]);
        assertEquals("version=1.0", split.get(1).get()[1]);
    }

    @Test
    public void filterShellArgsTest() {

        assertEquals(0, filtered("--remote", "/opt/dir").size());

        assertEquals(1, filtered("--remote", "/opt/dir", "helloWorld").size());
        assertEquals("helloWorld", filtered("--remote", "/opt/dir", "helloWorld").get(0));

        // --update is before --remote
        assertEquals(2, filtered("--update", "--remote", "/opt/dir", "helloWorld").size());
        assertEquals("--update", filtered("--update", "--remote", "/opt/dir", "helloWorld").get(0));
        assertEquals("helloWorld", filtered("--update", "--remote", "/opt/dir", "helloWorld").get(1));

        // --update is after --remote
        assertEquals(2, filtered("--remote", "--update", "/opt/dir", "helloWorld").size());
        assertEquals("-u", filtered( "--remote", "-u", "/opt/dir", "helloWorld").get(0));
        assertEquals("helloWorld", filtered("--remote", "--update", "/opt/dir", "helloWorld").get(1));

    }

    private static List<String> filtered(String... args) {
        return Arrays.asList(CmdLineArgs.filterShellArgs(args));
    }
}