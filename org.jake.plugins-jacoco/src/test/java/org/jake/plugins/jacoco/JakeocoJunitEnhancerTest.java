package org.jake.plugins.jacoco;

import java.io.File;

import org.jake.JakeClasspath;
import org.jake.java.testing.junit.JakeUnit;
import org.junit.Test;

public class JakeocoJunitEnhancerTest {

	@Test
	public void test() {
		JakeocoJunitEnhancer.of(new File("."), new File("."))
		.enhance(JakeUnit.of(JakeClasspath.of()));
	}

}
