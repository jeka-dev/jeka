package dev.jeka.core.tool.builtins.java;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class CommonDependenciesTest {

    @Test
    public void testFromDescription()  {
        URL url = CommonDependenciesTest.class.getResource("dependencies.txt");
        CommonDependencies commonDeps = CommonDependencies.ofTextDescription(url);
        assertEquals(3, commonDeps.getCompile().getDependencies().size());
        assertEquals(5, commonDeps.getRuntime().getDependencies().size());
        assertEquals(4, commonDeps.getTest().getDependencies().size());
    }

}