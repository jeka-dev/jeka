package dev.jeka.core.api.java.junit;

import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.java.junit.JkUnit.JunitReportDetail;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsTime;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Class to run test in a separate process.
 */
class JUnit4TestExecutor {

    static final String NO_REPORT_FILE = "NoReportFile";

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException(
                    "There should be at least 2 args. "
                            + "First is the file containing serialized result and others are the classes to test.");
        }
        final Path resultFile = Paths.get(args[0]);
        final boolean printEachTestInConsole = Boolean.parseBoolean(args[1]);
        final JunitReportDetail reportDetail = JunitReportDetail.valueOf(args[2]);
        final Path reportDir = NO_REPORT_FILE.equals(args[3]) ? null : Paths.get(args[3]);
        final String logHandlerSerPath = args[4];
        if (!logHandlerSerPath.isEmpty()) {
            Path serFile = Paths.get(logHandlerSerPath);
            JkLog.EventLogHandler eventLogHandler = (JkLog.EventLogHandler) JkUtilsIO.deserialize(serFile);
            JkUtilsPath.deleteFile(serFile);
            JkLog.register(eventLogHandler);
        }
        final File reportDirFile = reportDir == null ? null : reportDir.toFile();
        final Class<?>[] classes = toClassArray(Arrays.copyOfRange(args, 5, args.length));
        final JkTestSuiteResult result = launchInProcess(classes, printEachTestInConsole,
                reportDetail, reportDirFile, false);
        JkUtilsIO.serialize(result, resultFile);
    }

    // This method is also called by Junit4TestLaunch using reflection cross classloader.
    private static JkTestSuiteResult launchInProcess(Class<?>[] classes,
            boolean printEachTestOnConsole, JunitReportDetail reportDetail, File reportDir,
            boolean restoreSystemOut) {
        final JUnitCore jUnitCore = new JUnitCore();
        if (reportDetail.equals(JunitReportDetail.FULL)) {
            if (reportDir == null) {
                throw new JkException("No report dir has been specified to output test report.");
            }
            jUnitCore.addListener(new JUnitReportListener(reportDir.toPath()));
        }
        final PrintStream out = System.out;
        final PrintStream err = System.err;
        final JkLog.Verbosity previousVerbosity = JkLog.verbosity();
        if (printEachTestOnConsole) {
            jUnitCore.addListener(new PrintConsoleTestListener());
        } else {
            jUnitCore.addListener(new ProgressTestListener(JkLog.getOutputStream()));
            JkLog.setVerbosity(JkLog.Verbosity.MUTE);
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
                JkLog.setVerbosity(previousVerbosity);
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
                        + JkUrlClassLoader.ofCurrent());
            }
        }
        return classes.toArray(new Class[0]);
    }

}
