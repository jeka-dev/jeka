package dev.jeka.core.api.system;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * This decorator adds indentation for logs nested within a task.
 */
public final class JkIndentLogDecorator extends JkLog.JkLogDecorator {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    static final byte LINE_SEPARATOR = 10;

    static final byte[] MARGIN_UNIT = ("   ").getBytes(UTF8);

    private transient MarginStream marginOut;

    private transient MarginStream marginErr;

    private transient PrintStream out;

    private transient PrintStream err;

    @Override
    protected void init(PrintStream targetOut, PrintStream targetErr) {
        marginOut = new MarginStream(targetOut);
        marginErr = new MarginStream(targetErr);
        out = new PrintStream(marginOut);
        err = new PrintStream(marginErr);
    }

    private void readObject(ObjectInputStream objectInputStream) {
        marginOut = new MarginStream(System.out);
        marginErr = new MarginStream(System.err);
    }

    public void handle(JkLog.JkLogEvent event) {
        JkLog.Type logType = event.getType();
        PrintStream stream = out;
        if (logType == JkLog.Type.ERROR || logType == JkLog.Type.WARN) {
            out.flush();
            stream = err;
        } else {
            err.flush();
        }
        String message = event.getMessage();
        if (event.getType().isTraceWarnOrError()) {
            message = "[" + event.getType() + "] " + message;
        }
        if (logType == JkLog.Type.END_TASK) {
            // do nothing
        } else if (logType== JkLog.Type.START_TASK) {
            marginErr.flush();
            out.println(message);
            marginOut.notifyStart();
            marginErr.notifyStart();
            marginErr.mustPrintMargin = true;
        } else {
            stream.println(message);
        }
    }

    @Override
    public PrintStream getOut() {
        return out;
    }

    @Override
    public PrintStream getErr() {
        return err;
    }

    private static class MarginStream extends OutputStream {

        private final PrintStream delegate;

        private volatile boolean mustPrintMargin;

        private void notifyStart() {
            flush();
        }

        public MarginStream(PrintStream delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void write(int aByte) throws IOException {
            if (mustPrintMargin) {
                printMargin();
            }
            delegate.write(aByte);
            mustPrintMargin = (aByte == LINE_SEPARATOR);
        }

        void printMargin() throws IOException {
            Integer level = JkLog.getCurrentNestedLevel();
            for (int j = 0; j < level; j++) {
                delegate.write(MARGIN_UNIT);
            }
        }

        @Override
        public void flush() {
            delegate.flush();
        }

    }

}
