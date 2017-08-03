package org.jerkar.api.system;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;

import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsTime;

/**
 * Logger shared globally on the classloader. It provides time tracking method
 * and indentation feature accounting for task/subtask execution.
 *
 * @author Jerome Angibaud
 */
public final class JkLog {

    private static final String INDENT = "|  ";

    private static final String TAB = "  ";

    private static final ThreadLocal<LinkedList<Long>> START_TIMES = new ThreadLocal<LinkedList<Long>>();

    private static OffsetStream infoWriter = new OffsetStream(System.out);

    private static OffsetStream errorWriter = new OffsetStream(System.err);

    private static OffsetStream warnWriter = new OffsetStream(System.err);

    private static boolean silent;

    private static boolean verbose;

    /**
     * Set the silent mode to the specified mode.
     *
     * @see #silent()
     */
    public static void silent(boolean mode) {
        silent = mode;
    }

    /**
     * Returns <code>true</code> if the logger is in silent mode. In silent
     * mode, nothing is mogged.
     */
    public static boolean silent() {
        return silent;
    }

    /**
     * Specifies the verbose mode #see {@link #verbose()}
     */
    public static void verbose(boolean flag) {
        verbose = flag;
    }

    /**
     * Returns true if the log is in verbose mode. In verbose mode the message
     * logged with {@link #trace(String)} are actually logged. They are not
     * logged in non-verbose mode.
     */
    public static boolean verbose() {
        return verbose;
    }

    /**
     * Logs a message indicating that a processing has been started. Elipsis are
     * added at the end of the message and all subsequent logs will be shift
     * right until {@link #done()} is invoked.
     */
    public static void start(String message) {
        if (silent) {
            return;
        }
        infoWriter.print(message + " ... ");
        incOffset();
        startTimer();
    }

    /**
     * Returns the the infoString strem if the log is currently in verbose mode.
     * It returns <code>null</code> otherwise.
     * @return
     */
    public static PrintStream infoStreamIfVerbose() {
        if (silent) {
            return null;
        }
        if (verbose) {
            return infoStream();
        }
        return null;
    }

    private static void startTimer() {
        if (silent) {
            return;
        }
        LinkedList<Long> times = START_TIMES.get();
        if (times == null) {
            times = new LinkedList<Long>();
            START_TIMES.set(times);
        }
        times.push(System.nanoTime());
    }

    /**
     * As {@link #start(String)} but do a carriage return after the start
     * message.
     */
    public static void startln(String message) {
        if (silent) {
            return;
        }
        start(message);
        nextLine();
    }

    /**
     * As {@link #startln(String)} but underline the message.
     */
    public static void startUnderlined(String message) {
        if (silent) {
            return;
        }
        infoUnderlined(message);
        incOffset();
        startTimer();
    }

    /**
     * As {@link #startln(String)} but whith header message.
     */
    public static void startHeaded(String message) {
        if (silent) {
            return;
        }
        infoHeaded(message);
        incOffset();
        startTimer();
    }

    /**
     * Logs the specified message if the logger is currently in verbose mode. Do
     * nothing otherwise.
     */
    public static void trace(String message) {
        if (silent) {
            return;
        }
        if (verbose) {
            JkLog.info(message);
        }
    }

    /**
     * Notify that the processing notified with 'start' has terminated. The
     * elapsed time between last {@link #start(String)} invoke and this method
     * invoke is notified. Also the the shifting due to last 'start' invoke is
     * annihilated.
     */
    public static void done() {
        doneMessage("Done");
    }

    /**
     * As {@link #done()} but adding a tailored message.
     */
    public static void done(String message) {
        doneMessage("Done : " + message);
    }

    // This method is called by reflection when changing classloader
    static void beginOfLine() {
        infoWriter.beginOfLine = true;
        warnWriter.beginOfLine = true;
        errorWriter.beginOfLine = true;
    }

    private static void doneMessage(String message) {
        if (silent) {
            return;
        }
        decOffset();
        final LinkedList<Long> times = START_TIMES.get();
        if (times == null || times.isEmpty()) {
            throw new IllegalStateException(
                    "This 'done' do no match to any 'start'. "
                            + "Please, use 'done' only to mention that the previous 'start' activity is done.");
        }
        final long start = times.poll();
        infoWriter.println(" \\ " + message + " in " + JkUtilsTime.durationInSeconds(start)
        + " seconds.");

    }

    /**
     * Displays a message at infoString level.
     */
    public static void info(String message) {
        if (silent) {
            return;
        }
        infoWriter.println(message);
    }

    /**
     * Displays a multi-line message of the specified message followed by
     * specified lines.
     */
    public static void info(String message, Iterable<String> lines) {
        if (silent) {
            return;
        }
        infoWriter.print(message);
        for (final String line : lines) {
            infoWriter.println(line);
        }
    }

    /**
     * Displays multi-line message.
     */
    public static void info(Iterable<String> lines) {
        if (silent) {
            return;
        }
        for (final String line : lines) {
            infoWriter.println(line);
        }
    }

    /**
     * Displays multi-line message truncating lines if exceed the specified length
     */
    public static void info(Iterable<String> lines, int maxLength) {
        if (silent) {
            return;
        }
        for (final String line : lines) {
            infoWriter.println(JkUtilsString.elipse(line, maxLength));
        }
    }

    /**
     * Displays multi-line message.
     */
    public static void info(String... lines) {
        if (silent) {
            return;
        }
        info(Arrays.asList(lines));
    }

    /**
     * Displays a multi-line message at warn level.
     */
    public static void warn(Iterable<String> lines) {
        if (silent) {
            return;
        }
        for (final String line : lines) {
            warn(line);
        }
    }

    /**
     * Displays a multi-line message at warn level.
     */
    public static void warn(String ... lines) {
        if (silent) {
            return;
        }
        warn(Arrays.asList(lines));
    }

    /**
     * Displays a message at warn level.
     */
    public static void warn(String message) {
        if (silent) {
            return;
        }
        infoWriter.println("WARN : " + message);
    }

    /**
     * Displays a message at warn level if the specified condition is
     * <code>true</code>.
     */
    public static void warnIf(boolean condition, String message) {
        if (condition) {
            warn(message);
        }
    }

    /**
     * Displays a message at error level.
     */
    public static void error(String message) {
        if (silent) {
            return;
        }
        errorWriter.println(message);
    }

    /**
     * Displays a message at error level.
     */
    public static void error(Iterable<String> lines) {
        if (silent) {
            return;
        }
        for (final String line : lines) {
            errorWriter.println(line);
        }
    }

    /**
     * Line jump.
     */
    public static void nextLine() {
        if (silent) {
            return;
        }
        infoWriter.println();
    }

    /**
     * Returns the stream for infoString level.
     */
    public static PrintStream infoStream() {
        return infoWriter;
    }

    /**
     * Returns the stream for warn level.
     */
    public static PrintStream warnStream() {
        return warnWriter;
    }

    /**
     * Returns the stream for error level.
     */
    public static PrintStream errorStream() {
        return errorWriter;
    }

    private static void decOffset() {
        infoWriter.dec();
        warnWriter.dec();
        errorWriter.dec();
    }

    private static void incOffset() {
        infoWriter.inc();
        warnWriter.inc();
        errorWriter.inc();
    }

    /**
     * Shifts the left margin. All subsequent log will be shifted
     * <code>delta</code> characters to right.
     */
    public static void delta(int delta) {
        infoWriter.tabLevel += delta;
        errorWriter.tabLevel += delta;
        warnWriter.tabLevel += delta;
    }

    /**
     * Returns the current left margin size in character.
     */
    public static int offset() {
        return infoWriter.offsetLevel;
    }

    static void offset(int offset) {
        infoWriter.offsetLevel = offset;
        errorWriter.offsetLevel = offset;
        warnWriter.offsetLevel = offset;
    }

    private static class OffsetStream extends PrintStream {

        private static final String SEPARATOR = System.getProperty("line.separator");

        private int offsetLevel;

        private int tabLevel;

        private boolean beginOfLine;

        public OffsetStream(PrintStream delegate) {
            super(delegate);
        }

        @Override
        public void println(String s) {
            super.println(s);
            beginOfLine = true;
        }

        @Override
        public void println() {
            super.println();
            beginOfLine = true;
        }

        @Override
        public void print(String s) {
            super.print(s);
            beginOfLine = s.endsWith(SEPARATOR);
        }

        @Override
        public void write(byte[] cbuf, int off, int len) {
            final byte[] filler = getFiller().getBytes();
            final int lenght = filler.length;
            if (lenght > 0 && beginOfLine) {
                super.write(filler, 0, lenght);
            }
            super.write(cbuf, off, len);
        }

        private String getFiller() {
            if (offsetLevel == 0 && tabLevel == 0) {
                return "";
            }
            if (offsetLevel == 1 && tabLevel == 0) {
                return INDENT;
            }
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < offsetLevel; i++) {
                result.append(INDENT);
            }
            for (int i = 0; i < tabLevel; i++) {
                result.append(TAB);
            }
            return result.toString();
        }

        public void inc() {
            offsetLevel++;
        }

        public void dec() {
            if (offsetLevel > 0) {
                offsetLevel--;
            }
        }

    }

    /**
     * Logs in infoString stream the specified message enclosed as :
     *
     * <pre>
     * ------------------
     * message to display
     * ------------------
     * </pre>
     */
    public static void infoHeaded(String intro) {
        if (silent) {
            return;
        }
        final String pattern = "-";
        JkLog.info(JkUtilsString.repeat(pattern, intro.length()));
        JkLog.info(intro);
        JkLog.info(JkUtilsString.repeat(pattern, intro.length()));
    }

    /**
     * Logs in infoString stream the specified message enclosed as :
     *
     * <pre>
     * message to display
     * ------------------
     * </pre>
     */
    public static void infoUnderlined(String message) {
        if (silent) {
            return;
        }
        JkLog.info(message);
        JkLog.info(JkUtilsString.repeat("-", message.length()));
    }

}
