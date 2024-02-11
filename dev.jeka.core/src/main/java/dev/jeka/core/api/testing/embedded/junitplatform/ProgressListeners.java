package dev.jeka.core.api.testing.embedded.junitplatform;

import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsSystem;
import javafx.scene.Parent;
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
            case FULL: return new ProgressListeners.ConsoleProgressExecutionListener();
            case STEP: return new StepProgressExecutionListener();
            case SILENT: return new SilentProgressExecutionListener();
            default: return null;
        }
    }

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

    static class ConsoleProgressExecutionListener implements TestExecutionListener {

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(testIdentifier.getType().isTest()) {
                System.out.println(testIdentifier);
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

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            long testCount = testPlan.countTestIdentifiers(testIdentifier -> testIdentifier.getType().isTest());
            System.out.println("Launch " + testCount + " tests ");
            System.out.flush();
            silencer.silent(true);
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
            System.out.println();
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            char symbol = statusSymbol(testExecutionResult.getStatus());
            if(testIdentifier.getType().isTest()) {
                silencer.silent(false);
                System.out.print(symbol);
                System.out.flush();
                count++;
                if (count == 100) {
                    System.out.println();
                    count=0;
                }
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

        private long testCount;

        private TestPlan testPlan;

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            this.testPlan = testPlan;
            testCount = testPlan.countTestIdentifiers(testIdentifier -> testIdentifier.getType().isTest());
            System.out.println("Launch " + testCount + " tests ");
            String bootingLine = "Booting tests ...";
            System.out.print(bootingLine);
            charCount = bootingLine.length();
            System.out.flush();
            silencer.silent(true);
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            silencer.silent(false);
            System.out.println();
            System.out.flush();
        }

        @Override
        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            if (testIdentifier.isTest()) {
                index ++;
            } else {
                int children = testPlan.getDescendants(testIdentifier).size();
                index += children;
            }
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(testIdentifier.getType().isTest()) {
                index++;

                // Delete current line
                silencer.silent(false);
                System.out.print(JkUtilsString.repeat("\b", charCount));
                System.out.print(JkUtilsString.repeat(" ", charCount));
                System.out.print(JkUtilsString.repeat("\b", charCount));
                System.out.flush();

                // Print new line
                String line = line(testIdentifier);
                charCount = line.length();
                System.out.print(line);
                System.out.flush();
                silencer.silent(true);
            }
        }

        private String line(TestIdentifier testIdentifier) {
            int digitLenght =  Long.toString(testCount).length();
            return java.lang.String.format("Executing test %s/%s %s %s",
                    JkUtilsString.padStart(Integer.toString(index), digitLenght, '0'),
                    JkUtilsString.padStart(Long.toString(testCount), digitLenght, '0'),
                    bar(),
                    friendlyName(testIdentifier));
        }

        private String bar() {
            int count = (int) ((BAR_LENGTH * index) / testCount);
            count = Math.min(BAR_LENGTH, count);
            int spaceCount = BAR_LENGTH - count;
            return "[" + JkUtilsString.repeat("=", count) + JkUtilsString.repeat(" ", spaceCount) + "]";
        }

        private int testCountCharCount() {
            return Long.toString(testCount).length();
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

    private static String friendlyName(TestIdentifier testIdentifier) {
        Optional<UniqueId> parentUniqueId = testIdentifier.getParentIdObject();
        String prefix = "";
        if (parentUniqueId.isPresent()) {
            prefix = parentUniqueId.get().getLastSegment().getValue() + ".";
        }
        String candidate = prefix + testIdentifier.getDisplayName();
        return JkUtilsString.ellipse(candidate, 80);
    }
}
