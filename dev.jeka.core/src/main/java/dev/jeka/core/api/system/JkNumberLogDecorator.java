package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsString;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * This decorator adds task numbering
 * s: 1
 * s: 1.1
 * s: 1.1.1
 * e:
 * s: 1.1.2
 */
public final class JkNumberLogDecorator extends JkLog.JkLogDecorator {

    private static final String HEADER_LINE = JkUtilsString.repeat("=", 120);

    private transient PrintStream out;

    private transient PrintStream err;

    private LinkedList<Integer> stack = new LinkedList<>();

    private int next = 1;

    @Override
    protected void init(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    PrintStream getOut() {
        return out;
    }

    @Override
    PrintStream getErr() {
        return err;
    }


    public void handle(JkLog.JkLogEvent event) {
        JkLog.Type logType = event.getType();
        PrintStream out = getOut();
        PrintStream err = getErr();
        PrintStream stream = getOut();
        if (logType == JkLog.Type.ERROR || logType == JkLog.Type.WARN) {
            out.flush();
            stream = err;
        } else {
            err.flush();
        }
        String message = event.getMessage();
        if (logType == JkLog.Type.END_TASK) {
            err.flush();
            String title = String.format("[%s End] %s: Done in %d milliseconds. ",
                    toNumber(), message, event.getDurationMs());
            out.println(title);
            endTask();

        } else if (logType == JkLog.Type.START_TASK) {
            startNewTask();
            String title = String.format("[%s Start] %s", toNumber(), message);
            err.flush();
            out.println(title);
        } else {
            stream.println(message);
        }
    }

    private void startNewTask() {
        stack.addLast(next);
    }

    private void endTask() {
        int last = stack.pollLast();
        next = last + 1;
    }

    private String toNumber() {
        return stack.stream().map(i -> Integer.toString(i)).collect(Collectors.joining("."));
    }

    public void printHeader(String title) {
        out.println(HEADER_LINE);
        out.println(title);
        out.println(HEADER_LINE);
        out.flush();
    }







}
