package org.jerkar.tool;

import org.jerkar.api.system.JkLog;

import java.io.*;
import java.nio.charset.Charset;


class LogHandler implements JkLog.EventLogHandler {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final char BOX_DRAWINGS_LIGHT_VERTICAL = 0x2502;   // Shape similar to '|'

    private static final char BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = 0x2514;   // Shape similar to 'L'

    private static final byte LINE_SEPARATOR = 10;

    private static final byte[] MARGIN_UNIT = ("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ").getBytes(UTF8);

    private static final int MARGIN_UNIT_LENGTH = new String(MARGIN_UNIT, UTF8).length();

    static final int MAX_LENGTH = -1;

    private final MarginStream out = new MarginStream(System.out, MAX_LENGTH);

    private final MarginStream err = new MarginStream(System.err, MAX_LENGTH);

    @Override
    public void accept(JkLog.JkLogEvent event) {
        String message = event.message();
        if (event.type() == JkLog.Type.END_TASK) {
                StringBuilder sb = new StringBuilder();
                sb.append(BOX_DRAWINGS_LIGHT_UP_AND_RIGHT);
                if (event.durationMs() >= 0) {
                    sb.append(" Done in " + event.durationMs() + " milliseconds.");
                }
                sb.append(" ").append(message);
                message = sb.toString();
        } else if (event.type() == JkLog.Type.START_TASK) {
                message = message +  " ... ";
        }
        final MarginStream stream = (event.type() == JkLog.Type.ERROR) ? err : out;
        stream.handlingStart = event.type() == JkLog.Type.START_TASK;
        try {
            stream.write(message.getBytes(UTF8));
            stream.write(LINE_SEPARATOR);
            stream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        stream.handlingStart = false;
    }

    @Override
    public OutputStream outStream() {
        return out;
    }

    @Override
    public OutputStream errorStream() {
        return err;
    }

    private static class MarginStream extends OutputStream {

        private final PrintStream delegate;

        private final int maxLength;

        private int lineLength;

        private int lastByte = LINE_SEPARATOR;  // Display margin at first use (relevant for system.err)

        private boolean handlingStart;


        public MarginStream(PrintStream delegate, int maxLength) {
            super();
            this.delegate = delegate;
            this.maxLength = maxLength;
        }

        @Override
        public void write(int b) throws IOException {
            if (lastByte == LINE_SEPARATOR) {
                lineLength = 0;
                for (int j = 0; j < JkLog.currentNestedLevel(); j++) {
                    delegate.write(MARGIN_UNIT);
                    lineLength += MARGIN_UNIT_LENGTH;
                }
            }
            delegate.write(b);
            lastByte = b;
            lineLength ++;  // approximate 1 byte = 1 char (untrue for special characters).
            if (maxLength > -1 && lineLength > maxLength) {
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
