package org.jake.java.test.junit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeClassLoader;
import org.jake.JakeClasspath;
import org.jake.JakeLocator;
import org.jake.java.JakeJavaProcess;
import org.jake.java.test.junit.JakeUnit.JunitReportDetail;
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
		if (needJakeInClasspath(printEachTestOnConsole, reportDetail)) {
			process = jakeJavaProcess.andClasspath(JakeClasspath.of(JakeLocator.jakeJarFile()));
		} else {
			process = jakeJavaProcess;
		}
		process.startAndWaitFor(JUnit4TestExecutor.class.getName(), args.toArray(new String[0]));
		return (JakeTestSuiteResult) JakeUtilsIO.deserialize(file);
	}

	@SuppressWarnings("rawtypes")
	/**
	 * @param classes Non-empty <code>Iterable</code>.
	 */
	public static JakeTestSuiteResult launchInClassLoader(Iterable<Class> classes, boolean printEachTestOnConsole, JunitReportDetail reportDetail, File reportDir) {
		final JakeClassLoader classloader = JakeClassLoader.of(classes.iterator().next());
		final Class[] classArray = JakeUtilsIterable.toArray(classes, Class.class);
		if (needJakeInClasspath(printEachTestOnConsole, reportDetail)) {
			classloader.addEntry(JakeLocator.jakeJarFile());
		}
		return classloader.invokeStaticMethod(JUnit4TestExecutor.class.getName(), "launchInProcess", classArray, printEachTestOnConsole, reportDetail, reportDir);
	}


	private static boolean needJakeInClasspath(boolean printEachTestOnConsole, JunitReportDetail reportDetail) {
		return printEachTestOnConsole || reportDetail == JunitReportDetail.FULL;
	}

}
