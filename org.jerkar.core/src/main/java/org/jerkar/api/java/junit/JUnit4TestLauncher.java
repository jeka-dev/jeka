package org.jerkar.api.java.junit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.tool.JkLocator;

class JUnit4TestLauncher {

	@SuppressWarnings("rawtypes")
	public static JkTestSuiteResult launchInFork(JkJavaProcess jkJavaProcess, boolean printEachTestOnConsole, JunitReportDetail reportDetail, Iterable<Class> classes, File reportDir) {
		final List<String> args = new LinkedList<String>();
		final File file = JkUtilsFile.createTempFile("testResult-", ".ser");
		args.add("\""+ file.getAbsolutePath() +"\"");
		args.add(Boolean.toString(printEachTestOnConsole));
		args.add(reportDetail.name());
		args.add("\""+ reportDir.getAbsolutePath() +"\"");
		for (final Class<?> clazz : classes) {
			args.add(clazz.getName());
		}
		final JkJavaProcess process;
		process = jkJavaProcess.andClasspath(JkClasspath.of(JkLocator.jerkarJarFile()));
		process.runClassSync(JUnit4TestExecutor.class.getName(), args.toArray(new String[0]));
		return (JkTestSuiteResult) JkUtilsIO.deserialize(file);
	}

	@SuppressWarnings("rawtypes")
	/**
	 * @param classes Non-empty <code>Iterable</code>.
	 */
	public static JkTestSuiteResult launchInClassLoader(Iterable<Class> classes, boolean verbose, JunitReportDetail reportDetail, File reportDir) {
		final JkClassLoader classloader = JkClassLoader.of(classes.iterator().next());
		final Class[] classArray = JkUtilsIterable.arrayOf(classes, Class.class);
		classloader.addEntry(JkLocator.jerkarJarFile());
		if (verbose) {
			JkLog.info("Launching test using class loader :");
			JkLog.info(classloader.toString());
		}
		return classloader.invokeStaticMethod(true, JUnit4TestExecutor.class.getName(), "launchInProcess", classArray, verbose, reportDetail, reportDir);
	}



}
