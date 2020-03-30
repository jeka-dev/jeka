package dev.jeka.core.api.java.testplatform;

import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.function.JkUnaryOperator;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.platform.launcher.core.LauncherConfig;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.ServiceLoader;

/**
 * Processor executing a given bunch of tests existing in compiled Java classes. <p/>
 * This relies on Junit-platform (junit5), but having Junit-platform libraries on the classpath is optional.
 * For most of cases, processors can be used without coding against junit-platform API but it is possible to use
 * directly Junit-platform API for fine tuning.<p/>
 * Users can configure <ul>
 *     <li>Runtime support : classpath, forked process</li>
 *     <li>Engine behavior : listeners, reports, progress display </li>
 *     <li>The tests to run: discovery, selectors, filters, ...</li>
 * </ul>
 */
public final class JkTestProcessor {

    /**
     * Style of progress mark to display on console while the tests are running.
     */
    public enum JkProgressOutputStyle implements Serializable {
        FULL, TREE, ONE_LINE, SILENT
    }

    private static final String JAR_LOCATION = "META-INF/junitplatform/";

    private static final String ENGINE_SERVICE = "org.junit.platform.engine.TestEngine";

    private static final String PLATFORM_LAUNCHER_CLASS_NAME = "org.junit.platform.launcher.Launcher";

    private static final String PLATFORM_COMMONS_CLASS_NAME = "org.junit.platform.commons.JUnitException";

    private static final String PLATFORM_ENGINE_CLASS_NAME = "org.junit.platform.engine.TestEngine";

    private static final String PLATFORM_REPORT_CLASS_NAME =
            "org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener";

    private static final String OPENTEST4J_CLASS_NAME = "org.opentest4j.ValueWrapper";

    private static final String JUNIT_PLATFORM_LAUNCHER_JAR_NAME = "junit-platform-launcher-1.6.0.jar";

    private static final String JUNIT_PLATFORM_REPORTING_JAR_NAME = "junit-platform-reporting-1.6.0.jar";

    private static final String JUNIT_PLATFORM_ENGINE_JAR_NAME = "junit-platform-engine-1.6.0.jar";

    private static final String JUNIT_PLATFORM_COMMON_JAR_NAME = "junit-platform-commons-1.6.0.jar";

    private static final String JUNIT_VINTAGE_ENGINE_JAR_NAME = "junit-vintage-engine-5.6.0.jar";

    private static final String OPENTEST4J_JAR_NAME = "opentest4j-1.2.0.jar";

    private JkTestProcessor(JkEngineBehavior engineBehavior) {
        this.engineBehavior = engineBehavior;
    }

    private JkJavaProcess forkingProcess;

    private final JkEngineBehavior engineBehavior;

    private final JkRunnables postActions = JkRunnables.noOp();

    public static JkTestProcessor of() {
        return new JkTestProcessor(new JkEngineBehavior());
    }

    public JkJavaProcess getForkingProcess() {
        return forkingProcess;
    }

    public JkEngineBehavior getEngineBehavior() {
        return engineBehavior;
    }

    public JkRunnables getPostActions() {
        return postActions;
    }

    public JkTestProcessor setForkingProcess(JkJavaProcess process) {
        this.forkingProcess = process;
        return this;
    }

    public JkTestProcessor setForkingProcess(boolean fork) {
        if (fork) {
            if (forkingProcess != null) {
                return this;
            }
            this.forkingProcess = JkJavaProcess.of();
        }
        return this;
    }

    private JkClasspath computeClasspath(JkClasspath testClasspath) {
        JkClasspath result = testClasspath;
        JkClassLoader classloader = JkClassLoader.ofCurrent();
        result = addIfNeeded(result, classloader, PLATFORM_LAUNCHER_CLASS_NAME, JUNIT_PLATFORM_LAUNCHER_JAR_NAME);
        result = addIfNeeded(result, classloader, PLATFORM_REPORT_CLASS_NAME, JUNIT_PLATFORM_REPORTING_JAR_NAME);
        result = addIfNeeded(result, classloader, PLATFORM_COMMONS_CLASS_NAME, JUNIT_PLATFORM_COMMON_JAR_NAME);
        result = addIfNeeded(result, classloader, PLATFORM_ENGINE_CLASS_NAME, JUNIT_PLATFORM_ENGINE_JAR_NAME);
        result = addIfNeeded(result, classloader, OPENTEST4J_CLASS_NAME, OPENTEST4J_JAR_NAME);
        JkUrlClassLoader ucl = JkUrlClassLoader.of(result, classloader.get());
        Class<?> testEngineClass = ucl.toJkClassLoader().load(ENGINE_SERVICE);
        if (!ServiceLoader.load(testEngineClass, ucl.get()).iterator().hasNext()) {
            result = result.and(JkInternalClassloader.getEmbeddedLibAsPath(JAR_LOCATION
                    + JUNIT_VINTAGE_ENGINE_JAR_NAME));
        }
        return result;
    }

    private static JkClasspath addIfNeeded(JkClasspath classpath, JkClassLoader classloader,
                                           String className, String jarName) {
        JkClasspath result = classpath;
        if (!classloader.isDefined(className)) {
            if (result.getEntryContainingClass(className) == null) {
                result = result.and(JkInternalClassloader.getEmbeddedLibAsPath(JAR_LOCATION
                        + jarName));
            }
        }
        return result;
    }

    /**
     * Launches the specified test set with the underlying junit-platform. The classloader running the tests includes
     * the classpath of the current classloader plus the specified one.
     */
    public JkTestResult launch(JkClasspath extraTestClasspath, JkTestSelection testSelection) {
        JkLog.startTask("Executing tests");
        final JkTestResult result;
        if (forkingProcess == null) {
            result = launchInClassloader(extraTestClasspath, testSelection);
        } else {
            result = launchInForkedProcess(extraTestClasspath, testSelection);
        }
        postActions.run();
        JkLog.info("Result : " + result.getTestCount());
        JkLog.endTask();
        return result;
    }

    private JkTestResult launchInClassloader(JkClasspath testClasspath, JkTestSelection testSelection) {
        JkClasspath classpath = computeClasspath(testClasspath);
        return JkInternalJunitDoer.instance(classpath.entries()).launch(engineBehavior, testSelection);
    }

    private JkTestResult launchInForkedProcess(JkClasspath testClasspath, JkTestSelection testSelection) {
        Path serializedResultPath = JkUtilsPath.createTempFile("testResult-", ".ser");
        Args args = new Args();
        args.resultFile = serializedResultPath.toAbsolutePath().toString();
        args.engineBehavior = this.engineBehavior;
        args.testSelection = testSelection;
        Path serializedArgPath = JkUtilsPath.createTempFile("testArgs-", ".ser");
        JkUtilsIO.serialize(args, serializedArgPath);
        String arg = serializedArgPath.toAbsolutePath().toString();
        JkJavaProcess process = forkingProcess
                .withPrintCommand(false)
                .andClasspath(JkClassLoader.ofCurrent().getClasspath().
                        and(computeClasspath(testClasspath)).withoutDuplicates());
        process.runClassSync(JkTestProcessor.class.getName(), new String[] {arg});
        JkUtilsPath.deleteFile(serializedArgPath);
        JkTestResult result = JkUtilsIO.deserialize(serializedResultPath);
        JkUtilsPath.deleteFile(serializedResultPath);
        return result;
    }

    /**
     * Non public API. Used by #launchInClassloader.
     */
    public static void main(String[] args) {
        Path argFile = Paths.get(args[0]);
        Args data = JkUtilsIO.deserialize(argFile);
        JkTestResult result =
                JkInternalJunitDoer.instance(Collections.emptyList()).launch(data.engineBehavior, data.testSelection);
        JkUtilsIO.serialize(result, Paths.get(data.resultFile));
    }

    private static class Args implements Serializable {
        JkEngineBehavior engineBehavior;
        String resultFile;
        JkTestSelection testSelection;
    }

    public static class JkEngineBehavior implements Serializable {

        private String legacyReportDir; // Use String instead of Path for serialisation

        private JkProgressOutputStyle progressDisplayer;

        private JkUnaryOperator<LauncherConfig.Builder> launcherEnhancer;

        private JkEngineBehavior() {
        }

        public Path getLegacyReportDir() {
            return legacyReportDir == null ? null : Paths.get(legacyReportDir);
        }

        public JkProgressOutputStyle getProgressDisplayer() {
            return progressDisplayer;
        }

        public JkUnaryOperator<LauncherConfig.Builder> getLauncherEnhancer() {
            return launcherEnhancer;
        }

        /**
         * Sets the directory where will be generated the legacy standard XML report.
         * If {@code null}, no legacy standard XML report will be generated.
         */
        public JkEngineBehavior setLegacyReportDir(Path legacyReportDir) {
            this.legacyReportDir = legacyReportDir == null ? null : legacyReportDir.toString();
            return this;
        }

        /**
         * Sets the test progress type to display on the console.
         * If {@code null}, no progress will be displayed.
         */
        public JkEngineBehavior setProgressDisplayer(JkProgressOutputStyle progressDisplayer) {
            this.progressDisplayer = progressDisplayer;
            return this;
        }

        /**
         * Lets to setup the Junit-platform native {@link LauncherConfig} used to build
         * the {@link org.junit.platform.launcher.Launcher}.
         * @param launcherEnhancer a function that takes the default {@link LauncherConfig} as argument
         *                          and returns the config to use.
         */
        public JkEngineBehavior setLauncherEnhancer(JkUnaryOperator<LauncherConfig.Builder> launcherEnhancer) {
            this.launcherEnhancer = launcherEnhancer;
            return this;
        }

    }

}
