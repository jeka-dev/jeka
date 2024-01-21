package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsString;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * This decorator shows minimalist task decoration...
 */
public final class JkFlatLogDecorator extends JkLog.JkLogDecorator {

    private static final String HEADER_LINE = JkUtilsString.repeat("=", 120);

    private transient PrintStream out;

    private transient PrintStream err;

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
            if (!event.getMessage().isEmpty()) {
                err.flush();
                out.println(event.getMessage());
            }
            // Do nothing
        } else if (logType == JkLog.Type.START_TASK) {
            err.flush();
            out.print(message);
            out.println(" ...");
        } else {
            stream.println(message);
        }
    }











}
