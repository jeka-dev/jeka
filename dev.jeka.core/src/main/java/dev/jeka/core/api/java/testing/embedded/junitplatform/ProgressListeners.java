package dev.jeka.core.api.java.testing.embedded.junitplatform;

import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import org.apache.ivy.util.StringUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

class ProgressListeners {

    static TestExecutionListener get(JkTestProcessor.JkProgressOutputStyle progressDisplayer) {
        if (progressDisplayer == null) {
            return null;
        }
        switch (progressDisplayer) {
            case DYNAMIC: return new ProgressListeners.DynamicProgressExecutionListener();
            case TREE: return new ProgressListeners.TreeProgressExecutionListener();
            case FULL: return new ProgressListeners.ConsoleProgressExecutionListener();
            case ONE_LINE: return new ProgressListeners.OneLineProgressExecutionListener();
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

    static class OneLineProgressExecutionListener implements TestExecutionListener {

        private final Silencer silencer = new Silencer();

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            long testCount = testPlan.countTestIdentifiers(testIdentifier -> testIdentifier.getType().isTest());
            System.out.print("Launch " + testCount + " tests ");
            System.out.flush();
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            System.out.println();
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(testIdentifier.getType().isTest()) {
                System.out.print(".");
                System.out.flush();
                silencer.silent(true);
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if(testIdentifier.getType().isTest()) {
                silencer.silent(false);
            }
        }
    }

    static class DynamicProgressExecutionListener implements TestExecutionListener {

        private static final int BAR_LENGTH = 50;

        private final Silencer silencer = new Silencer();

        int index;

        private int charCount;

        private long testCount;

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            testCount = testPlan.countTestIdentifiers(testIdentifier -> testIdentifier.getType().isTest());
            System.out.println("Launch " + testCount + " tests ");
            System.out.flush();
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            // Do nothing
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if(testIdentifier.getType().isTest()) {
                index++;
                String line = line(testIdentifier);
                charCount = line.length();
                System.out.print(line);
                System.out.flush();
                silencer.silent(true);
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if(testIdentifier.getType().isTest()) {
                silencer.silent(false);
                System.out.print(StringUtils.repeat("\b", charCount));
                System.out.print(StringUtils.repeat(" ", charCount));
                System.out.print(StringUtils.repeat("\b", charCount));
                System.out.flush();
            }
        }

        private String line(TestIdentifier testIdentifier) {
            int digitLenght =  Long.toString(testCount).length();
            return String.format("Executing test %s/%s %s %s",
                    JkUtilsString.padStart(Integer.toString(index), digitLenght, '0'),
                    JkUtilsString.padStart(Long.toString(testCount), digitLenght, '0'),
                    bar(),
                    testIdentifier.getParentId().orElse("") + "." + testIdentifier.getDisplayName());
        }

        private String bar() {
            int count = (int) ((BAR_LENGTH * index) / testCount);
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
}
