package org.jerkar.api.java.junit;

import java.io.PrintStream;

import org.jerkar.api.system.JkEvent;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

class JUnitConsoleListener extends RunListener {

    private PrintStream out;

    private PrintStream err;

    @Override
    public void testStarted(Description description) throws Exception {
        JkEvent.start(this, "Running " + description.getClassName() + "." + description.getMethodName());
        out = System.out;
        err = System.err;
        System.setOut(JkEvent.stream());
        System.setErr(JkEvent.errorStream());
    }

    @Override
    public void testFinished(Description description) throws Exception {
        JkEvent.end(this, "");
        System.setOut(out);
        System.setErr(err);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        JkEvent.info(this,"- Test " + description.getDisplayName() + " ignored.");
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        failure.getException().printStackTrace(JkEvent.stream());
    }

    @Override
    public void testFailure(Failure failure) {
        failure.getException().printStackTrace(JkEvent.stream());
    }

}
