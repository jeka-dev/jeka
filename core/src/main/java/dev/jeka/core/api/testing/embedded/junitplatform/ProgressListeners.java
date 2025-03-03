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

package dev.jeka.core.api.testing.embedded.junitplatform;

import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.api.utils.JkUtilsTime;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.PrintStream;
import java.util.Optional;

/*
 * Progress listeners for test execution.
 */
class ProgressListeners {

    static TestExecutionListener get(JkTestProcessor.JkProgressStyle progressDisplayer) {
        if (progressDisplayer == null) {
            return null;
        }
        switch (progressDisplayer) {
            case BAR: return new ProgressBarExecutionListener();
            case FULL: return new MavenLikeProgressExecutionListener(false);
            case PLAIN: return new MavenLikeProgressExecutionListener(true);
            case STEP: return new StepProgressExecutionListener();
            case MUTE: return new SilentProgressExecutionListener();
            default: return null;
        }
    }

    /**e.
     * Execute test plan silently by silencing the system output and error streams.
     */
    static class SilentProgressExecutionListener implements TestExecutionListener {

        private final Silencer silencer = new Silencer();

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            printTestContainerCount(testPlan);
            silencer.silent(true);
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
        }

    }

    /**
     * Print test under execution on console without silencing test output.
     */
    static class MavenLikeProgressExecutionListener implements TestExecutionListener {

        private final Silencer silencer;

        private long startTime;

        private int testCount;

        private int failureCount;

        private int skippedCount;

        MavenLikeProgressExecutionListener(boolean muteTestStdout) {
            this.silencer = muteTestStdout ? new Silencer() : Silencer.NO_OP;
        }

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            printTestContainerCount(testPlan);
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(mustShow(testIdentifier)) {
                silencer.silent(false);
                System.out.println("Running " + className(testIdentifier));
                silencer.silent(true);
                startTime = System.nanoTime();
                testCount = 0;
                failureCount = 0;
                skippedCount = 0;
            }
        }

        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            if (testIdentifier.getType().isTest()) {
                skippedCount++;
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (mustShow(testIdentifier)) {
                String status = failureCount > 0 ? "Fail" : "Success";
                long duration = JkUtilsTime.durationInMillis(startTime);
                String elapsed = JkUtilsTime.formatMillis(duration);
                String message = String.format("%s, run: %s, Failure: %s, Skipped: %s, Time elapsed: %s -- in %s",
                        status,
                        testCount,
                        failureCount,
                        skippedCount,
                        elapsed,
                        JkUtilsString.removePackagePrefix(className(testIdentifier)));
                this.silencer.silent(false);
                System.out.println("-> " + message);
                this.silencer.silent(true);

            }
            if (testIdentifier.getType().isTest()) {
                testCount++;
                if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                    failureCount++;
                }
            }
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
        }

        private static boolean mustShow(TestIdentifier testIdentifier) {
            return isClassContainer(testIdentifier);
        }

    }

    /**
     * Create a progress bar that print a test status char at each execution step.
     * It reaches a maximum of 100 characters before creating a new line.
     */
    static class StepProgressExecutionListener implements TestExecutionListener {

        private static final int DOT_COUNT_PER_LINE = 100;

        private final Silencer silencer = new Silencer();

        private int bootingCharCount;

        private int dotInCurrentRowCount;

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            printTestContainerCount(testPlan);
            silencer.silent(true);
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(mustShow(testIdentifier)) {
                if (bootingCharCount > 0) {
                    silencer.silent(false);
                    deleteLastChars(bootingCharCount);
                    silencer.silent(true);
                    bootingCharCount = 0;
                }
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (mustShow(testIdentifier)) {
                printProgress(testExecutionResult.getStatus());
            }
        }

        @Override
        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            if(mustShow(testIdentifier)) {
                printProgress(TestExecutionResult.Status.ABORTED);
            }
        }

        private void printProgress(TestExecutionResult.Status status) {
            String symbol = statusSymbol(status);
            this.silencer.silent(false);
            if (dotInCurrentRowCount >= DOT_COUNT_PER_LINE) {
                System.out.println();
                dotInCurrentRowCount = 0;
            }
            System.out.print(symbol);
            dotInCurrentRowCount++;
            this.silencer.silent(true);
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
            System.out.println();
        }

        private static boolean mustShow(TestIdentifier testIdentifier) {
            return isClassContainer(testIdentifier);
        }
    }

    /**
     * A progress bar that display the current executed test.
     */
    static class ProgressBarExecutionListener implements TestExecutionListener {

        private static final int BAR_LENGTH = 50;

        private final Silencer silencer = new Silencer();

        int index;

        private int charCount;

        private long testContainerCount;

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            testContainerCount = printTestContainerCount(testPlan);
            String bootingLine = "Booting tests ...";
            System.out.print(bootingLine);
            charCount = bootingLine.length();
            System.out.flush();
            silencer.silent(true);
        }


        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(mustShow(testIdentifier)) {

                deleteCurrentLine();  // may need to delete booting line

                // Print new line
                silencer.silent(false);
                String line = line(testIdentifier);
                charCount = line.length();
                System.out.print(line);
                System.out.flush();
                silencer.silent(true);
            }
            if (testIdentifier.getType().isContainer()) {
                index++;
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (mustShow(testIdentifier)) {
               deleteCurrentLine();
            }
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
        }

        private void deleteCurrentLine() {
            silencer.silent(false);
            deleteLastChars(charCount);
            System.out.flush();
            charCount = 0;
            silencer.silent(true);
        }

        private String line(TestIdentifier testIdentifier) {
            int digitLenght =  Long.toString(testContainerCount).length();
            return String.format("Executing test %s/%s %s %s",
                    JkUtilsString.padStart(Integer.toString(index), digitLenght, '0'),
                    JkUtilsString.padStart(Long.toString(testContainerCount), digitLenght, '0'),
                    bar(),
                    JkUtilsString.substringAfterLast(friendlyName(testIdentifier), "."));
        }

        private String bar() {
            int count = (int) ((BAR_LENGTH * index) / Math.max(testContainerCount, 1));
            count = Math.min(BAR_LENGTH, count);
            int spaceCount = BAR_LENGTH - count;
            return "[" + JkUtilsString.repeat("=", count) + JkUtilsString.repeat(" ", spaceCount) + "]";
        }

        private static boolean mustShow(TestIdentifier testIdentifier) {
            return isClassContainer(testIdentifier);
        }

    }

    static class Silencer {

        private static final PrintStream standardOutputStream = System.out;

        private static final PrintStream standardErrStream = System.err;

        static final Silencer NO_OP = new NoOp();

        public void silent(boolean silent) {
            if (silent) {
                System.out.flush();
                System.err.flush();
                System.setOut(JkUtilsIO.nopPrintStream());
                System.setErr(JkUtilsIO.nopPrintStream());
            } else {
                System.out.flush();  // before to recover from silence, we need to purge the stream
                System.err.flush();
                System.setOut(standardOutputStream);
                System.setErr(standardErrStream);
            }
        }

        private static class NoOp extends Silencer {

            @Override
            public void silent(boolean silent) {
                // Do nothing
            }
        }
    }

    private static void deleteLastChars(int charCount) {
        System.out.print(JkUtilsString.repeat("\b", charCount));
        System.out.print(JkUtilsString.repeat(" ", charCount));
        System.out.print(JkUtilsString.repeat("\b", charCount));
    }

    private static String statusSymbol(TestExecutionResult.Status status) {
        if (JkUtilsSystem.CONSOLE == null) {
            if (status == TestExecutionResult.Status.ABORTED) {
                return "o";
            }
            if (status == TestExecutionResult.Status.FAILED) {
                return "x";
            }
            return ".";
        }
        if (status == TestExecutionResult.Status.ABORTED) {
            return "-";
        }
        if (status == TestExecutionResult.Status.FAILED) {
            return "✗";
        }
        return "✓";
    }

    static String friendlyName(TestIdentifier testIdentifier) {
        Optional<UniqueId> parentUniqueId = testIdentifier.getParentIdObject();
        String prefix = "";
        if (parentUniqueId.isPresent()) {
            prefix = parentUniqueId.get().getLastSegment().getValue() + ".";
        }
        String candidate = prefix + testIdentifier.getDisplayName();
        return JkUtilsString.ellipse(candidate, 80);
    }

    private static boolean isClassContainer(TestIdentifier testIdentifier) {
        return testIdentifier.getType() == TestDescriptor.Type.CONTAINER
                && testIdentifier.getSource().isPresent()
                && (testIdentifier.getSource().get() instanceof ClassSource);
    }

    private static String className(TestIdentifier testIdentifier) {
        ClassSource classSource = (ClassSource) testIdentifier.getSource().get();
        return classSource.getClassName();
    }

    // There is no possibility to use ANSI color here, has the test process
    // does not inherit io
    private static String friendlyStatus(TestExecutionResult.Status status) {
        return JkUtilsString.capitalize(status.toString().toLowerCase());
    }

    private static long printTestContainerCount(TestPlan testPlan) {
        long count = testPlan.countTestIdentifiers(ProgressListeners::isClassContainer);
        System.out.println("Found " + count + " test containers.");
        return count;
    }

}
