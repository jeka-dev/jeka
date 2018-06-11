package org.jerkar.tool;

import org.jerkar.api.system.JkEvent;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsString;

import java.io.PrintStream;
import java.util.LinkedList;

class LogHandler implements JkEvent.EventLogHandler {

    private static final char BOX_DRAWINGS_LIGHT_VERTICAL = 0x2502;

    private static final char BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = 0x2514;

    private final LinkedList<Boolean> hasLoggedSinceStart = new LinkedList<>();

    private int nestedLevel = 0;

    @Override
    public void accept(JkEvent event) {
        final String prefix;
        String suffix = "";
        boolean newLine = true;
        if (event.type() == JkEvent.Type.END_TASK) {
            if (hasLoggedSinceStart.pollLast()) {
                prefix = JkUtilsString.repeat("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ", event.nestedLevel() - 1)
                        + BOX_DRAWINGS_LIGHT_UP_AND_RIGHT + " ";
            } else {
                prefix = "";
                newLine = false;
            }
            suffix = "Done in " + (JkEvent.getElapsedNanoSecondsFromStartOfCurrentTask() / 1000000) + " milliseconds. ";
            if (!hasLoggedSinceStart.isEmpty()){
                hasLoggedSinceStart.removeLast();
                hasLoggedSinceStart.add(Boolean.TRUE);
            }
        } else {
            if (event.type() == JkEvent.Type.START_TASK) {
                suffix =  " ... ";
                hasLoggedSinceStart.add(Boolean.FALSE);
                nestedLevel = event.nestedLevel();
            } else if (!hasLoggedSinceStart.isEmpty()){
                hasLoggedSinceStart.removeLast();
                hasLoggedSinceStart.add(Boolean.TRUE);
            }
            prefix = leftMargin();
        }
        PrintStream printStream = System.out;
        String[] lines =  event.message().split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (newLine) {
                printStream.print("\n");
            }
            printStream.print(prefix);
            printStream.print(lines[i]);
            if (i == lines.length - 1) {
                printStream.print(suffix);
            }
        }
    }

    private String leftMargin() {
        return JkUtilsString.repeat("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ", nestedLevel);
    }

    @Override
    public PrintStream outStream() {
       // return new MarginStream(System.out);
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
        public void write(byte[] cbuf, int off, int len) {
            String buffer = new String(cbuf);
            byte[] margin = ("\n" + leftMargin()).getBytes();
            String[] lines =  buffer.split("\n");
            for (int i = 0; i < lines.length; i++) {
                super.write(margin, 0, margin.length);
                byte[] bytes = lines[i].getBytes();
                super.write(bytes, 0, bytes.length);
            }
        }
    }

}
