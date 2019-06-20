package dev.jeka.core.api.java.junit;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.java.junit.JkUnit.JunitReportDetail;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkLocator;

class JUnit4TestLauncher {

    @SuppressWarnings("rawtypes")
    public static JkTestSuiteResult launchInFork(JkJavaProcess javaProcess,
                                                 boolean printEachTestOnConsole, JunitReportDetail reportDetail,
                                                 Iterable<Class> classes, File reportDir) {
        final List<String> args = new LinkedList<>();
        final Path file = JkUtilsPath.createTempFile("testResult-", ".ser");
        if (file.toAbsolutePath().toString().contains(" ")) {
            args.add("\"" + file.toAbsolutePath() + "\"");
        } else {
            args.add(file.toAbsolutePath().toString());
        }
        args.add(Boolean.toString(printEachTestOnConsole));
        args.add(reportDetail.name());
        String reportFileArg = reportDir == null ? JUnit4TestExecutor.NO_REPORT_FILE : reportDir.getAbsolutePath();
        if (reportFileArg.contains(" ")) {
            args.add("\"" + reportFileArg + "\"");
        } else {
            args.add(reportFileArg);
        }

        // Serialize log handler
        if (JkLog.getLogConsumer() != null && (JkLog.getLogConsumer() instanceof Serializable)) {
            Path path = JkUtilsPath.createTempFile("jk-logHandler", ".ser");
            JkUtilsIO.serialize(JkLog.getLogConsumer(), path);
            String pathString = path.normalize().toAbsolutePath().toString();
            if (pathString.contains(" ")) {
                args.add("\"" + pathString+ "\"");
            } else {
                args.add(pathString);
            }
        } else {
            args.add("\"\"");
        }

        // Classes to test
        for (final Class<?> clazz : classes) {
            args.add(clazz.getName());
        }
        final JkJavaProcess process;
        process = javaProcess.andClasspath(JkClasspath.of(JkLocator.getJekaJarPath()));
        process.runClassSync(JUnit4TestExecutor.class.getName(), args.toArray(new String[0]));
        return (JkTestSuiteResult) JkUtilsIO.deserialize(file);
    }


    /**
     * @param classes Non-empty <code>Iterable</code>.
     */
    public static JkTestSuiteResult launchInProcess(Iterable<Class> classes, boolean logRunningTest,
                                                    JunitReportDetail reportDetail, File reportDir) {
        final JkUrlClassLoader testClassloader = JkUrlClassLoader.ofLoaderOf(classes.iterator().next());
        final Class[] classArray = JkUtilsIterable.arrayOf(classes, Class.class);
        final JkUrlClassLoader launchtestClassLoader = JkUrlClassLoader.of(JkLocator.getJekaJarPath(),
                testClassloader.get());
        if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
            JkLog.trace("Launching test using class loader : " + testClassloader);
        }
        return launchtestClassLoader.toJkClassLoader().invokeStaticMethod(true, JUnit4TestExecutor.class.getName(),
                "launchInProcess", classArray, logRunningTest, reportDetail, reportDir, true);
    }

}
