package org.jerkar.tool;

import org.jerkar.api.system.JkEvent;
import org.jerkar.api.utils.JkUtilsString;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.function.Consumer;

class LogHandler implements Consumer<JkEvent> {

    private static final char BOX_DRAWINGS_LIGHT_VERTICAL = 0x2502;

    private static final char BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = 0x2514;

    private final LinkedList<Boolean> hasLoggedSinceStart = new LinkedList<>();

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
            suffix = "Done in " + (System.currentTimeMillis() - JkEvent.getLastTaskStartTs()) + " milliseconds. ";
            if (!hasLoggedSinceStart.isEmpty()){
                hasLoggedSinceStart.removeLast();
                hasLoggedSinceStart.add(Boolean.TRUE);
            }
        } else {
            prefix = JkUtilsString.repeat("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ", event.nestedLevel());
            if (event.type() == JkEvent.Type.START_TASK) {
                suffix =  "... ";
                hasLoggedSinceStart.add(Boolean.FALSE);
            } else if (!hasLoggedSinceStart.isEmpty()){
                hasLoggedSinceStart.removeLast();
                hasLoggedSinceStart.add(Boolean.TRUE);
            }
        }
        PrintStream printStream = printStream();
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

    public OutputStream stream() {
        return System.out;
    }

    private PrintStream printStream() {
        if (stream() instanceof PrintStream) {
            return (PrintStream) stream();
        }
        return new PrintStream(stream());
    }

}
