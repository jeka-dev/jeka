package dev.jeka.core.api.project;

import dev.jeka.core.api.utils.JkUtilsIO;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class LocalAndTxtDependenciesTest {

    @Test
    void testFromDescription()  {
        URL url = LocalAndTxtDependenciesTest.class.getResource(JkDependenciesTxt.PROJECT_DEPENDENCIES_FILE);
        LocalAndTxtDependencies commonDeps = LocalAndTxtDependencies.ofTextDescription(url, null, null, new HashMap<>());
        assertEquals(4, commonDeps.getCompile().getEntries().size());
        assertEquals(6, commonDeps.getRuntime().getEntries().size());
        assertEquals(11, commonDeps.getTest().getEntries().size());

        assertEquals(0, commonDeps.getCompile().getVersionProvider().getBoms().size());
    }

    @Test
    void legacyFormat_isDetected() {
        URL url = LocalAndTxtDependenciesTest.class.getResource(JkDependenciesTxt.PROJECT_DEPENDENCIES_FILE);
        String content = JkUtilsIO.read(url);
        assertTrue(LocalAndTxtDependencies.isLegacyFormat(content));
    }

    @Test
    void newFormat_isDetected() {
        URL url = LocalAndTxtDependenciesTest.class.getResource("jeka.project.deps-ini");
        String content = JkUtilsIO.read(url);
        assertFalse(LocalAndTxtDependencies.isLegacyFormat(content));
    }

}