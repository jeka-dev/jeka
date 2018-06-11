package org.jerkar.api.java.junit;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.api.system.JkEvent;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

/**
 * Class to run test in a separate process.
 */
class JUnit4TestExecutor {

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException(
                    "There should be at least 2 args. "
                            + "First is the file containing serialized result and others are the classes to test.");
        }
        final File resultFile = new File(args[0]);
        final boolean printEachTestInConsole = Boolean.parseBoolean(args[1]);
        final JunitReportDetail reportDetail = JunitReportDetail.valueOf(args[2]);
        final File reportDir = new File(args[3]);
        final Class<?>[] classes = toClassArray(Arrays.copyOfRange(args, 4, args.length));
        final JkTestSuiteResult result = launchInProcess(classes, printEachTestInConsole,
                reportDetail, reportDir, false);
        JkUtilsIO.serialize(result, resultFile);
    }

    private static JkTestSuiteResult launchInProcess(Class<?>[] classes,
            boolean printEachTestOnConsole, JunitReportDetail reportDetail, File reportDir,
            boolean restoreSystemOut) {
        final JUnitCore jUnitCore = new JUnitCore();

        if (reportDetail.equals(JunitReportDetail.FULL)) {
            jUnitCore.addListener(new JUnitReportListener(reportDir.toPath()));
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
            if (restoreSystemOut) {
                System.setErr(err);
                System.setOut(out);
            }
        }
        final long durationInMillis = JkUtilsTime.durationInMillis(start);
        return JkTestSuiteResult.fromJunit4Result(properties, "all", result, durationInMillis);
    }

    private static Class<?>[] toClassArray(String[] classNames) {
        final List<Class<?>> classes = new ArrayList<>();
        for (final String className : classNames) {
            try {
                classes.add(Class.forName(className));
            } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException("Class " + className + " not found in classloader "
                        + JkClassLoader.current());
            }
        }
        return classes.toArray(new Class[0]);
    }

}
