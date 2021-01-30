package dev.jeka.core.api.system;

import java.io.*;
import java.nio.charset.Charset;

public final class JkIndentConsoleLogConsumer implements JkLog.JkEventLogConsumer, Serializable {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final byte LINE_SEPARATOR = 10;

    private static final byte[] MARGIN_UNIT = ("   ").getBytes(UTF8);

    private transient MarginStream marginOut;

    private transient MarginStream marginErr;

    private transient PrintStream formerOut;

    private transient PrintStream formerErr;

    private transient PrintStream out;

    private transient PrintStream err;

    public void init() {
        formerOut = System.out;
        formerErr = System.err;
        marginOut = new MarginStream(formerOut);
        marginErr = new MarginStream(formerErr);
        out = new PrintStream(marginOut);
        err = new PrintStream(marginErr);
        System.setOut(out);
        System.setErr(err);
    }

    public void restore() {
        if (formerOut != null && (out == System.out)) {
            System.setOut(formerOut);
        }
        if (formerErr != null && (err == System.err)) {
            System.setErr(formerErr);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) {
        marginOut = new MarginStream(System.out);
        marginErr = new MarginStream(System.err);
    }

    @Override
    public void accept(JkLog.JkLogEvent event) {
        JkLog.Type logType = event.getType();
        PrintStream stream = System.out;
        if (logType == JkLog.Type.ERROR || logType == JkLog.Type.WARN) {
            stream.flush();
            stream = System.err;
        }
        String message = event.getMessage();
        if (logType == JkLog.Type.END_TASK) {
        } else if (logType== JkLog.Type.START_TASK) {
            stream.print(message);
            marginOut.notifyStart();
            marginErr.notifyStart();
        } else {
            stream.println(message);
        }
    }

    @Override
    public OutputStream getOutStream() {
        return System.out;
    }

    @Override
    public OutputStream getErrorStream() {
        return System.err;
    }

    private static class MarginStream extends OutputStream {

        private final PrintStream delegate;

        private int lastByte = LINE_SEPARATOR;  // Display margin at first use (relevant for ofSystem.err)

        private boolean pendingStart;

        private boolean endTask;

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
            if (pendingStart & !endTask) {
                delegate.write(LINE_SEPARATOR);
                lastByte = LINE_SEPARATOR;
                pendingStart = false;
            }
            if (lastByte == LINE_SEPARATOR) {
                Integer level = JkLog.getCurrentNestedLevel();
                if (endTask) level++;
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
