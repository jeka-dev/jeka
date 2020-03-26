package dev.jeka.core.api.java.junit.embedded.junitplatform;

import dev.jeka.core.api.java.junit.JkTestProcessor;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.PrintStream;

class ProgressListeners {

    static TestExecutionListener get(JkTestProcessor.JkProgressOutputStyle progressDisplayer) {
        if (progressDisplayer == null) {
            return null;
        }
        switch (progressDisplayer) {
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
        public void testPlanExecutionStarted(TestPlan testPlan) {
            long testCount = testPlan.countTestIdentifiers(testIdentifier -> testIdentifier.getType().isTest());
        }

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
            System.out.print("Launching " + testCount + " tests ");
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            System.out.println("");
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

    static class Silencer {

        private final PrintStream standardOutputStream = System.out;

        private final PrintStream standardErrStream = System.err;

        public void silent(boolean silent) {
            if (silent) {
                System.setOut(JkUtilsIO.nopPrintStream());
                System.setErr(JkUtilsIO.nopPrintStream());
            } else {
                System.setOut(standardOutputStream);
                System.setErr(standardErrStream);
            }
        }
    }
}
