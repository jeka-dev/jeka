package org.jake.plugins.jacoco;

import java.io.File;

import org.jake.JakeBuild;
import org.jake.JakeDoc;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.build.JakeJavaBuildPlugin;
import org.jake.java.testing.junit.JakeUnit;
import org.jake.java.testing.junit.JakeUnit.Enhancer;

@JakeDoc("Performs Jacoco code coverage analysing while junit is running.")
public class JakeBuildPluginJacoco extends JakeJavaBuildPlugin {


	public static Enhancer enhancer(JakeJavaBuild jakeJavaBuild) {
		return enhancer(jakeJavaBuild, jakeJavaBuild.baseDir().root());
	}

	private Enhancer enhancer;

	@Override
	public void configure(JakeBuild jakeJavaBuild) {
		this.enhancer = enhancer((JakeJavaBuild) jakeJavaBuild);
	}


	@Override
	public JakeUnit alterUnitTester(JakeUnit jakeUnit) {
		return this.enhancer.enhance(jakeUnit);
	}

	private static Enhancer enhancer(JakeJavaBuild jakeJavaBuild, File agent) {
		final File destFile = new File(jakeJavaBuild.testReportDir(), "jacoco/jacoco.exec");
		return JakeocoJunitEnhancer.of(destFile, jakeJavaBuild.baseDir().root());
	}


}
