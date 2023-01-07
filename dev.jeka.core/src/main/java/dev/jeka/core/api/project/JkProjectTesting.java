package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestResult;
import dev.jeka.core.api.testing.JkTestSelection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Handles project testing step. This involves both test compilation and run.
 * Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompiler}, {@link JkJavaCompileSpec} for test compilation and
 * {@link JkTestProcessor}, {@link JkTestSelection} for test run.
 */
public class JkProjectTesting {

    private final JkProject project;

    public final JkProjectCompilation<JkProjectTesting> testCompilation;

    /**
     * The processor running the tests.
     */
    public final JkTestProcessor<JkProjectTesting>  testProcessor;

    /**
     * Tests to be run.
     */
    public final JkTestSelection<JkProjectTesting> testSelection;

    // relative path from output dir
    private String reportDir = "test-report";

    private boolean done;

    private boolean skipped;

    private boolean breakOnFailures = true;

    /**
     * For parent chaining
     */
    public final JkProject __;

    JkProjectTesting(JkProject project) {
        this.project = project;
        this.__ = project;
        testCompilation = new JkProjectTestCompilation();
        testProcessor = defaultTestProcessor();
        testSelection = defaultTestSelection();
    }

    public JkProjectTesting apply(Consumer<JkProjectTesting> consumer) {
        consumer.accept(this);
        return this;
    }


    /**
     * Returns the classpath to run the test. It consists in test classes + prod classes +
     * dependencies defined in testing/compile.
     */
    public JkPathSequence getTestClasspath() {
        JkProjectCompilation prodCompilation = project.prodCompilation;
        return JkPathSequence.of()
                .and(testCompilation.initialLayout().resolveClassDir())
                .and(testCompilation.resolveDependencies().getFiles())
                .and(prodCompilation.layout.resolveClassDir())
                .and(project.packaging.resolveRuntimeDependencies().getFiles())
                .withoutDuplicates();
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
    public JkProjectTesting setSkipped(boolean skipped) {
        this.skipped = skipped;
        return this;
    }

    /**
     * Returns if #run should fail (throwing a {@link IllegalArgumentException}) if test result has failures.
     */
    public boolean isBreakOnFailures() {
        return breakOnFailures;
    }

    public JkProjectTesting setBreakOnFailures(boolean breakOnFailures) {
        this.breakOnFailures = breakOnFailures;
        return this;
    }

    public Path getReportDir() {
        return project.getOutputDir().resolve(reportDir);
    }

    public JkProjectTesting setReportDir(String reportDir) {
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
        JkLog.startTask("Process tests");
        this.project.prodCompilation.runIfNeeded();
        this.testCompilation.run();
        if (!JkPathTree.of(this.testCompilation.layout.resolveClassDir()).containFiles()) {
            JkLog.endTask("No tests to execute.");
            return;
        }
        executeWithTestProcessor();
        JkLog.endTask();
    }

    /**
     * As #run, but performs only if not already done.
     */
    public void runIfNeeded() {
        if (done) {
            JkLog.trace("Tests has already been performed. Won't do it again.");
        } else if (skipped) {
            JkLog.info("Tests are skipped. Won't perform.");
        } else {
            run();
            done = true;
        }
    }

    void reset() {
        done = false;
    }

    private void executeWithTestProcessor() {
        UnaryOperator<JkPathSequence> op = paths -> paths.resolvedTo(project.getOutputDir());
        testSelection.setTestClassRoots(op);
        JkTestResult result = testProcessor.launch(getTestClasspath(), testSelection);
        if (breakOnFailures) {
            result.assertNoFailure();
        }
    }

    private JkTestProcessor<JkProjectTesting> defaultTestProcessor() {
        JkTestProcessor result = JkTestProcessor.ofParent(this);
        final Path reportDir = testCompilation.layout.getOutputDir().resolve(this.reportDir);
        result
            .setRepoSetSupplier(() -> project.dependencyResolver.getRepos())
            .engineBehavior
                .setLegacyReportDir(reportDir)
                .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.ONE_LINE);
        return result;
    }

    private JkTestSelection<JkProjectTesting> defaultTestSelection() {
        return JkTestSelection.ofParent(this).addTestClassRoots(
                Paths.get(testCompilation.layout.getClassDir()));
    }

    private class JkProjectTestCompilation extends JkProjectCompilation<JkProjectTesting> {

        public JkProjectTestCompilation() {
            super(JkProjectTesting.this.project, JkProjectTesting.this);
        }

        @Override
        protected String purpose() {
            return "test";
        }

        @Override
        protected JkDependencySet baseDependencies() {
            JkDependencySet base = project.packaging.getRuntimeDependencies()
                    .merge(project.prodCompilation.getDependencies()).getResult();
            if (project.isIncludeTextAndLocalDependencies()) {
                base = project.textAndLocalDeps().getTest().and(base);
            }
            return base;
        }

        @Override
        protected JkCompileLayout initialLayout() {
            return super.initialLayout()
                    .setSourceMavenStyle(JkCompileLayout.Concern.TEST)
                    .setStandardOutputDirs(JkCompileLayout.Concern.TEST);
        }

        @Override
        protected JkPathSequence classpath() {
            return super.classpath()
                    .andPrepend(project.prodCompilation.layout.resolveClassDir());
        }
    }

}
