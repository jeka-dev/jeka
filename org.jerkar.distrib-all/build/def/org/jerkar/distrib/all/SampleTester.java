package org.jerkar.distrib.all;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.system.JkProcess;

class SampleTester {
	
	private final JkFileTree sampleBaseDir;

	SampleTester(JkFileTree buildDir) {
		super();
		this.sampleBaseDir = buildDir.from("../org.jerkar.script-samples");
	}
	
	void doTest() {
		test("HttpClientTaskBuild", "eclipse#generateFiles");
		test("AClassicBuild");
		test("AntStyleBuild");
		test("MavenStyleBuild");
		test("OpenSourceJarBuild");
		test("HttpClientTaskBuild");
		test("SonarParametrizedBuild");	
	}
	
	private void test(String className, String ...args) {
		JkProcess.of("jerkar.bat").withWorkingDir(sampleBaseDir.root())
			.withParameters("-buildClass="+className)
			.andParameters(args).failOnError(true)
			.runSync();
	}

}
