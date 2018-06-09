package org.jerkar.tool;

import org.jerkar.api.system.JkEvent;
import org.jerkar.api.utils.JkUtilsString;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.TimeZone;
import java.util.function.Consumer;

class LogHandler implements Consumer<JkEvent> {

    private static final char BOX_DRAWINGS_LIGHT_VERTICAL = 0x2502;

    private static final char BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = 0x2514;

    @Override
    public void accept(JkEvent event) {
        final String prefix;
        if (event.type() == JkEvent.Type.END_TASK) {
            prefix = JkUtilsString.repeat("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ", event.nestedLevel() - 1)
            + BOX_DRAWINGS_LIGHT_UP_AND_RIGHT + " Done in " + (System.currentTimeMillis() - JkEvent.getLastTaskStartTs()) + " milliseconds. ";
        } else {
            prefix = JkUtilsString.repeat("" + BOX_DRAWINGS_LIGHT_VERTICAL + " ", event.nestedLevel());
        }
        PrintStream printStream = printStream();
        for (String line : event.message().split("\n")) {
            printStream.print(prefix);
            printStream.print(line);
            printStream.print("\n");
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
