package org.jerkar.plugins.jacoco;

import java.io.File;

import org.jerkar.JkBuild;
import org.jerkar.JkDoc;
import org.jerkar.java.build.JkJavaBuild;
import org.jerkar.java.build.JkJavaBuildPlugin;
import org.jerkar.java.testing.junit.JkUnit;
import org.jerkar.java.testing.junit.JkUnit.Enhancer;

@JkDoc("Performs Jacoco code coverage analysing while junit is running.")
public class JkBuildPluginJacoco extends JkJavaBuildPlugin {


	public static Enhancer enhancer(JkJavaBuild jakeJavaBuild) {
		return enhancer(jakeJavaBuild, jakeJavaBuild.baseDir().root());
	}

	private Enhancer enhancer;

	@Override
	public void configure(JkBuild jakeJavaBuild) {
		this.enhancer = enhancer((JkJavaBuild) jakeJavaBuild);
	}


	@Override
	public JkUnit alterUnitTester(JkUnit jkUnit) {
		return this.enhancer.enhance(jkUnit);
	}

	private static Enhancer enhancer(JkJavaBuild jakeJavaBuild, File agent) {
		final File destFile = new File(jakeJavaBuild.testReportDir(), "jacoco/jacoco.exec");
		return JkocoJunitEnhancer.of(destFile, jakeJavaBuild.baseDir().root());
	}


}
