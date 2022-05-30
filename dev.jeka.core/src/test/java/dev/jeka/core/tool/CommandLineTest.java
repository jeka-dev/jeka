package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkModuleDependency;
import org.junit.Assert;
import org.junit.Test;

public class CommandLineTest {

    @Test
    public void testToDependency() {
        Assert.assertEquals(JkModuleDependency.class,
                CommandLine.toDependency("commons-lang:commons-lang:2.63").getClass());
        Assert.assertEquals(JkModuleDependency.class,
                CommandLine.toDependency("dev.jeka:a-jeka-module").getClass());
    }
}
