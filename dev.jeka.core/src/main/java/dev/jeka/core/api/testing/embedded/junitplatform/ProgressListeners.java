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
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.PrintStream;
import java.util.Optional;

class ProgressListeners {

    static TestExecutionListener get(JkTestProcessor.JkProgressOutputStyle progressDisplayer) {
        if (progressDisplayer == null) {
            return null;
        }
        switch (progressDisplayer) {
            case BAR: return new ProgressBarExecutionListener();
            case TREE: return new ProgressListeners.TreeProgressExecutionListener();
            case PLAIN: return new ProgressListeners.ConsoleProgressExecutionListener();
            case STEP: return new StepProgressExecutionListener();
            case SILENT: return new SilentProgressExecutionListener();
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
            silencer.silent(true);
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
        }

    }

    static class TreeProgressExecutionListener implements TestExecutionListener {

        private final Silencer silencer = new Silencer();

        private int nestedLevel;

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            System.out.println();
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            System.out.print(JkUtilsString.repeat("  ", nestedLevel) + testIdentifier.getDisplayName());
            if (testIdentifier.getType().isContainer()) {
                System.out.println();
                nestedLevel ++;
            }
            if(testIdentifier.getType().isTest()) {
                silencer.silent(true);
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.getType().isTest()) {
                silencer.silent(false);
                System.out.println(" : " + testExecutionResult.getStatus());
            }
            if(testIdentifier.getType().isContainer()) {
                nestedLevel --;
            }
        }
    }

    /**
     * Print test under execution on console without silencing test output.
     */
    static class ConsoleProgressExecutionListener implements TestExecutionListener {

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(testIdentifier.getType().isTest()) {
                System.out.println(friendlyName(testIdentifier));
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.getType().isTest()) {
                System.out.println(testExecutionResult.getStatus());
                System.out.println();
            }
        }

    }

    /**
     * Create a progress bar that print a test status char at each execution step.
     * It reaches a maximum of 100 characters before creating a new line.
     */
    static class StepProgressExecutionListener implements TestExecutionListener {

        private final Silencer silencer = new Silencer();

        int count = 0;

        int charCount;

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            long testCount = testPlan.countTestIdentifiers(testIdentifier -> testIdentifier.getType().isContainer());
            System.out.println("Found " + testCount + " test containers ");
            String bootingLine = "Booting tests ...";
            System.out.print(bootingLine);
            charCount = bootingLine.length();
            System.out.flush();
            silencer.silent(true);
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
            deleteLastChars(charCount);
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            char symbol = statusSymbol(testExecutionResult.getStatus());
            if(testIdentifier.getType().isContainer()) {
                silencer.silent(false);
                System.out.print(symbol);
                count++;
                charCount++;
                if (count == 100) {
                    System.out.println();
                    count=0;
                    charCount++;
                }
                System.out.flush();
                silencer.silent(true);
            }
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

            testContainerCount = testPlan.countTestIdentifiers(TestIdentifier::isContainer);

            System.out.println("Found " + testContainerCount + " test containers ");
            String bootingLine = "Booting tests ...";
            System.out.print(bootingLine);
            charCount = bootingLine.length();
            System.out.flush();
            silencer.silent(true);
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
            System.out.flush();
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(testIdentifier.getType().isContainer()) {
                index++;

                deleteCurrentLine();  // may need to delete booting line

                // Print new line
                silencer.silent(false);
                String line = line(testIdentifier);
                charCount = line.length();
                System.out.print(line);
                System.out.flush();
                silencer.silent(true);
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.getType().isContainer()) {
               deleteCurrentLine();
            }
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
            return java.lang.String.format("Executing test %s/%s %s %s",
                    JkUtilsString.padStart(Integer.toString(index), digitLenght, '0'),
                    JkUtilsString.padStart(Long.toString(testContainerCount), digitLenght, '0'),
                    bar(),
                    friendlyName(testIdentifier));
        }

        private String bar() {
            int count = (int) ((BAR_LENGTH * index) / Math.max(testContainerCount, 1));
            count = Math.min(BAR_LENGTH, count);
            int spaceCount = BAR_LENGTH - count;
            return "[" + JkUtilsString.repeat("=", count) + JkUtilsString.repeat(" ", spaceCount) + "]";
        }

        private int testCountCharCount() {
            return Long.toString(testContainerCount).length();
        }

    }

    static class Silencer {

        private static final PrintStream standardOutputStream = System.out;

        private static final PrintStream standardErrStream = System.err;

        public void silent(boolean silent) {
            if (silent) {
                System.out.flush();
                System.err.flush();
                System.setOut(JkUtilsIO.nopPrintStream());
                System.setErr(JkUtilsIO.nopPrintStream());
            } else {
                System.setOut(standardOutputStream);
                System.setErr(standardErrStream);
            }
        }
    }

    private static void deleteLastChars(int charCount) {
        System.out.print(JkUtilsString.repeat("\b", charCount));
        System.out.print(JkUtilsString.repeat(" ", charCount));
        System.out.print(JkUtilsString.repeat("\b", charCount));
    }

    private static char statusSymbol(TestExecutionResult.Status status) {
        if (JkUtilsSystem.CONSOLE == null) {
            return '.';
        }
        if (status == TestExecutionResult.Status.ABORTED) {
            return '-';
        }
        if (status == TestExecutionResult.Status.FAILED) {
            return '✗';
        }
        return '✓';
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
}
