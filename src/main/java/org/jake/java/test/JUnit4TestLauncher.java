package org.jake.java.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jake.JakeException;
import org.jake.JakeLocator;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeClassloader;
import org.jake.java.JakeClasspath;
import org.jake.java.JakeJavaProcess;
import org.jake.java.test.JakeJUnit.JunitReportDetail;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

class JUnit4TestLauncher {

	/**
	 * Use this main class to run test in a separate process.
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException("There should be at least 2 args. "
					+ "First is the file where is serialized the result, and others are the classes to test.");
		}
		final File resultFile = new File(args[0]);
		final boolean printEachTestInConsole = Boolean.getBoolean(args[1]);
		final JunitReportDetail reportDetail = JunitReportDetail.valueOf(args[2]);
		final File reportDir = new File(args[3]);
		final Class<?>[] classes = toClassArray(Arrays.copyOfRange(args, 4, args.length-1));
		final JakeTestSuiteResult result = launchInProcess(classes, printEachTestInConsole, reportDetail, reportDir);
		JakeUtilsIO.serialize(result, resultFile);
	}

	public static JakeTestSuiteResult launchInFork(JakeJavaProcess jakeJavaProcess, boolean printEachTestOnConsole, JunitReportDetail reportDetail, Iterable<Class<?>> classes) {
		final List<String> args = new LinkedList<String>();
		final File file = JakeUtilsFile.createTempFile("testResult-", ".ser");
		args.add("\""+ file.getAbsolutePath() +"\"");
		args.add(Boolean.toString(printEachTestOnConsole));
		args.add(reportDetail.name());
		for (final Class<?> clazz : classes) {
			args.add(clazz.getName());
		}
		final JakeJavaProcess process;
		if (needJakeInClasspath(printEachTestOnConsole, reportDetail)) {
			process = jakeJavaProcess.andClasspath(JakeClasspath.of(JakeLocator.jakeJarFile()));
		} else {
			process = jakeJavaProcess;
		}
		final int result = process.startAndWaitFor(JUnit4TestLauncher.class.getName(), args.toArray(new String[0]));
		if (result != 0) {
			throw new JakeException("Process returned in error.");
		}
		return (JakeTestSuiteResult) JakeUtilsIO.deserialize(file);
	}

	@SuppressWarnings("rawtypes")
	public static JakeTestSuiteResult launchInClassLoader(JakeClassloader classloader, Iterable<Class> classes, boolean printEachTestOnConsole, JunitReportDetail reportDetail, File reportDir) {
		final Class[] classArray = JakeUtilsIterable.toArray(classes, Class.class);
		final JakeClassloader loader;
		if (needJakeInClasspath(printEachTestOnConsole, reportDetail)) {
			loader = classloader.and(JakeLocator.jakeJarFile());
		} else {
			loader = classloader;
		}
		return loader.invokeStaticMethod(JUnit4TestLauncher.class.getName(), "launchInProcess", classArray, printEachTestOnConsole, reportDetail, reportDir);
	}


	private static JakeTestSuiteResult launchInProcess(Class<?>[] classes, boolean printEachTestOnConsole, JunitReportDetail reportDetail, File reportDir) {
		final JUnitCore jUnitCore = new JUnitCore();
		if (reportDetail.equals(JunitReportDetail.FULL)) {
			jUnitCore.addListener(new JUnitReportListener());
		}
		if (printEachTestOnConsole) {
			jUnitCore.addListener(new JUnitConsoleListener(System.out));
		}
		final Properties properties = (Properties) System.getProperties().clone();
		final long start = System.nanoTime();
		final Result result = jUnitCore.run(classes);
		final long durationInMillis = JakeUtilsTime.durationInMillis(start);
		return JakeTestSuiteResult.fromJunit4Result(properties, "all", result, durationInMillis);
	}


	private static Class<?>[] toClassArray(String[] classNames) {
		final List<Class<?>> classes = new ArrayList<Class<?>>();
		for (final String each : classNames) {
			try {
				classes.add(Class.forName(each));
			} catch (final ClassNotFoundException e) {
				throw new IllegalArgumentException("Class "  + each
						+ " not found in classloader " + JakeClassloader.current());
			}
		}
		return classes.toArray(new Class[0]);
	}

	private static boolean needJakeInClasspath(boolean printEachTestOnConsole, JunitReportDetail reportDetail) {
		return printEachTestOnConsole || reportDetail == JunitReportDetail.FULL;
	}

}
