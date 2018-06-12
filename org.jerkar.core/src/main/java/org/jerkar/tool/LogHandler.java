package org.jerkar.tool;

import org.jerkar.api.system.JkEvent;
import org.jerkar.api.utils.JkUtilsIO;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.util.LinkedList;

class LogHandler implements JkEvent.EventLogHandler {

    private static final char BOX_DRAWINGS_LIGHT_VERTICAL = 0x2502;

    private static final char BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = 0x2514;

    private static final byte LINE_SEPARATOR = 10;

    private static final byte[] MARGIN_UNIT = ("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ").getBytes();

    private final LinkedList<Boolean> hasLoggedSinceStart = new LinkedList<>();

    private int nestedLevel = 0;

    private final PrintStream out = new MarginStream(System.out);

    private final PrintStream err = new MarginStream(System.err);


    @Override
    public void accept(JkEvent event) {
        boolean newLine = true;
        String message = event.message();
        nestedLevel = event.nestedLevel();
        if (event.type() == JkEvent.Type.END_TASK) {
            StringBuilder sb = new StringBuilder();
            if (!hasLoggedSinceStart.getLast()) {
                newLine = false;
            } else {
                sb.append(BOX_DRAWINGS_LIGHT_UP_AND_RIGHT);
            }
            sb.append(" done in " + (JkEvent.getElapsedNanoSecondsFromStartOfCurrentTask() / 1000000) + " milliseconds. ")
                    .append(message);
            message = sb.toString();
            if (!hasLoggedSinceStart.isEmpty()){
                hasLoggedSinceStart.removeLast();
                hasLoggedSinceStart.add(Boolean.TRUE);
            }
        } else {
            if (event.type() == JkEvent.Type.START_TASK) {
                message = message +  " ... ";
                hasLoggedSinceStart.add(Boolean.FALSE);
            } else if (!hasLoggedSinceStart.isEmpty()){
                hasLoggedSinceStart.removeLast();
                hasLoggedSinceStart.add(Boolean.TRUE);
            }
        }
        final PrintStream printStream = event.type() == JkEvent.Type.ERROR ? err : out;
        if (newLine) {
            printStream.println();
        }
        printStream.print(message);
    }


    @Override
    public PrintStream outStream() {
        //return out;
       return JkUtilsIO.nopPrintStream();
}

    @Override
    public PrintStream errorStream() {
        //return  new MarginStream(System.err);
        return JkUtilsIO.nopPrintStream();
    }

    private class MarginStream extends PrintStream {

        public MarginStream(PrintStream delegate) {
            super(delegate);
        }

        @Override
        public void write(byte[] bytes, int off, int len) {
            if (len == 0) {
                return;
            }
            try {
                synchronized (this) {
                    for (int i = 0 ; i < len ; i++) {
                        byte b = bytes[off + i];
                        out.write(b);
                        if (b == LINE_SEPARATOR) {
                            writeLeftMargin();
                        }
                    }
                    this.flush();
                }
            }
            catch (InterruptedIOException x) {
                Thread.currentThread().interrupt();
            }
            catch (IOException x) {
                setError();
            }
        }


        private void writeLeftMargin()  {
            for (int j = 0; j < nestedLevel; j++) {
                try {
                    out.write(MARGIN_UNIT);
                } catch (IOException e) {
                    setError();
                }
            }
        }

    }

}
