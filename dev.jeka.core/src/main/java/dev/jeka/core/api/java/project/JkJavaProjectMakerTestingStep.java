package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.testplatform.JkTestProcessor;
import dev.jeka.core.api.java.testplatform.JkTestResult;
import dev.jeka.core.api.java.testplatform.JkTestSelection;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Handles project testing step. This involve both test compilation and run.
 * Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompiler}, {@link JkJavaCompileSpec} for test compilation and
 * {@link JkTestProcessor}, {@link JkTestSelection} for test run.
 */
public class JkJavaProjectMakerTestingStep {

    private final JkJavaProjectMaker maker;

    private JkTestCompile testCompileStep;

    public final JkRunnables afterTest = JkRunnables.of(() -> {});

    private JkTestProcessor testProcessor;

    private JkTestSelection testSelection;

    private boolean done;

    private boolean skipped;

    private boolean breakOnFailures = true;

    /**
     * For parent chaining
     */
    public final JkJavaProjectMaker.JkSteps _;

    private JkJavaProjectMakerTestingStep(JkJavaProjectMaker maker) {
        this.maker = maker;
        this._ = maker.getSteps();
    }

    static JkJavaProjectMakerTestingStep of(JkJavaProjectMaker maker) {
        JkJavaProjectMakerTestingStep result = new JkJavaProjectMakerTestingStep(maker);
        result.testCompileStep = JkTestCompile.of(result);
        result.testProcessor = result.defaultTestProcessor();
        result.testSelection = result.defaultTestSelection();
        return result;
    }

    /**
     * Returns tests to be run. The returned instance is mutable so users can modify it
     * from this method return.
     */
    public JkTestSelection<JkJavaProjectMakerTestingStep> getTestSelection() {
        return testSelection;
    }

    /**
     * Returns processor running the tests. The returned instance is mutable so users can modify it
     * from this method return.
     */
    public JkTestProcessor<JkJavaProjectMakerTestingStep> getTestProcessor() {
        return testProcessor;
    }

    /**
     * Returns the compilation step for the test part.
     */
    public JkTestCompile getTestCompileStep() {
        return testCompileStep;
    }

    /**
     * Returns the classpath to run the test. It consists in test classes + prod classes +
     * dependencies involved in TEST scope.
     */
    public JkClasspath getTestClasspath() {
        return JkClasspath.of(maker.getOutLayout().getTestClassDir())
                .and(maker.getOutLayout().getClassDir())
                .and(maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_TEST));
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

    public JkJavaProjectMakerTestingStep setBreakOnFailures(boolean breakOnFailures) {
        this.breakOnFailures = breakOnFailures;
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
        this.maker.getSteps().getCompilation().runIfNecessary();
        this.testCompileStep.run();
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

    private JkTestProcessor defaultTestProcessor() {
        JkTestProcessor result = JkTestProcessor.of(this);
        final Path reportDir = maker.getOutLayout().getTestReportDir().resolve("junit");
        result.getEngineBehavior()
                .setLegacyReportDir(reportDir)
                .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.ONE_LINE);
        return result;
    }

    private JkTestSelection defaultTestSelection() {
        return JkTestSelection.of(this).addTestClassRoots(maker.getOutLayout().getTestClassDir());
    }

    public static class JkTestCompile extends JkJavaProjectMakerCompilationStep<JkTestCompile> {

        private Consumer<JkJavaCompileSpec> compileSpecConfigurer = spec -> {};

        private Consumer<JkResourceProcessor> resourceProcessorConfigurer = processor -> {};

        /**
         * For parent chaining
         */
        public final JkJavaProjectMakerTestingStep _;

        private JkTestCompile(JkJavaProjectMakerTestingStep parent) {
            super(parent.maker);
            _ = parent;
        }

        static JkTestCompile of(JkJavaProjectMakerTestingStep parent) {
            JkTestCompile result = new JkTestCompile(parent);
            result.init();
            return result;
        }

        /**
         * Sets a configurer for the compile spec. This configurer will apply on a copy of
         * the compile spec of the compile step.
         */
        public JkTestCompile setCompileSpecConfigurer(Consumer<JkJavaCompileSpec> compileSpecConfigurer) {
            this.compileSpecConfigurer = compileSpecConfigurer;
            return this;
        }

        /**
         * Sets a configurer for the resource processor. This configurer will apply on a copy of
         * the resource processor of compile step.
         */
        public JkTestCompile setResourceProcessorConfigurer(Consumer<JkResourceProcessor> resourceProcessorConfigurer) {
            this.resourceProcessorConfigurer = resourceProcessorConfigurer;
            return this;
        }

        @Override
        protected JkJavaCompileSpec getCompileSpec() {
            JkJavaProjectMaker maker = _.maker;
            final JkPathSequence classpath = maker.fetchDependenciesFor(JkJavaDepScopes.SCOPES_FOR_TEST)
                    .andPrepending(maker.getOutLayout().getClassDir());
            JkJavaCompileSpec compileStepSpec = maker.getSteps().getCompilation().getCompileSpec();
            JkJavaCompileSpec compileSpec = JkJavaCompileSpec.of(this)
                    .setEncoding(compileStepSpec.getEncoding())
                    .setSourceVersion(compileStepSpec.getSourceVersion())
                    .setTargetVersion(compileStepSpec.getTargetVersion())
                    .setClasspath(classpath)
                    .addSources(maker.project.getSourceLayout().getTests())
                    .setOutputDir(maker.getOutLayout().getTestClassDir());
            compileSpecConfigurer.accept(compileSpec);
            return compileSpec;
        }

        @Override
        protected JkResourceProcessor getResourceProcessor() {
            JkJavaProjectMaker maker = _.maker;
            JkResourceProcessor resourceProcessor = JkResourceProcessor.of(this)
                    .setResources(maker.project.getSourceLayout().getTestResources())
                    .setInterpolationCharset(maker.getSteps().getCompilation()
                            .getResourceProcessor().getInterpolationCharset());
            resourceProcessorConfigurer.accept(resourceProcessor);
            return resourceProcessor;
        }

        @Override
        protected String getScope() {
            return "test code";
        }

    }


}
