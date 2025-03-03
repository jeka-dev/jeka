package dev.jeka.core.api.project;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class LocalAndTxtDependenciesTest {

    @Test
    public void testFromDescription()  {
        URL url = LocalAndTxtDependenciesTest.class.getResource("dependencies.txt");
        LocalAndTxtDependencies commonDeps = LocalAndTxtDependencies.ofTextDescription(url);
        assertEquals(4, commonDeps.getCompile().getEntries().size());
        assertEquals(6, commonDeps.getRuntime().getEntries().size());
        assertEquals(11, commonDeps.getTest().getEntries().size());

        assertEquals(0, commonDeps.getCompile().getVersionProvider().getBoms().size());
    }

}