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

package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestResult;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Handles project testing step. This involves both test compilation and run.
 * Users can configure inner phases by chaining runnables.
 * They also can modify {@link JkJavaCompilerToolChain}, {@link JkJavaCompileSpec} for test compilation and
 * {@link JkTestProcessor}, {@link JkTestSelection} for test run.
 */
public class JkProjectTesting {

    private final JkProject project;

    public final JkProjectCompilation compilation;

    /**
     * The processor running the tests.
     */
    public final JkTestProcessor testProcessor;

    /**
     * Tests to be run.
     */
    public final JkTestSelection testSelection;

    public final JkRunnables postActions = JkRunnables.of().setLogTasks(true);

    // relative path from output dir
    private String reportDir = "test-report";

    private boolean done;

    private boolean skipped;

    private boolean breakOnFailures = true;

    JkProjectTesting(JkProject project) {
        this.project = project;
        compilation = new JkProjectTestCompilation();
        testProcessor = createDefaultTestProcessor();
        testSelection = createDefaultTestSelection();
    }

    /**
     * Applies the given consumer to the current instance of JkProjectTesting.
     * This method allows you to modify the JkProjectTesting object using the provided consumer.
     */
    public JkProjectTesting apply(Consumer<JkProjectTesting> consumer) {
        consumer.accept(this);
        return this;
    }


    /**
     * Returns the classpath to run the test. It consists in test classes + prod classes +
     * dependencies defined in testing/compile.
     */
    public JkPathSequence getTestClasspath() {
        JkProjectCompilation prodCompilation = project.compilation;
        return JkPathSequence.of()
                .and(compilation.initialLayout().resolveClassDir())

                // The production classes must set be before test dependencies
                // As if we get in trouble if a test dependencies grab a transitive
                // dependency which is a previous version of the project we are building.
                // This is the case for apache.commons.lang
                .and(prodCompilation.layout.resolveClassDir())
                .and(compilation.resolveDependenciesAsFiles())
                .and(project.packaging.resolveRuntimeDependenciesAsFiles())
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

    /**
     * Sets whether the execution of tests should break (throw an IllegalArgumentException)
     * if there are test failures.
     */
    public JkProjectTesting setBreakOnFailures(boolean breakOnFailures) {
        this.breakOnFailures = breakOnFailures;
        return this;
    }

    /**
     * Returns the directory path where the test reports are stored.
     */
    public Path getReportDir() {
        return project.getOutputDir().resolve(reportDir);
    }

    /**
     * Sets the directory path where the test reports are stored.
     */
    public JkProjectTesting setReportDir(String reportDir) {
        this.reportDir = reportDir;
        return this;
    }

    /**
     * Performs entire test phase, including : <ul>
     * <li>compile regular code if needed</li>
     * <li>perform pre test tasks if present</li>
     * <li>compile test code and process test resources</li>
     * <li>execute compiled tests</li>
     * <li>execute post tesks if present</li>
     * </ul>
     */
    public void run() {
        JkLog.startTask("process-tests");
        if (skipped) {
            JkLog.info("Skipped");
            JkLog.endTask();
            return;
        }
        if (!project.compilation.isDone()) {
            JkLog.startTask("process-production-code");     // Make explicit task for clearer output
            this.project.compilation.runIfNeeded();
            JkLog.endTask();
        }
        this.compilation.run();
        if (!JkPathTree.of(this.compilation.layout.resolveClassDir()).containFiles()) {
            JkLog.endTask("No tests to execute.");
            return;
        }
        executeWithTestProcessor();
        postActions.run();
        JkLog.endTask();
    }

    /**
     * As #run, but performs only if not already done.
     */
    public void runIfNeeded() {
        if (done) {
            JkLog.verbose("Tests has already been performed. Won't do it again.");
        } else {
            run();
            done = true;
        }
    }

    /**
     * Creates a default test selection for the current project.
     * The default test selection includes the compiled test classes
     */
    public JkTestSelection createDefaultTestSelection() {
        return JkTestSelection.of()
                .addTestClassRoots(compilation.layout.resolveClassDir())
                .setRootResolver(project::getBaseDir);
    }

    /**
     * Creates a default test processor for running tests.
     *
     * @return a default {@link JkTestProcessor} object.
     */
    public JkTestProcessor createDefaultTestProcessor() {
        JkTestProcessor result = JkTestProcessor.of();
        final Path reportDir = compilation.layout.getOutputDir().resolve(this.reportDir);
        result
                .setRepoSetSupplier(() -> project.dependencyResolver.getRepos()) // cannot use lambda
                .engineBehavior
                .setLegacyReportDir(reportDir)
                .setProgressDisplayer(defaultProgressStyle());
        return result;
    }

    private void executeWithTestProcessor() {
        testSelection.resolveTestRootClasses();
        JkTestResult result = testProcessor.launch(getTestClasspath(), testSelection);
        if (breakOnFailures) {
            result.assertSuccess();
        }
    }

    private static JkTestProcessor.JkProgressStyle defaultProgressStyle() {
        if (JkLog.isDebug()) {
            return JkTestProcessor.JkProgressStyle.FULL;
        }
        if (!JkLog.isAnimationAccepted()) {
            return JkTestProcessor.JkProgressStyle.STEP;
        }
        return JkUtilsSystem.CONSOLE == null ? JkTestProcessor.JkProgressStyle.STEP :
                JkTestProcessor.JkProgressStyle.BAR;
    }

    private class JkProjectTestCompilation extends JkProjectCompilation {

        public JkProjectTestCompilation() {
            super(JkProjectTesting.this.project);
        }

        @Override
        protected String purpose() {
            return "tests";
        }

        @Override
        protected JkDependencySet baseDependencies() {
            JkDependencySet base = project.packaging.runtimeDependencies.get()
                    .merge(project.compilation.dependencies.get()).getResult();
            if (project.isIncludeTextAndLocalDependencies()) {
                base = project.textAndLocalDeps().getTest().and(base);
            }
            return base.withoutDuplicate();
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
                    .andPrepend(project.compilation.layout.resolveClassDir());
        }
    }

}
