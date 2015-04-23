package org.jerkar.plugins.jacoco;

import java.io.File;

import org.jerkar.JkBuild;
import org.jerkar.JkDoc;
import org.jerkar.builtins.javabuild.build.JkJavaBuild;
import org.jerkar.builtins.javabuild.build.JkJavaBuildPlugin;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit.Enhancer;

@JkDoc("Performs Jacoco code coverage analysing while junit is running.")
public class JkBuildPluginJacoco extends JkJavaBuildPlugin {


	public static Enhancer enhancer(JkJavaBuild jkJavaBuild) {
		return enhancer(jkJavaBuild, jkJavaBuild.baseDir().root());
	}

	private Enhancer enhancer;

	@Override
	public void configure(JkBuild jkJavaBuild) {
		this.enhancer = enhancer((JkJavaBuild) jkJavaBuild);
	}


	@Override
	public JkUnit alterUnitTester(JkUnit jkUnit) {
		return this.enhancer.enhance(jkUnit);
	}

	private static Enhancer enhancer(JkJavaBuild jkJavaBuild, File agent) {
		final File destFile = new File(jkJavaBuild.testReportDir(), "jacoco/jacoco.exec");
		return JkocoJunitEnhancer.of(destFile, jkJavaBuild.baseDir().root());
	}


}
