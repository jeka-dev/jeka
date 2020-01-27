package dev.jeka.core.api.java.junit;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsTime;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.PrintStream;

class PrintConsoleTestListener extends RunListener {

    private PrintStream out;

    private PrintStream err;

    private long startTs;

    @Override
    public void testStarted(Description description) throws Exception {
        JkLog.startTask("Running " + description.getClassName() + "." + description.getMethodName());
        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(JkLog.getOutputStream()));
        System.setErr(new PrintStream(JkLog.getErrorStream()));
        startTs = System.nanoTime();
    }

    @Override
    public void testFinished(Description description) throws Exception {
        JkLog.endTask(("Done in " + JkUtilsTime.durationInMillis(startTs) + " milliseconds."));
        System.setOut(out);
        System.setErr(err);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        JkLog.info("- Test " + description.getDisplayName() + " ignored.");
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        failure.getException().printStackTrace(new PrintStream(JkLog.getOutputStream()));
    }

    @Override
    public void testFailure(Failure failure) {
        failure.getException().printStackTrace(new PrintStream(JkLog.getOutputStream()));
    }

}
