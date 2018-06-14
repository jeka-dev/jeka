package org.jerkar.api.java.junit;

import java.io.PrintStream;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsTime;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

class JUnitConsoleListener extends RunListener {

    private PrintStream out;

    private PrintStream err;

    private long startTs;

    @Override
    public void testStarted(Description description) throws Exception {
        JkLog.startTask("Running " + description.getClassName() + "." + description.getMethodName());
        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(JkLog.stream()));
        System.setErr(new PrintStream(JkLog.errorStream()));
        startTs = System.nanoTime();
    }

    @Override
    public void testFinished(Description description) throws Exception {
        JkLog.endTask(JkUtilsTime.durationInMillis(startTs));
        System.setOut(out);
        System.setErr(err);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        JkLog.info("- Test " + description.getDisplayName() + " ignored.");
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        failure.getException().printStackTrace(new PrintStream(JkLog.stream()));
    }

    @Override
    public void testFailure(Failure failure) {
        failure.getException().printStackTrace(new PrintStream(JkLog.stream()));
    }

}
