package org.jerkar.java.eclipse;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jerkar.JkDirSet;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.java.build.JkJavaBuild;
import org.junit.Test;

public class DotClasspathTest {

	private static final String SAMPLE_NAME = "classpath";

	@Test
	public void testFromFile() throws URISyntaxException {
		sample();
	}

	@Test
	public void testSourceDirs() throws URISyntaxException {
		final JkDirSet dirSet = sample().sourceDirs(structure(), Sources.ALL_PROD).prodSources;
		assertEquals(2, dirSet.jkDirs().size());
	}

	@Test
	public void testLibs() throws URISyntaxException {
		final List<Lib> libs = sample().libs(structure(), new ScopeResolverSmart(null));
		assertEquals(5, libs.size());
	}

	@Test
	public void testToDependencies() throws URISyntaxException {
		final List<Lib> libs = sample().libs( structure(), new ScopeResolverSmart(null));
		assertEquals(5, libs.size());

		final JkDependencies deps = Lib.toDependencies(null, libs, new ScopeResolverSmart(null));


		assertEquals(0, deps.dependenciesDeclaredWith(JkJavaBuild.TEST).size());
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
