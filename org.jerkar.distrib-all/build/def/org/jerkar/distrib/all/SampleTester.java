package org.jerkar.distrib.all;

import java.io.File;
import java.util.Arrays;

import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsSystem;
import org.jerkar.tool.JkConstants;

class SampleTester {
	
	private final JkFileTree sampleBaseDir;
	
	private final JkFileTree out; 
	
	private File launchScript;
	
	SampleTester(JkFileTree buildDir) {
		super();
		this.sampleBaseDir = buildDir.from("../org.jerkar.script-samples");
		this.out = sampleBaseDir.from("build/output");
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
		scaffoldAndEclipse();
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
	
	private void scaffoldAndEclipse() {
		JkLog.startHeaded("Test scaffolding");
		File scafoldedProject = out.file("scaffolded");
		JkProcess scaffoldProcess = process().withWorkingDir(scafoldedProject);
		scafoldedProject.mkdirs();
		scaffoldProcess.withParameters("scaffold").runSync(); // scaffold project
		scaffoldProcess.runSync(); // Build the scaffolded project
		JkLog.infoUnderline("Test eclipse generation and compile            ");
		scaffoldProcess.withParameters("eclipse#generateFiles").runSync();
		scaffoldProcess.withParameters("eclipse#").runSync(); // build usng the .classpath for resolving classpath
		JkLog.done();
	}
	
	private JkProcess process() {
		return JkProcess.of(launchScript.getAbsolutePath())
				.failOnError(true);
		
	}

}
