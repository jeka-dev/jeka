package org.jerkar.java.testing.junit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.JkClassLoader;
import org.jerkar.JkClasspath;
import org.jerkar.JkLocator;
import org.jerkar.JkLog;
import org.jerkar.java.JkJavaProcess;
import org.jerkar.java.testing.junit.JkUnit.JunitReportDetail;
import org.jerkar.utils.JkUtilsFile;
import org.jerkar.utils.JkUtilsIO;
import org.jerkar.utils.JkUtilsIterable;

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
		process = jkJavaProcess.andClasspath(JkClasspath.of(JkLocator.jerkarFile()));
		process.startAndWaitFor(JUnit4TestExecutor.class.getName(), args.toArray(new String[0]));
		return (JkTestSuiteResult) JkUtilsIO.deserialize(file);
	}

	@SuppressWarnings("rawtypes")
	/**
	 * @param classes Non-empty <code>Iterable</code>.
	 */
	public static JkTestSuiteResult launchInClassLoader(Iterable<Class> classes, boolean verbose, JunitReportDetail reportDetail, File reportDir) {
		final JkClassLoader classloader = JkClassLoader.of(classes.iterator().next());
		final Class[] classArray = JkUtilsIterable.toArray(classes, Class.class);
		classloader.addEntry(JkLocator.jerkarFile());
		if (verbose) {
			JkLog.info("Launching test using class loader :");
			JkLog.info(classloader.toString());
		}
		return classloader.invokeStaticMethod(JUnit4TestExecutor.class.getName(), "launchInProcess", classArray, verbose, reportDetail, reportDir);
	}



}
