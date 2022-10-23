package dev.jeka.core.api.project;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class LocalAndTxtDependenciesTest {

    @Test
    public void testFromDescription()  {
        URL url = LocalAndTxtDependenciesTest.class.getResource("dependencies.txt");
        LocalAndTxtDependencies commonDeps = LocalAndTxtDependencies.ofTextDescription(url);
        assertEquals(2, commonDeps.getRegular().getEntries().size());
        assertEquals(1, commonDeps.getCompileOnly().getEntries().size());
        assertEquals(3, commonDeps.getRuntimeOnly().getEntries().size());
        assertEquals(4, commonDeps.getTest().getEntries().size());

        assertEquals(1, commonDeps.getRegular().getVersionProvider().getBoms().size());
    }

}