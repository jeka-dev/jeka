package dev.jeka.core.api.system;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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

    public void init(PrintStream targetOut, PrintStream targetErr) {
        marginOut = new MarginStream(targetOut);
        marginErr = new MarginStream(targetErr);
        out = new PrintStream(marginOut);
        err = new PrintStream(marginErr);
    }

    private void readObject(ObjectInputStream objectInputStream) {
        marginOut = new MarginStream(System.out);
        marginErr = new MarginStream(System.err);
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
        if (event.getType() == JkLog.Type.END_TASK) {
            marginStream.closingBrace = true;
            stream.print("}");
            if (event.getDurationMs() >= 0) {
                stream.print(" Done in ");
                stream.print(event.getDurationMs());
                stream.println(" milliseconds.");
            }
            stream.print(" ");
            stream.println(message);
            marginStream.closingBrace = false;
            marginStream.pendingStart = false;
        } else if (event.getType() == JkLog.Type.START_TASK) {
            stream.print(message);
            stream.print(" {");
            marginOut.notifyStart();
            marginErr.notifyStart();
        }  else {
            stream.println(message);
        }
    }

    private static class MarginStream extends OutputStream {

        private final PrintStream delegate;

        private int lastByte = LINE_SEPARATOR;  // Display margin at first use (relevant for ofSystem.err)

        private boolean pendingStart;

        private boolean closingBrace;

        private void notifyStart() {
            flush();
            pendingStart = true;
        }

        public MarginStream(PrintStream delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void write(int aByte) throws IOException {
            if (pendingStart & !closingBrace) {
                delegate.write(LINE_SEPARATOR);
                lastByte = LINE_SEPARATOR;
                pendingStart = false;
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
