package org.jerkar.builtins.javabuild.testing.junit;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.jerkar.JkClassLoader;
import org.jerkar.builtins.javabuild.testing.junit.JkUnit.JunitReportDetail;
import org.jerkar.utils.JkUtilsIO;
import org.jerkar.utils.JkUtilsTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

class JUnit4TestExecutor {

	/**
	 * Use this main class to run test in a separate process.
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException("There should be at least 2 args. "
					+ "First is the file where is serialized the result, and others are the classes to test.");
		}
		final File resultFile = new File(args[0]);
		final boolean printEachTestInConsole = Boolean.parseBoolean(args[1]);
		final JunitReportDetail reportDetail = JunitReportDetail.valueOf(args[2]);
		final File reportDir = new File(args[3]);
		final Class<?>[] classes = toClassArray(Arrays.copyOfRange(args, 4, args.length));
		final JkTestSuiteResult result = launchInProcess(classes, printEachTestInConsole, reportDetail, reportDir);
		JkUtilsIO.serialize(result, resultFile);
	}




	private static JkTestSuiteResult launchInProcess(Class<?>[] classes, boolean printEachTestOnConsole, JunitReportDetail reportDetail, File reportDir) {
		final JUnitCore jUnitCore = new JUnitCore();
		if (reportDetail.equals(JunitReportDetail.FULL)) {
			jUnitCore.addListener(new JUnitReportListener(reportDir));
		}
		final PrintStream out = System.out;
		final PrintStream err = System.err;
		if (printEachTestOnConsole) {
			jUnitCore.addListener(new JUnitConsoleListener());
		} else {
			System.setErr(JkUtilsIO.nopPrintStream());
			System.setOut(JkUtilsIO.nopPrintStream());
		}

		final Properties properties = (Properties) System.getProperties().clone();
		final long start = System.nanoTime();
		final Result result;
		try {
			result = jUnitCore.run(classes);
		} finally {
			System.setErr(err);
			System.setOut(out);
		}
		final long durationInMillis = JkUtilsTime.durationInMillis(start);
		return JkTestSuiteResult.fromJunit4Result(properties, "all", result, durationInMillis);
	}


	private static Class<?>[] toClassArray(String[] classNames) {
		final List<Class<?>> classes = new ArrayList<Class<?>>();
		for (final String each : classNames) {
			try {
				classes.add(Class.forName(each));
			} catch (final ClassNotFoundException e) {
				throw new IllegalArgumentException("Class "  + each
						+ " not found in classloader " + JkClassLoader.current());
			}
		}
		return classes.toArray(new Class[0]);
	}



}
