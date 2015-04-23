package org.jerkar.plugins.jacoco;

import java.io.File;

import org.jerkar.JkClasspath;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit;
import org.jerkar.plugins.jacoco.JkocoJunitEnhancer;
import org.junit.Test;

public class JkocoJunitEnhancerTest {

	@Test
	public void test() {
		JkocoJunitEnhancer.of(new File("."), new File("."))
		.enhance(JkUnit.of(JkClasspath.of()));
	}

}
