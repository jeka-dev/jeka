package org.jake.java.eclipse;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jake.JakeDirSet;
import org.jake.java.JakeJavaDependencyResolver;
import org.junit.Test;

public class DotClasspathTest {

	private static final String SAMPLE_NAME = "classpath";

	@Test
	public void testFromFile() throws URISyntaxException {
		sample();
	}

	@Test
	public void testSourceDirs() throws URISyntaxException {
		final JakeDirSet dirSet = sample().sourceDirs(structure(), Sources.ALL_PROD).prodSources;
		assertEquals(2, dirSet.listJakeDirs().size());
	}

	@Test
	public void testLibs() throws URISyntaxException {
		final List<Lib> libs = sample().libs(new File(structure(),"containers"), structure(), Lib.SMART_LIB);
		assertEquals(6, libs.size());
	}

	@Test
	public void testToDependencyResolver() throws URISyntaxException {
		final List<Lib> libs = sample().libs(new File(structure(),"containers"), structure(), Lib.SMART_LIB);
		final JakeJavaDependencyResolver resolver = Lib.toDependencyResolver(libs);
		assertEquals(6, libs.size());
		System.out.println(resolver.testScope());
		assertEquals(6, resolver.testScope().entries().size());
	}

	private DotClasspath sample() throws URISyntaxException {
		final URL sampleFileUrl = DotClasspathTest.class.getResource("samplestructure/" + SAMPLE_NAME);
		final File sampleFile = new File(sampleFileUrl.toURI().getPath());
		return DotClasspath.from(sampleFile);
	}

	private File structure() throws URISyntaxException {
		final URL sampleFileUrl = DotClasspathTest.class.getResource("samplestructure/" + SAMPLE_NAME);
		return new File(sampleFileUrl.toURI().getPath()).getParentFile();
	}

}
