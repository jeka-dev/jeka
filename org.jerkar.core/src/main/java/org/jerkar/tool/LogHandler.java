package org.jerkar.tool;

import org.jerkar.api.system.JkLog;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

class LogHandler implements JkLog.EventLogHandler {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final char BOX_DRAWINGS_LIGHT_VERTICAL = 0x2502;   // Shape similar to '|'

    private static final char BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = 0x2514;   // Shape similar to 'L'

    private static final byte LINE_SEPARATOR = 10;

    private static final byte[] MARGIN_UNIT = ("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ").getBytes(UTF8);

    private final MarginStream out = new MarginStream(System.out);

    private final OutputStream err = new MarginStream(System.err);

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
        final OutputStream stream = event.type() == JkLog.Type.ERROR ? err : out;
        try {
            stream.write(message.getBytes(UTF8));
            stream.write(LINE_SEPARATOR);
            stream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

        private final OutputStream delegate;

        private int lastByte = LINE_SEPARATOR;  // Display margin at first use (relevant for system.err)

        public MarginStream(OutputStream delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            if (lastByte == LINE_SEPARATOR) {
                for (int j = 0; j < JkLog.currentNestedLevel(); j++) {
                    delegate.write(MARGIN_UNIT);
                }
            }
            delegate.write(b);
            lastByte = b;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

    }

}
