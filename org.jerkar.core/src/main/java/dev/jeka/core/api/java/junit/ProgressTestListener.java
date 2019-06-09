package dev.jeka.core.api.java.junit;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.io.OutputStream;

class ProgressTestListener extends RunListener {

    private final OutputStream out;

    ProgressTestListener(OutputStream out) {
        this.out = out;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        out.write(".".getBytes());
        out.flush();
    }

    public void testRunFinished(Result result) throws Exception {
        out.write("\n".getBytes());
    }



}
