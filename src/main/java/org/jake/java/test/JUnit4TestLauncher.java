package org.jake.java.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jake.JakeException;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeClassloader;
import org.jake.java.JakeJavaProcess;
import org.jake.java.test.JakeJUnit.JunitReportDetail;
import org.jake.utils.JakeUtilsIO;
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
		final boolean fullreport = Boolean.getBoolean(args[2]);
		final Class<?>[] classes = toClassArray(Arrays.copyOfRange(args, 3, args.length-1));
		final JakeTestSuiteResult result = launchInProcess(classes, printEachTestInConsole, fullreport);
		JakeUtilsIO.serialize(result, resultFile);
	}

	public static JakeTestSuiteResult launchInFork(JakeJavaProcess jakeJavaProcess, boolean printEachTestOnConsole, JunitReportDetail reportDetail, Iterable<Class<?>> classes) {
		final List<String> args = new LinkedList<String>();
		final File file = JakeUtilsFile.createTempFile("testResult-", ".ser");
		args.add("\""+ file.getAbsolutePath() +"\"");
		args.add(Boolean.toString(printEachTestOnConsole));
		args.add(Boolean.toString(reportDetail == JunitReportDetail.FULL));
		for (final Class<?> clazz : classes) {
			args.add(clazz.getName());
		}
		final int result = jakeJavaProcess.startAndWaitFor(JUnit4TestLauncher.class.getName(), args.toArray(new String[0]));
		if (result != 0) {
			throw new JakeException("Process returned in error.");
		}
		return (JakeTestSuiteResult) JakeUtilsIO.deserialize(file);
	}


	private static JakeTestSuiteResult launchInProcess(Class<?>[] classes, boolean printEachTestOnConsole, boolean fullReport) {
		final JUnitCore jUnitCore = new JUnitCore();
		if (fullReport) {
			jUnitCore.addListener(new JUnitReportListener());
		}
		if (printEachTestOnConsole) {
			jUnitCore.addListener(new JUnitConsoleListener(System.out));
		}
		final Properties properties = (Properties) System.getProperties().clone();
		final long start = System.nanoTime();
		final Result result = jUnitCore.run(classes);
		final long end = System.nanoTime();
		final long duration = (end - start) / 1000000;
		return JakeTestSuiteResult.fromJunit4Result(properties, "all", result, duration);
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

}
