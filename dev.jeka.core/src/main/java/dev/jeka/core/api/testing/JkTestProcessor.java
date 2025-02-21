/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.testing;

import dev.jeka.core.api.depmanagement.JkCoordinateFileProxy;
import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.function.JkUnaryOperator;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import org.junit.platform.launcher.core.LauncherConfig;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Processor executing a given bunch of tests existing in compiled Java classes. <p/>
 * This relies on Junit-platform (junit5), but having Junit-platform libraries on the classpath is optional.
 * For most of the cases, processors can be used without coding against junit-platform API but, it is possible to use
 * directly Junit-platform API for fine-tuning.<p/>
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
    public enum JkProgressStyle implements Serializable {

        /**
         * Maven-like style progress.
         */
        FULL,

        /**
         * Maven-like progress, but skips test outputs.
         */
        PLAIN,

        /**
         * Prints a '.' for each test executed. Supports parallel tests.
         */
        STEP,

        /**
         * No output during test execution.
         */
        MUTE,

        /**
         * Shows a progress bar with the current test name.
         */
        BAR;

        private static final long serialVersionUID = 154654647775L;

    }

    private static final String ENGINE_SERVICE = "org.junit.platform.engine.TestEngine";

    private static final String PLATFORM_LAUNCHER_CLASS_NAME = "org.junit.platform.launcher.Launcher";

    // This class is absent from platform-engine 1.5.2,
    // so if 1.5.2 is present inh the classpath, we need to add 1.6 as well.
    private static final String PLATFORM_ENGINE_CLASS_NAME = "org.junit.platform.engine.EngineDiscoveryListener";

    private static final String PLATFORM_REPORT_CLASS_NAME =
            "org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener";

    private static final String JUNIT_PLATFORM_LAUNCHER_MODULE = "org.junit.platform:junit-platform-launcher";

    private static final String JUNIT_PLATFORM_REPORTING_MODULE = "org.junit.platform:junit-platform-reporting";

    private static final String JUNIT_PLATFORM_TEST_ENGINE_MODULE = "org.junit.platform:junit-platform-engine";

    private static final String JUNIT_PLATFORM_COMMON_MODULE = "org.junit.platform:junit-platform-commons";

    private JkJavaProcess forkingProcess = JkJavaProcess
            .ofJava(JkTestProcessor.class.getName())
            .setFailOnError(false)
            .setDestroyAtJvmShutdown(true);  // Tests are forked by default

    public final JkEngineBehavior engineBehavior;

    public final JkRunnables preActions = JkRunnables.of();

    /**
     * Collection of <i>Runnables</i> to be executed after the test processor has run.
     * <p>
     * It is typically used for generating reports after a test definition has run.
     * <p>
     * If you want to run another test 'suite', you'll need to use {@link dev.jeka.core.api.project.JkProjectTesting#postActions}
     * instead.
     */
    public final JkRunnables postActions = JkRunnables.of();

    // Using the latest platform versions might break compatibility with older JUnit 5 versions.
    @JkDepSuggest(versionOnly = true, hint="org.junit.platform:junit-platform-commons")
    private String junitPlatformVersion = "1.9.3";

    private JvmHints jvmHints = JvmHints.ofDefault();

    private Supplier<JkRepoSet> repoSetSupplier = () ->
            JkRepoProperties.of(JkProperties.ofStandardProperties()).getDownloadRepos();

    private JkTestProcessor() {
        engineBehavior = new JkEngineBehavior();
    }

    public static JkTestProcessor of() {
        return new JkTestProcessor();
    }

    public static boolean isEngineTestPresent() {
        return JkClassLoader.ofCurrent().isDefined(ENGINE_SERVICE);
    }

    public JkJavaProcess getForkingProcess() {
        return forkingProcess;
    }

    public JkTestProcessor setForkingProcess(JkJavaProcess process) {
        this.forkingProcess = process;
        return this;
    }

    public JkTestProcessor setJvmHints(JkJavaCompilerToolChain.JkJdks jdks, JkJavaVersion javaVersion) {
        JkUtilsAssert.argument(jdks != null, "jdks argument cannot be null");
        this.jvmHints = new JvmHints(jdks, javaVersion);
        return this;
    }

    public JkTestProcessor setForkingProcess(boolean fork) {
        if (fork) {
            if (forkingProcess != null) {
                return this;
            }
            this.forkingProcess = JkJavaProcess.ofJava(JkTestProcessor.class.getName());
        } else {
            forkingProcess = null;
        }
        return this;
    }

    public String getJunitPlatformVersion() {
        return junitPlatformVersion;
    }


    public JkTestProcessor setJunitPlatformVersion(
            @JkDepSuggest(versionOnly = true, hint="org.junit.platform:junit-platform-commons") String junitPlatformVersion) {
        this.junitPlatformVersion = junitPlatformVersion;
        return this;
    }

    public JkTestProcessor setRepoSetSupplier(Supplier<JkRepoSet> repoSetSupplier) {
        this.repoSetSupplier = repoSetSupplier;
        return this;
    }

    /**
     * Launches the specified test set with the underlying junit-platform. The classloader running the tests includes
     * the classpath of the current classloader plus the specified one.
     */
    public JkTestResult launch(JkPathSequence extraTestClasspath, JkTestSelection testSelection) {
        if (!testSelection.hasTestClasses()) {
            JkLog.info("No test class found in %s. No test to run." , testSelection.getTestClassRoots() );
            return JkTestResult.of();
        }
        final JkTestResult result;
        preActions.run();
        JkLog.startTask("execute-tests");
        JkLog.verbose(testSelection.toString());
        if (forkingProcess == null) {
            result = launchInClassloader(extraTestClasspath, testSelection);
        } else {
            result = launchInForkedProcess(extraTestClasspath, testSelection);
        }
        postActions.run();
        boolean success = result.getFailures().isEmpty();
        if (!success) {
            printTestFailure(result);
        }

        //String greenMsg = JkAnsi.of().fg(JkAnsi.Color.GREEN).a("ALL TESTS PASSED").reset().toString();
        String greenMsg = "ALL TEST PASSED";

        String summaryTitle = success ? greenMsg: "Summary";
        JkLog.info(summaryTitle + ": " + result.getTestCount().toReportString());
        JkLog.endTask();


        return result;
    }

    private List<Path> computeClasspath(JkPathSequence testClasspath) {
        JkPathSequence result = testClasspath;
        JkClassLoader classloader = JkClassLoader.ofCurrent();

        result = addIfNeeded(result, classloader, PLATFORM_LAUNCHER_CLASS_NAME, JUNIT_PLATFORM_LAUNCHER_MODULE);
        result = addIfNeeded(result, classloader, PLATFORM_REPORT_CLASS_NAME, JUNIT_PLATFORM_REPORTING_MODULE);
        result = addIfNeeded(result, classloader, ENGINE_SERVICE, JUNIT_PLATFORM_TEST_ENGINE_MODULE);

        result = addIfNeeded(result, classloader, "org.junit.platform.commons.logging.LoggerFactory, ",
                JUNIT_PLATFORM_COMMON_MODULE);

        JkUrlClassLoader ucl = JkUrlClassLoader.of(result, classloader.get());
        ucl.toJkClassLoader().load(ENGINE_SERVICE);
        return result.getEntries();
    }

    private void printTestFailure(JkTestResult testResult) {
        List<JkTestResult.JkFailure> failures = testResult.getFailures();
        String templateMsg = failures.size() == 1 ? "%s failure found." : "%s failures found.";
        String msg = String.format(templateMsg, failures.size());

        // For an unknown reason, JkLog.error implies an unexpected right padding when test are
        // run with the BAR progress style (not for the other)
        // The workaround is to fake an error log using the JkLog.info

        //String redMsg = JkAnsi.of().fg(JkAnsi.Color.RED).a("ERROR ").reset().toString();
        String redMsg = "ERROR: ";
        JkLog.info(redMsg + msg);

        for (JkTestResult.JkFailure failure : failures) {
            String exceptionClass = failure.isAssertionFailure() ? "" : failure.getThrowableClassname() + ":";

            String methodFqn = failure.getTestId().getDisplayName().replace("()", "");
            String shortenMethodName = JkUtilsString.removePackagePrefix(methodFqn);
            JkLog.info("    %s: %s %s",
                    shortenMethodName,
                    exceptionClass,
                    failure.getThrowableMessage()
                    );
        }
    }


    private JkPathSequence addIfNeeded(JkPathSequence classpath, JkClassLoader classloader,
                                       String className, String moduleName) {
        JkPathSequence result = classpath;
        if (!classloader.isDefined(className)) {
            if (result.findEntryContainingClass(className) == null) {
                String dep = moduleName + ":" + this.junitPlatformVersion;
                Path path = JkCoordinateFileProxy.of(this.repoSetSupplier.get(), dep).get();
                result = result.and(path);
            }
        }
        return result;
    }

    private JkTestResult launchInClassloader(JkPathSequence testClasspath, JkTestSelection testSelection) {
        List<Path> classpath = computeClasspath(testClasspath);
        if (JkLog.isDebug()) {
            JkLog.debug("------------ Test Classpath --------------");
            classpath.forEach(path -> JkLog.debug(path.toString()));
            JkLog.debug("------------------------------------------");
        }
        return JkInternalJunitDoer.instance(classpath).launch(engineBehavior, testSelection);
    }

    private JkTestResult launchInForkedProcess(JkPathSequence testClasspath, JkTestSelection testSelection) {
        Path serializedResultPath = JkUtilsPath.createTempFile("testResult-", ".ser");
        Args args = new Args();
        args.resultFile = serializedResultPath.toAbsolutePath().toString();
        args.engineBehavior = this.engineBehavior;
        args.testSelection = testSelection;
        Path serializedArgPath = JkUtilsPath.createTempFile("testArgs-", ".ser");
        JkUtilsIO.serialize(args, serializedArgPath);
        String arg = serializedArgPath.toAbsolutePath().toString();
        List<Path> classpath = JkClassLoader.ofCurrent().getClasspath()
                .andPrepend(computeClasspath(testClasspath)).withoutDuplicates().getEntries();
        if (JkLog.isDebug()) {
            JkLog.debug("------------ Test Classpath --------------");
            classpath.forEach(path -> JkLog.debug(path.toString()));
            JkLog.debug("------------------------------------------");
        }
        JkJavaProcess clonedProcess = forkingProcess.copy()
                .setLogCommand(JkLog.isVerbose())
                .setClasspath(classpath)
                .addParams(arg);
        Path specificJdkHome = this.jvmHints.javaHome();
        if (specificJdkHome != null) {
            JkLog.verbose("Run tests on JVM %s", specificJdkHome);
            clonedProcess.setParamAt(0, specificJdkHome.resolve("bin/java").toString());
        }
        clonedProcess.exec();

        JkUtilsPath.deleteFile(serializedArgPath);
        JkTestResult result = JkUtilsIO.deserialize(serializedResultPath);
        JkUtilsPath.deleteFile(serializedResultPath);
        return result;
    }

    /**
     * Non-public API. Used by #launchInClassloader.
     */
    public static void main(String[] args) {
        Path argFile = Paths.get(args[0]);
        Args data = JkUtilsIO.deserialize(argFile);
        JkTestResult result =
                JkInternalJunitDoer.instance(Collections.emptyList())
                        .launch(data.engineBehavior, data.testSelection);
        JkUtilsIO.serialize(result, Paths.get(data.resultFile));
        System.exit(0);  // Triggers shutdown hooks
    }

    private static class Args implements Serializable {

        private static final long serialVersionUID = 1L;

        JkEngineBehavior engineBehavior;

        String resultFile;

        JkTestSelection testSelection;

    }

    public static class JkEngineBehavior implements Serializable {

        private static final long serialVersionUID = 123454355656443L;

        private String legacyReportDir; // Use String instead of Path for serialisation

        private JkProgressStyle progressDisplayer;

        private JkUnaryOperator<LauncherConfig.Builder> launcherConfigurer;

        private JkEngineBehavior() {
        }

        public Path getLegacyReportDir() {
            return legacyReportDir == null ? null : Paths.get(legacyReportDir);
        }

        public JkProgressStyle getProgressStyle() {
            return progressDisplayer;
        }

        public JkUnaryOperator<LauncherConfig.Builder> getLauncherConfigurer() {
            return launcherConfigurer;
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
        public JkEngineBehavior setProgressDisplayer(JkProgressStyle progressDisplayer) {
            this.progressDisplayer = progressDisplayer;
            return this;
        }

        /**
         * Sets up the Junit-platform native {@link LauncherConfig} used to build
         * the {@link org.junit.platform.launcher.Launcher}.
         * @param launcherConfigurer a function that takes the default {@link LauncherConfig} as argument
         *                          and returns the config to use.
         */
        public JkEngineBehavior setLauncherConfigurer(JkUnaryOperator<LauncherConfig.Builder> launcherConfigurer) {
            this.launcherConfigurer = launcherConfigurer;
            return this;
        }

    }

    private static class JvmHints {
        final JkJavaCompilerToolChain.JkJdks jdks;
        final JkJavaVersion javaVersion;

        JvmHints(JkJavaCompilerToolChain.JkJdks jdks, JkJavaVersion javaVersion) {
            this.jdks = jdks;
            this.javaVersion = javaVersion;
        }

        static JvmHints ofDefault() {
            return new JvmHints(JkJavaCompilerToolChain.JkJdks.of(), null);
        }

        Path javaHome() {
            if (javaVersion == null) {
                return null;
            }
            return jdks.getHome(javaVersion);
        }
    }

}
