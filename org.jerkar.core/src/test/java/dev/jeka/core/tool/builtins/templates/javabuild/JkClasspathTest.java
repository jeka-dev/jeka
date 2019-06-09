package dev.jeka.core.tool.builtins.templates.javabuild;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.java.JkClasspath;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkClasspathTest {

    @Test
    public void testEntriesContainingPath() throws Exception {
        final URL sampleJarUrl = JkClasspathTest.class.getResource("jarWithTwoClassesIn.jar");
        final Path sampleJar = Paths.get(sampleJarUrl.toURI());
        final JkClasspath classpath = JkClasspath.of(sampleJar);
        assertEquals(sampleJar, classpath.getEntryContainingClass("org.jake.JakeBuildBase"));
        assertEquals(sampleJar,
                classpath.getEntryContainingClass("org.jake.JakeBuildBase$ActionDescription"));
        assertEquals(null, classpath.getEntryContainingClass("no.existing.MyClass"));
    }

}
