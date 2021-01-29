package dev.jeka.core.api.system;

import java.io.*;
import java.nio.charset.Charset;

public final class JkSquareConsoleLogConsumer implements JkLog.JkEventLogConsumer, Serializable {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final char BOX_DRAWINGS_LIGHT_VERTICAL = 0x2502;   // Shape similar to '|'

    private static final char BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = 0x2514;   // Shape similar to 'L'

    private static final byte LINE_SEPARATOR = 10;

    private static final byte[] MARGIN_UNIT = ("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ").getBytes(UTF8);

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
        JkSquareConsoleLogConsumer.maxLength = maxLength;
    }

    @Override
    public void accept(JkLog.JkLogEvent event) {
        String message = event.getMessage();
        if (event.getType() == JkLog.Type.END_TASK) {
                StringBuilder sb = new StringBuilder();
                sb.append(BOX_DRAWINGS_LIGHT_UP_AND_RIGHT);
                if (event.getDurationMs() >= 0) {
                    sb.append(" Done in " + event.getDurationMs() + " milliseconds.");
                }
                sb.append(" ").append(message);
                message = sb.toString();
        } else if (event.getType() == JkLog.Type.START_TASK) {
                message = message +  " ... ";
        }
        final MarginStream marginStream = (event.getType() == JkLog.Type.ERROR) ? marginErr : marginOut;
        final PrintStream stream = (event.getType() == JkLog.Type.ERROR) ? System.err : System.out;
        marginStream.handlingStart = event.getType() == JkLog.Type.START_TASK;
        try {
            if (event.getType() == JkLog.Type.WARN) {
                stream.write("Warn: ".getBytes(UTF8));
            } else if (event.getType() == JkLog.Type.ERROR) {
                stream.write("Error: ".getBytes(UTF8));
            }
            System.out.println(message);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        marginStream.handlingStart = false;
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

        private int lineLength;

        private int lastByte = LINE_SEPARATOR;  // Display margin at first use (relevant for ofSystem.err)

        private boolean handlingStart;

        public MarginStream(PrintStream delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            if (lastByte == LINE_SEPARATOR) {
                lineLength = 0;
                for (int j = 0; j < JkLog.getCurrentNestedLevel(); j++) {
                    delegate.write(MARGIN_UNIT);
                    lineLength += MARGIN_UNIT_LENGTH;
                }
            }
            delegate.write(b);
            lastByte = b;
            lineLength ++;  // approximate 1 byte = 1 char (untrue for special characters).
            if (JkSquareConsoleLogConsumer.maxLength > -1 && lineLength > JkSquareConsoleLogConsumer.maxLength) {
                lineLength = 0;
                if (handlingStart) {
                    this.write(("\n" + new String(MARGIN_UNIT, UTF8) + "  ").getBytes(UTF8));
                } else {
                    this.write(("\n    ").getBytes(UTF8));
                }
            }
        }

        @Override
        public void flush() {
            delegate.flush();
        }

    }

}
