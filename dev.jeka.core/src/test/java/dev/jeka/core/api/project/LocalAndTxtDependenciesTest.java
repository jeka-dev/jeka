package dev.jeka.core.api.project;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class LocalAndTxtDependenciesTest {

    @Test
    public void testFromDescription()  {
        URL url = LocalAndTxtDependenciesTest.class.getResource("dependencies.txt");
        LocalAndTxtDependencies commonDeps = LocalAndTxtDependencies.ofTextDescription(url);
        assertEquals(3, commonDeps.getCompile().getEntries().size());
        assertEquals(5, commonDeps.getRuntime().getEntries().size());
        assertEquals(10, commonDeps.getTest().getEntries().size());

        assertEquals(1, commonDeps.getCompile().getVersionProvider().getBoms().size());
    }

}