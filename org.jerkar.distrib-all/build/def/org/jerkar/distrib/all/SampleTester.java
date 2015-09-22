package org.jerkar.distrib.all;

import java.util.Arrays;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsSystem;

class SampleTester {
	
	private final JkFileTree sampleBaseDir;

	SampleTester(JkFileTree buildDir) {
		super();
		this.sampleBaseDir = buildDir.from("../org.jerkar.script-samples");
	}
	
	void doTest() {
		test("", "eclipse#generateFiles");
		test("AClassicBuild");
		test("AntStyleBuild");
		test("MavenStyleBuild");
		test("OpenSourceJarBuild");
		test("HttpClientTaskBuild");
		test("SonarParametrizedBuild");	
	}
	
	private void test(String className, String ...args) {
		JkLog.startHeaded("Test " + className + " " + Arrays.toString(args));
		String script = JkUtilsSystem.IS_WINDOWS ? "jerkar.bat" : "jerkar";
		JkProcess.of(script).withWorkingDir(sampleBaseDir.root())
			.withParametersIf(!JkUtilsString.isBlank(className), "-buildClass="+className)
			.andParameters(args).failOnError(true)
			.runSync();
		JkLog.done();
	}

}
