package dev.jeka.core.tool;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CmdLineArgsTest {

    @Test
    public void splitByKbeanContext() {
        List<CmdLineArgs> split = new CmdLineArgs("boo", "bar=2",  "project:", "version=1.0").splitByKbeanContext();
        assertEquals(2, split.size());
        assertEquals(2, split.get(0).get().length);

        assertEquals("boo", split.get(0).get()[0]);
        assertEquals("bar=2", split.get(0).get()[1]);

        assertEquals("project:", split.get(1).get()[0]);
        assertEquals("version=1.0", split.get(1).get()[1]);
    }
}