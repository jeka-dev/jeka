package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import org.junit.Assert;
import org.junit.Test;

public class CommandLineTest {

    @Test
    public void testToDependency() {
        Assert.assertEquals(JkCoordinateDependency.class,
                CommandLine.toDependency("commons-lang:commons-lang:2.63").getClass());
        Assert.assertEquals(JkCoordinateDependency.class,
                CommandLine.toDependency("dev.jeka:a-jeka-module").getClass());
    }
}
