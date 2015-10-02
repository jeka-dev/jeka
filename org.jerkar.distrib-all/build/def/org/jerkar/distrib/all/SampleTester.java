package org.jerkar.distrib.all;

import java.io.File;
import java.util.Arrays;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsSystem;
import org.jerkar.tool.JkConstants;

class SampleTester {
	
	private final JkFileTree sampleBaseDir;
	
	private final JkFileTree sampleDependeeBaseDir;
	
	private final JkFileTree output; 
	
	private File launchScript;
	
	SampleTester(JkFileTree buildDir) {
		super();
		this.sampleBaseDir = buildDir.from("../org.jerkar.samples");
		this.sampleDependeeBaseDir = buildDir.from("../org.jerkar.samples-dependee");
		this.output = sampleBaseDir.from("build/output");
		String scriptName = JkUtilsSystem.IS_WINDOWS ? "jerkar.bat" : "jerkar";
		launchScript = buildDir.file("build/output/dist/"+scriptName);
	}
	
	void doTest() {
		testSamples("", "eclipse#generateFiles");
		testSamples("AClassicBuild");
		testSamples("AntStyleBuild");
		testSamples("MavenStyleBuild");
		testSamples("OpenSourceJarBuild");
		testSamples("HttpClientTaskBuild");
		testSamples("", "doPublish");
		scaffoldAndEclipse();
		testDependee("FatJarBuild");
		testDependee("NormalJarBuild");
		testFork();
	}
	
	private void testSamples(String className, String ...args) {
		JkLog.infoHeaded("Test " + className + " " + Arrays.toString(args));
		JkProcess.of(launchScript.getAbsolutePath())
			.withWorkingDir(sampleBaseDir.root())
			.withParametersIf(!JkUtilsString.isBlank(className), "-buildClass="+className)
			.andParameters(args)
			.failOnError(true)
			.runSync();
	}
	
	private void testDependee(String className, String ...args) {
		JkLog.infoHeaded("Test " + className + " " + Arrays.toString(args));
		JkProcess.of(launchScript.getAbsolutePath())
			.withWorkingDir(this.sampleDependeeBaseDir.root())
			.withParametersIf(!JkUtilsString.isBlank(className), "-buildClass="+className)
			.andParameters(args)
			.failOnError(true)
			.runSync();
	}
	
	private void scaffoldAndEclipse() {
		JkLog.startHeaded("Test scaffolding");
		File scafoldedProject = output.file("scaffolded");
		JkProcess scaffoldProcess = process().withWorkingDir(scafoldedProject);
		scafoldedProject.mkdirs();
		scaffoldProcess.withParameters("scaffold").runSync(); // scaffold project
		scaffoldProcess.runSync(); // Build the scaffolded project
		JkLog.infoUnderlined("Test eclipse generation and compile            ");
		scaffoldProcess.withParameters("eclipse#generateFiles").runSync();
		scaffoldProcess.withParameters("eclipse#").runSync(); // build usng the .classpath for resolving classpath
		JkLog.done();
	}
	
	private JkProcess process() {
		return JkProcess.of(launchScript.getAbsolutePath())
				.failOnError(true);
	}
	
	private void testFork() {
		testSamples("", "-tests.fork");
		JkUtilsAssert.isTrue(output.file("test-reports/junit").exists(), "No test report generated in test fork mode.");
	}

}
