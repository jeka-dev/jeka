package org.jake.java.testing.junit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeClassLoader;
import org.jake.JakeClasspath;
import org.jake.JakeLocator;
import org.jake.JakeLog;
import org.jake.java.JakeJavaProcess;
import org.jake.java.testing.junit.JakeUnit.JunitReportDetail;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;

class JUnit4TestLauncher {

	@SuppressWarnings("rawtypes")
	public static JakeTestSuiteResult launchInFork(JakeJavaProcess jakeJavaProcess, boolean printEachTestOnConsole, JunitReportDetail reportDetail, Iterable<Class> classes, File reportDir) {
		final List<String> args = new LinkedList<String>();
		final File file = JakeUtilsFile.createTempFile("testResult-", ".ser");
		args.add("\""+ file.getAbsolutePath() +"\"");
		args.add(Boolean.toString(printEachTestOnConsole));
		args.add(reportDetail.name());
		args.add("\""+ reportDir.getAbsolutePath() +"\"");
		for (final Class<?> clazz : classes) {
			args.add(clazz.getName());
		}
		final JakeJavaProcess process;
		process = jakeJavaProcess.andClasspath(JakeClasspath.of(JakeLocator.jakeJarFile()));
		process.startAndWaitFor(JUnit4TestExecutor.class.getName(), args.toArray(new String[0]));
		return (JakeTestSuiteResult) JakeUtilsIO.deserialize(file);
	}

	@SuppressWarnings("rawtypes")
	/**
	 * @param classes Non-empty <code>Iterable</code>.
	 */
	public static JakeTestSuiteResult launchInClassLoader(Iterable<Class> classes, boolean verbose, JunitReportDetail reportDetail, File reportDir) {
		final JakeClassLoader classloader = JakeClassLoader.of(classes.iterator().next());
		final Class[] classArray = JakeUtilsIterable.toArray(classes, Class.class);
		classloader.addEntry(JakeLocator.jakeJarFile());
		if (verbose) {
			JakeLog.info("Launching test using class loader :");
			JakeLog.info(classloader.toString());
		}
		return classloader.invokeStaticMethod(JUnit4TestExecutor.class.getName(), "launchInProcess", classArray, verbose, reportDetail, reportDir);
	}



}
