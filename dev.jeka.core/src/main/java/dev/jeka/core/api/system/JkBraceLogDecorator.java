package dev.jeka.core.api.system;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.jeka.core.api.system.JkIndentLogDecorator.LINE_SEPARATOR;
import static dev.jeka.core.api.system.JkIndentLogDecorator.MARGIN_UNIT;

/**
 * This decorator add curly brace around task to delimit task. It also add an indication of
 * processing task for each task.
 */
public final class JkBraceLogDecorator extends JkLog.JkLogDecorator {

    private static final int MARGIN_UNIT_LENGTH = new String(MARGIN_UNIT, StandardCharsets.UTF_8).length();

    private static int maxLength = -1;

    private transient MarginStream marginOut;

    private transient MarginStream marginErr;

    private transient PrintStream out;

    private transient PrintStream err;

    private AtomicBoolean pendingStart = new AtomicBoolean(false);

    public void init(PrintStream targetOut, PrintStream targetErr) {
        marginOut = new MarginStream(targetOut, pendingStart);
        marginErr = new MarginStream(targetOut, pendingStart);   // Cause erratic output if logged on separate streams
        out = new PrintStream(marginOut);
        err = new PrintStream(marginErr);
    }

    private void readObject(ObjectInputStream objectInputStream) {
        marginOut = new MarginStream(System.out, pendingStart);
        marginErr = new MarginStream(System.out, pendingStart);
    }

    public static void setMaxLength(int maxLength) {
        JkBraceLogDecorator.maxLength = maxLength;
    }

    public PrintStream getOut() {
        return out;
    }

    public PrintStream getErr() {
        return err;
    }

    public void handle(JkLog.JkLogEvent event) {
        final MarginStream marginStream = (event.getType() == JkLog.Type.ERROR) ? marginErr : marginOut;
        PrintStream stream = out;
        String message = event.getMessage();
        if (event.getType() == JkLog.Type.ERROR || event.getType() == JkLog.Type.WARN) {
            out.flush();
            stream = err;
        }
        if (event.getType().isTraceWarnOrError()) {
            message = "[" + event.getType() + "] " + message;
        }
        if (event.getType() == JkLog.Type.START_TASK) {
            stream.print(message);
            stream.print(" {");
            marginOut.notifyStart();
            marginErr.notifyStart();
        } else if (event.getType() == JkLog.Type.END_TASK) {
            marginStream.closingBrace = true;
            stream.print("}");
            stream.print(" Done in ");
            stream.print(event.getDurationMs());
            stream.print(" milliseconds. ");
            stream.println(message);
            marginStream.closingBrace = false;
            pendingStart.set(false);
        } else {
            stream.println(message);
        }
    }

    private static class MarginStream extends OutputStream {

        private final PrintStream delegate;

        private int lastByte = LINE_SEPARATOR;  // Display margin at first use (relevant for ofSystem.err)

        private final AtomicBoolean pendingStart;

        private boolean closingBrace;

        private void notifyStart() {
            flush();
            pendingStart.set(true);
        }
        public MarginStream(PrintStream delegate, AtomicBoolean pendingStart) {
            super();
            this.delegate = delegate;
            this.pendingStart = pendingStart;
        }

        @Override
        public void write(int aByte) throws IOException {
            if (pendingStart.get() & !closingBrace) {
                delegate.write(LINE_SEPARATOR);
                lastByte = LINE_SEPARATOR;
                pendingStart.set(false);
            }
            if (lastByte == LINE_SEPARATOR) {
                Integer level = JkLog.getCurrentNestedLevel();
                for (int j = 0; j < level; j++) {
                    delegate.write(MARGIN_UNIT);
                }
            }
            delegate.write(aByte);
            lastByte = aByte;
        }

        @Override
        public void flush() {
            delegate.flush();
        }

    }

}
