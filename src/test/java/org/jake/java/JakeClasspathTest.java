package org.jake.java;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import org.junit.Test;

public class JakeClasspathTest {

	@Test
	public void testEntriesContainingPath() throws Exception {
		final URL sampleJarUrl = JakeClasspathTest.class.getResource("jarWithTwoClassesIn.jar");
		final File sampleJar = new File(sampleJarUrl.toURI().getPath());
		final JakeClasspath classpath = JakeClasspath.of(sampleJar);
		assertEquals(sampleJar, classpath.getEntryContainingClass("org.jake.JakeBuildBase"));
		assertEquals(sampleJar, classpath.getEntryContainingClass("org.jake.JakeBuildBase$ActionDescription"));
		assertEquals(null, classpath.getEntryContainingClass("no.existing.MyClass"));
	}

}
