package dev.jeka.core.api.system;

import java.io.*;
import java.nio.charset.Charset;

public final class JkBraceConsoleLogConsumer implements JkLog.JkEventLogConsumer, Serializable {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final byte LINE_SEPARATOR = 10;

    private static final byte[] MARGIN_UNIT = ("   ").getBytes(UTF8);

    private static final int MARGIN_UNIT_LENGTH = new String(MARGIN_UNIT, UTF8).length();

    private static int maxLength = -1;

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

    public static void setMaxLength(int maxLength) {
        JkBraceConsoleLogConsumer.maxLength = maxLength;
    }

    @Override
    public void accept(JkLog.JkLogEvent event) {
        final MarginStream marginStream = (event.getType() == JkLog.Type.ERROR) ? marginErr : marginOut;

        final PrintStream stream = (event.getType() == JkLog.Type.ERROR) ? System.err : System.out;
        String message = event.getMessage();
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
            marginStream.notifyStart();
        }  else {
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
