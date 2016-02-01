package org.jerkar.tool.builtins.templates.javabuild;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import org.jerkar.api.java.JkClasspath;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkClasspathTest {

    @Test
    public void testEntriesContainingPath() throws Exception {
        final URL sampleJarUrl = JkClasspathTest.class.getResource("jarWithTwoClassesIn.jar");
        final File sampleJar = new File(sampleJarUrl.toURI().getPath());
        final JkClasspath classpath = JkClasspath.of(sampleJar);
        assertEquals(sampleJar, classpath.getEntryContainingClass("org.jake.JakeBuildBase"));
        assertEquals(sampleJar,
                classpath.getEntryContainingClass("org.jake.JakeBuildBase$ActionDescription"));
        assertEquals(null, classpath.getEntryContainingClass("no.existing.MyClass"));
    }

}
