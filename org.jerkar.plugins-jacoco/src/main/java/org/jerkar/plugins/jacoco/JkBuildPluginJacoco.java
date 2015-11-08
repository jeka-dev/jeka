package org.jerkar.plugins.jacoco;

import java.io.File;

import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.junit.JkUnit.Enhancer;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaBuildPlugin;

/**
 * Alter the unitTester to be launched with the Jacoco agent. It results in
 * producing a jacoco.exec test coverage report file.
 * 
 * @author Jerome Angibaud
 */
@JkDoc("Performs Jacoco code coverage analysing while junit is running.")
public class JkBuildPluginJacoco extends JkJavaBuildPlugin {

    @JkDoc("true to produce an html report along the binary report")
    private boolean produceHtml;

    public static Enhancer enhancer(JkJavaBuild jkJavaBuild, boolean produceHtmlReport) {
	return enhancer(jkJavaBuild, jkJavaBuild.baseDir().root(), produceHtmlReport);
    }

    private Enhancer enhancer;

    @Override
    public void configure(JkBuild jkJavaBuild) {
	this.enhancer = enhancer((JkJavaBuild) jkJavaBuild, produceHtml);
    }

    @Override
    public JkUnit alterUnitTester(JkUnit jkUnit) {
	return this.enhancer.enhance(jkUnit);
    }

    private static Enhancer enhancer(JkJavaBuild jkJavaBuild, File agent, boolean html) {
	final File destFile = new File(jkJavaBuild.testReportDir(), "jacoco/jacoco.exec");
	if (html) {
	    throw new IllegalStateException("Sorry, not implemented yet. Please, turn off produceHtml flag.");
	}
	return JkocoJunitEnhancer.of(destFile);
    }

    public JkBuildPluginJacoco produceHtmlReport(boolean flag) {
	this.produceHtml = flag;
	return this;
    }

}
