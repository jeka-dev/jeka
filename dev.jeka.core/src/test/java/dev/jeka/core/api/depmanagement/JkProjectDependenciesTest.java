package dev.jeka.core.api.depmanagement;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class JkProjectDependenciesTest {

    @Test
    public void testFromDescription()  {
        URL url = JkProjectDependenciesTest.class.getResource("dependencies.txt");
        JkProjectDependencies commonDeps = JkProjectDependencies.ofTextDescription(url);
        assertEquals(3, commonDeps.getCompileDeps().getEntries().size());
        assertEquals(5, commonDeps.getRuntimeDeps().getEntries().size());
        assertEquals(4, commonDeps.getTestDeps().getEntries().size());
    }

}