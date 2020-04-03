package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.java.testing.JkTestResult;
import dev.jeka.core.api.java.testing.JkTestSelection;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;

/**
 * Handles project testing step. This involve both test compilation and run.
 * Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompiler}, {@link JkJavaCompileSpec} for test compilation and
 * {@link JkTestProcessor}, {@link JkTestSelection} for test run.
 */
public class JkJavaProjectTestingStep {

    private final JkJavaProject project;

    private final JkJavaProjectCompilationStep<JkJavaProjectTestingStep> testCompilation;

    private final JkJavaProjectCompilationStep prodCompilation;

    public final JkRunnables afterTest;

    private JkTestProcessor testProcessor;

    private JkTestSelection testSelection;

    // replative path from output dir
    private String reportDir = "test-report";

    private boolean done;

    private boolean skipped;

    private boolean breakOnFailures = true;

    /**
     * For parent chaining
     */
    public final JkJavaProject.JkSteps __;

    JkJavaProjectTestingStep(JkJavaProject project, JkJavaProject.JkSteps parent) {
        this.project = project;
        this.__ = parent;
        testCompilation = JkJavaProjectCompilationStep.ofTest(project, this);
        prodCompilation = parent.getCompilation();
        afterTest = JkRunnables.noOp(this);
        testProcessor = defaultTestProcessor();
        testSelection = defaultTestSelection();
    }

    /**
     * Returns tests to be run. The returned instance is mutable so users can modify it
     * from this method return.
     */
    public JkTestSelection<JkJavaProjectTestingStep> getTestSelection() {
        return testSelection;
    }

    /**
     * Returns processor running the tests. The returned instance is mutable so users can modify it
     * from this method return.
     */
    public JkTestProcessor<JkJavaProjectTestingStep> getTestProcessor() {
        return testProcessor;
    }

    /**
     * Returns the compilation step for the test part.
     */
    public JkJavaProjectCompilationStep<JkJavaProjectTestingStep> getTestCompilation() {
        return testCompilation;
    }

    /**
     * Returns the classpath to run the test. It consists in test classes + prod classes +
     * dependencies involved in TEST scope.
     */
    public JkClasspath getTestClasspath() {
        return JkClasspath.of(testCompilation.getLayout().getClassDir())
                .and(prodCompilation.getLayout().getClassDir())
                .and(project.getDependencyManagement()
                        .fetchDependencies(JkJavaDepScopes.SCOPES_FOR_TEST).getFiles());
    }

    /**
     * Returns if the tests should be skipped.
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Specifies if the tests should be skipped.
     */
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    /**
     * Returns if #run should fail (throwing a {@link dev.jeka.core.api.system.JkException}) if
     * test result has failures.
     */
    public boolean isBreakOnFailures() {
        return breakOnFailures;
    }

    public JkJavaProjectTestingStep setBreakOnFailures(boolean breakOnFailures) {
        this.breakOnFailures = breakOnFailures;
        return this;
    }

    public Path getReportDir() {
        return project.getOutputDir().resolve(reportDir);
    }

    public JkJavaProjectTestingStep setReportDir(String reportDir) {
        this.reportDir = reportDir;
        return this;
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
        JkLog.startTask("Processing tests");
        this.project.getSteps().getCompilation().runIfNecessary();
        this.testCompilation.run();
        executeWithTestProcessor();
        afterTest.run();
        JkLog.endTask();
    }

    /**
     * As #run but perfom only if not already done.
     */
    public void runIfNecessary() {
        if (done) {
            JkLog.trace("Test task already done. Won't perfom again.");
        } else if (skipped) {
            JkLog.info("Tests are skipped. Won't perfom.");
        } else {
            run();
            done = true;
        }
    }

    void reset() {
        done = false;
    }

    private void executeWithTestProcessor() {
        JkTestResult result = testProcessor.launch(getTestClasspath(), testSelection);
        if (breakOnFailures) {
            result.assertNoFailure();
        }
    }

    private JkTestProcessor<JkJavaProjectTestingStep> defaultTestProcessor() {
        JkTestProcessor result = JkTestProcessor.ofParent(this);
        final Path reportDir = testCompilation.getLayout().getOutputDir().resolve(this.reportDir);
        result.getEngineBehavior()
                .setLegacyReportDir(reportDir)
                .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.ONE_LINE);
        return result;
    }

    private JkTestSelection<JkJavaProjectTestingStep> defaultTestSelection() {
        return JkTestSelection.ofParent(this).addTestClassRoots(testCompilation.getLayout().getClassDir());
    }

}
