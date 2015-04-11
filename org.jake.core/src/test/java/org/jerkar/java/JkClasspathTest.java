package org.jerkar.java;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import org.jerkar.JkClasspath;
import org.junit.Test;

public class JkClasspathTest {

	@Test
	public void testEntriesContainingPath() throws Exception {
		final URL sampleJarUrl = JkClasspathTest.class.getResource("jarWithTwoClassesIn.jar");
		final File sampleJar = new File(sampleJarUrl.toURI().getPath());
		final JkClasspath classpath = JkClasspath.of(sampleJar);
		assertEquals(sampleJar, classpath.getEntryContainingClass("org.jake.JakeBuildBase"));
		assertEquals(sampleJar, classpath.getEntryContainingClass("org.jake.JakeBuildBase$ActionDescription"));
		assertEquals(null, classpath.getEntryContainingClass("no.existing.MyClass"));
	}

}
