package org.jerkar.distrib.all;

import java.io.File;
import java.util.Arrays;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsSystem;
import org.jerkar.tool.JkLocator;

class SampleTester {
	
	private final JkFileTree sampleBaseDir;
	
	private File launchScript;
	
	SampleTester(JkFileTree buildDir) {
		super();
		this.sampleBaseDir = buildDir.from("../org.jerkar.script-samples");
		String scriptName = JkUtilsSystem.IS_WINDOWS ? "jerkar.bat" : "jerkar";
		launchScript = buildDir.file("build/output/dist/"+scriptName);
	}
	
	void doTest() {
		test("", "eclipse#generateFiles");
		test("AClassicBuild");
		test("AntStyleBuild");
		test("MavenStyleBuild");
		test("OpenSourceJarBuild");
		test("HttpClientTaskBuild");
	}
	
	private void test(String className, String ...args) {
		JkLog.startHeaded("Test " + className + " " + Arrays.toString(args));
		JkProcess.of(launchScript.getAbsolutePath())
			.withWorkingDir(sampleBaseDir.root())
			.withParametersIf(!JkUtilsString.isBlank(className), "-buildClass="+className)
			.andParameters(args)
			.failOnError(true)
			.runSync();
		JkLog.done();
	}

}
