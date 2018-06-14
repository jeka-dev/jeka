package org.jerkar.api.java.junit;

import org.jerkar.api.utils.JkUtilsString;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.io.OutputStream;

class ProgressTestListener extends RunListener {

    private final OutputStream out;

    private String lastDisplay = "";

    ProgressTestListener(OutputStream out) {
        this.out = out;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        String erase = JkUtilsString.repeat("\b", lastDisplay.length());
        //out.flush();
        String display = description.getClassName() + "#" + description.getMethodName() + " ... " + JkUtilsString.repeat(" ", 150);
        out.write((erase + display).getBytes());
        out.flush();
        lastDisplay = display;
    }

    public void testRunFinished(Result result) throws Exception {
        String erase = JkUtilsString.repeat("\b", lastDisplay.length());
        out.write(erase.getBytes());
        out.flush();
        String eraseLine = JkUtilsString.repeat(" ", 300);
        out.write(eraseLine.getBytes());
        out.write(JkUtilsString.repeat("\b", 300).getBytes());
        out.flush();
    }



}
