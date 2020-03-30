package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.junit.*;
import dev.jeka.core.api.system.JkLog;

import java.nio.charset.Charset;
import java.nio.file.Path;

public class JkJavaProjectTestTasks {

    /**
     * File pattern for selecting Test classes.
     */
    public static final String[] TEST_CLASS_PATTERN = new String[] {"*Test.class", "**/*Test.class"};

    /**
     * File pattern for selecting Integration Test classes.
     */
    public static final String[] IT_CLASS_PATTERN = new String[] {"*IT.class", "**/*IT.class"};

    private final JkJavaProjectMaker maker;

    private final JkRunnables preTest = JkRunnables.of(() -> {});

    private final JkRunnables resourceGenerator = JkRunnables.of(() -> {});

    public final JkRunnables postTest = JkRunnables.of(() -> {});

    private final JkRunnables resourceProcessor;

    private final JkRunnables compileRunner;

    private JkUnit runner;

    private JkTestProcessor testProcessor;

    public final JkRunnables testExecutor = JkRunnables.of(this::execute4or5);

    private JkJavaCompiler compiler = JkJavaCompiler.ofJdk();

    private JkPathMatcher testClassMatcher = JkPathMatcher.of(true, TEST_CLASS_PATTERN);

    private boolean done;

    private boolean skipTests;

    // ----- Junit5

    private boolean breakOnFailures = true;

    private final JkTestSelection testSelection;

    private boolean useJunit5 = false;

    // ------

    JkJavaProjectTestTasks(JkJavaProjectMaker maker, Charset charset) {
        this.maker = maker;
        resourceProcessor = JkRunnables.of(() -> JkResourceProcessor.of(maker.project.getSourceLayout().getTestResources())
                .and(maker.project.getResourceInterpolators())
                .generateTo(maker.getOutLayout().getTestClassDir(), charset));
        compileRunner = JkRunnables.of(() -> {
            final JkJavaCompileSpec testCompileSpec = getTestCompileSpec();
            compiler.compile(testCompileSpec);
        });
        runner = getDefaultTester();

        // ----- Junit5
        testProcessor = defaultTestProcessor();
        testSelection = defaultTestSelection();
    }

    public JkRunnables getPreTest() {
        return preTest;
    }

    public JkRunnables getResourceGenerator() {
        return resourceGenerator;
    }

    public JkRunnables getResourceProcessor() {
        return resourceProcessor;
    }

    public JkRunnables getCompileRunner() {
        return compileRunner;
    }

    public JkJavaCompiler getCompiler() {
        return compiler;
    }

    public JkJavaProjectTestTasks setCompiler(JkJavaCompiler compiler) {
        this.compiler = compiler;
        return this;
    }

    public JkJavaProjectTestTasks setFork(boolean fork, String ... params) {
        this.compiler = this.compiler.withForking(fork, params);
        return this;
    }

    public JkUnit getRunner() {
        return runner;
    }

    public void setRunner(JkUnit runner) {
        this.runner = runner;
    }

    private JkUnit getDefaultTester() {
        final Path junitReport = maker.getOutLayout().getTestReportDir().resolve("junit");
        return JkUnit.of().withOutputOnConsole(false).withReport(JkUnit.JunitReportDetail.BASIC)
                .withReportDir(junitReport);
    }

    public JkJavaTestClasses getTestClasses() {
        return JkJavaTestClasses.of(getTestClasspath(),
                JkPathTreeSet.of(maker.getOutLayout().getTestClassDir()).andMatcher(testClassMatcher));
    }

    public JkJavaProjectTestTasks setForkRun(boolean fork) {
        this.runner = runner.withForking(fork);
        this.testProcessor.setForkingProcess(fork);
        return this;
    }

    public JkJavaProjectTestTasks setForkCompile(boolean fork, String ... params) {
        compiler = compiler.withForking(fork, params);
        return this;
    }

    public JkPathMatcher getTestClassMatcher() {
        return testClassMatcher;
    }

    public JkJavaProjectTestTasks setTestClassMatcher(JkPathMatcher testClassMatcher) {
        this.testClassMatcher = testClassMatcher;
        return this;
    }

    private void execute4or5() {
        if (useJunit5) {
            executeWithTestProcessor();
        } else {
            executeWithJuni4();
        }
    }

    private void executeWithJuni4() {
        runner.run(getTestClasses());
    }

    private JkJavaCompileSpec getTestCompileSpec() {
        JkJavaCompileSpec result = maker.project.getCompileSpec().copy();
        final JkPathSequence classpath = maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_TEST).andPrepending(maker.getOutLayout().getClassDir());
        return result
                .setClasspath(classpath)
                .addSources(maker.project.getSourceLayout().getTests())
                .setOutputDir(maker.getOutLayout().getTestClassDir());
    }

    public JkClasspath getTestClasspath() {
        return JkClasspath.of(maker.getOutLayout().getTestClassDir())
                .and(maker.getOutLayout().getClassDir())
                .and(maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_TEST));
    }

    public boolean isTestSkipped() {
        return skipTests;
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    /**
     * Performs entire test phase, including : <ul>
     *     <li>compile regular code if needed</li>
     *     <li>perform pre test tasks if present</li>
     *     <li>compile test code and process test resources</li>
     *     <li>execute compiled tests</li>
     *     <li>execute post tesks if present</li>
     * </ul>
     */
    public void run() {
        maker.getTasksForCompilation().runIfNecessary();
        JkLog.startTask("Running unit tests");
        if (maker.project.getSourceLayout().getTests().count(0, false) == 0) {
            JkLog.info("No unit test found in : " + maker.project.getSourceLayout().getTests());
        } else {
            this.maker.getTasksForCompilation().runIfNecessary();
            preTest.run();
            compileRunner.run();
            resourceGenerator.run();
            resourceProcessor.run();
            testExecutor.run();
            postTest.run();
        }
        JkLog.endTask();
    }

    /**
     * As #run but perfom only if not already done.
     */
    public void runIfNecessary() {
        if (done) {
            JkLog.trace("Test task already done. Won't perfom again.");
        } else if (skipTests) {
            JkLog.info("Tests are skipped. Won't perfom.");
        } else {
            run();
            done = true;
        }
    }

    void reset() {
        done = false;
    }

    // ------ JUnit5

    public boolean isBreakOnFailures() {
        return breakOnFailures;
    }

    public JkJavaProjectTestTasks setBreakOnFailures(boolean breakOnFailures) {
        this.breakOnFailures = breakOnFailures;
        this.runner = runner.withBreakOnFailure(breakOnFailures);
        return this;
    }

    public JkTestSelection getTestSelection() {
        return testSelection;
    }

    public JkTestProcessor getTestProcessor() {
        return testProcessor;
    }

    public JkJavaProjectTestTasks setUseJunit5(boolean useJunit5) {
        this.useJunit5 = useJunit5;
        return this;
    }

    private void executeWithTestProcessor() {
        JkTestResult result = testProcessor.launch(getTestClasspath(), testSelection);
        if (breakOnFailures) {
            result.assertNoFailure();
        }
    }

    private JkTestProcessor defaultTestProcessor() {
        JkTestProcessor result = JkTestProcessor.of();
        final Path reportDir = maker.getOutLayout().getTestReportDir().resolve("junit");
        result.getEngineBehavior()
                .setLegacyReportDir(reportDir)
                .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.ONE_LINE);
        return result;
    }

    private JkTestSelection defaultTestSelection() {
        return JkTestSelection.ofStandard(maker.getOutLayout().getTestClassDir());
    }

}
