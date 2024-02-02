package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

public class ParsedCmdLineTest {

    @Test
    public void testToDependency() {
        Assert.assertEquals(JkCoordinateDependency.class,
                ParsedCmdLine.toDependency(Paths.get(""), "commons-lang:commons-lang:2.63").getClass());
        Assert.assertEquals(JkCoordinateDependency.class,
                ParsedCmdLine.toDependency(Paths.get(""), "dev.jeka:a-jeka-module").getClass());
    }
}
