package org.jerkar.api.java.junit;

import java.io.PrintStream;

import org.jerkar.api.system.JkLog;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

class JUnitConsoleListener extends RunListener {

    private PrintStream out;

    private PrintStream err;

    @Override
    public void testStarted(Description description) throws Exception {
        JkLog.start(this, "Running " + description.getClassName() + "." + description.getMethodName());
        out = System.out;
        err = System.err;
        System.setOut(JkLog.stream());
        System.setErr(JkLog.errorStream());
    }

    @Override
    public void testFinished(Description description) throws Exception {
        JkLog.end(this, "");
        System.setOut(out);
        System.setErr(err);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        JkLog.info(this,"- Test " + description.getDisplayName() + " ignored.");
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        failure.getException().printStackTrace(JkLog.stream());
    }

    @Override
    public void testFailure(Failure failure) {
        failure.getException().printStackTrace(JkLog.stream());
    }

}
