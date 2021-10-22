package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsTime;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides static methods for logging events. <p>
 *
 * Events are simply forwarded to an event consumer which has the responsibility to handle it. A basic handler may
 * simply display events on the console. That is the case for the Jeka tool.
 * By default, logging events turns in a no-op.<p>
 *
 * This class provides task concept for hierarchical logs. A log event happening during a task will be assigned a nested
 * task level (task can be nested). This feature mekes hierarchical logging possible.
 */
public final class JkLog implements Serializable {

    public enum Type {
        INFO, WARN, ERROR, TRACE, PROGRESS, TASK, START_TASK, END_TASK
    }

    public enum Verbosity {
        MUTE, WARN_AND_ERRORS, NORMAL, VERBOSE, QUITE_VERBOSE;

        public boolean isVerbose() {
            return this == VERBOSE || this == QUITE_VERBOSE;
        }
    }
    
    public enum Style {
        BRACE(new JkBraceLogDecorator()),
        INDENT(new JkIndentLogDecorator()),
        DEBUG(new JkDebugLogDecorator());

        private final JkLogDecorator decorator;

        Style(JkLogDecorator decorator) {
            this.decorator = decorator;
        }
    }

    private static final NoOpDecorator NO_OP_DECORATOR = new NoOpDecorator();

    private static final PrintStream NO_OP_STREAM = JkUtilsIO.nopPrintStream();

    private static final PrintStream INITIAL_OUT = System.out;

    private static final PrintStream INITIAL_ERR = System.err;

    private static JkLogDecorator decorator = NO_OP_DECORATOR;

    private static Verbosity verbosity = Verbosity.NORMAL;

    private static AtomicInteger currentNestedTaskLevel = new AtomicInteger(0);

    private static final ThreadLocal<LinkedList<Long>> START_TIMES = new ThreadLocal<>();

    private static LinkedList<Long> getStartTimes() {
        LinkedList<Long> result = START_TIMES.get();
        if (result == null) {
            result = new LinkedList<>();
            START_TIMES.set(result);
        }
        return result;
    }

    /**
     * By default, events are not consumed, meaning nothing appends when {@link #info(String, Object...)} (String)},
     * {@link #error(String)}, {@link #warn(String)} or {@link #trace(String)} are invoked.
     * Therefore, users have to set explicitly a consumer using this method or {@link #setConsumer(Style)} ()}.
     */
    public static void setDecorator(JkLogDecorator newDecorator) {
        newDecorator.doInit(INITIAL_OUT, INITIAL_ERR);
        decorator = newDecorator;
        System.setOut(decorator.getOut());
        System.setErr(decorator.getErr());
    }

    /**
     * This set the default consumer. This consumer displays logs in the console in a hierarchical style.
     */
    public static void setConsumer(Style style) {
        setDecorator(style.decorator);
    }

    /**
     *  Let JkLog returns to its initial state, meaning events are not output and System.out/err are back to the
     *  state found when this class was loaded.
     */
    public static void restoreToInitialState() {
        System.setOut(INITIAL_OUT);
        System.setErr(INITIAL_ERR);
        decorator = NO_OP_DECORATOR;
    }

    public static void redirect(PrintStream out, PrintStream err) {
        if (decorator != null) {
            decorator.doInit(out, err);
        }
    }

    public static void restoreOutput() {
        redirect(INITIAL_OUT, INITIAL_ERR);
    }

    public static void setVerbosity(Verbosity verbosityArg) {
        JkUtilsAssert.argument(verbosityArg != null, "Verbosity can noot be set to null.");
        verbosity = verbosityArg;
    }

    public static int getCurrentNestedLevel() {
        return currentNestedTaskLevel.get();
    }

    public static OutputStream getOutputStream() {
        if (Verbosity.MUTE == verbosity()) {
            return NO_OP_STREAM;
        }
        return decorator.getOut();
    }

    public static PrintStream getDecoratedErr() {
        if (Verbosity.MUTE == verbosity()) {
            return NO_OP_STREAM;
        }
        return decorator.getOut();
    }

    public static void info(String message, Object... params) {
        consume(JkLogEvent.ofRegular(Type.INFO, String.format(message, params)));
    }

    public static void warn(String message) {
        consume(JkLogEvent.ofRegular(Type.WARN, message));
    }

    public static void trace(String message) {
        if (verbosity().isVerbose()) {
            consume(JkLogEvent.ofRegular(Type.TRACE, message));
        }
    }

    public static void error(String message) {
        consume(JkLogEvent.ofRegular(Type.ERROR, message));
    }

    private static boolean shouldPrint(Type type) {
        if (Verbosity.MUTE == verbosity()) {
            return false;
        }
        if (Verbosity.WARN_AND_ERRORS == verbosity() && (type != Type.ERROR && type != Type.WARN)) {
            return false;
        }
        return true;
    }

    /**
     * Logs the start of the current task. Subsequent logs will be nested in this task log until #endTask is invoked.
     */
    public static void startTask(String message, Object ... params) {
        consume(JkLogEvent.ofRegular(Type.START_TASK, String.format(message, params)));
        if (shouldPrint(Type.START_TASK)) {
            currentNestedTaskLevel.incrementAndGet();
            getStartTimes().addLast(System.nanoTime());
        }
    }

    /**
     * Logs a end the current task with a specific message. The specified message is formatted by String#format passing
     * the duration time in milliseconds as argument. So if your message contains '%d', it will be replaced by
     * the duration taken to complete the current task.
     */
    public static void endTask(String message) {
        if (shouldPrint(Type.END_TASK)) {
            currentNestedTaskLevel.decrementAndGet();
            Long startTime = getStartTimes().pollLast();
            if (startTime == null) {
                for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                    System.err.println(ste);
                }
                throw new IllegalStateException("No start task found matching with this endTask. Check that you don't have " +
                        "used an 'endTask' one too many in your code.");

            }
            Long durationMillis = JkUtilsTime.durationInMillis(startTime);
            consume(JkLogEvent.ofRegular(Type.END_TASK, String.format(message, durationMillis)));
        }
    }

    /**
     * Same as {@link #endTask(String)} but using the standard message.
     */
    public static void endTask() {
        endTask("Done in %d milliseconds.");
    }

    public static boolean isVerbose() {
        return verbosity == Verbosity.VERBOSE;
    }

    private static void consume(JkLogEvent event) {
        if (decorator == null) {
            return;
        }
        if (!shouldPrint(event.getType()) ){
            return;
        }

        // This is necessary for avoing class cast exception when run in other classloader (unit tests)
        if (event.getClass().getClassLoader() != decorator.getClass().getClassLoader()) {  // survive to classloader change
            final Object evt = JkUtilsIO.cloneBySerialization(event, decorator.getClass().getClassLoader());
            try {
                Method accept = decorator.getClass().getMethod("accept", evt.getClass());
                accept.setAccessible(true);
                accept.invoke(decorator, evt);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            decorator.handle((JkLogEvent) event);
        }
    }

    public static JkLogDecorator getDecorator() {
        return decorator;
    }

    public static class JkLogEvent implements Serializable {

        private JkLogEvent(Type type, String message, long duration) {
            this.type = type;
            this.message = message;
            this.duration = duration;
        }

        static JkLogEvent ofRegular(Type type, String message) {
            return new JkLogEvent(type, message,  -1);
        }

        private final Type type;

        private final String message;

        private final long duration;

        public Type getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public long getDurationMs() {
            return duration;
        }
    }

    public static Verbosity verbosity() {
        return verbosity;
    }

    public static abstract class JkLogDecorator implements Serializable {

        private PrintStream targetOut;

        private PrintStream targetErr;

        final void doInit(PrintStream targetOut, PrintStream targetErr) {
            this.targetOut = targetOut;
            this.targetErr = targetErr;
            init(targetOut, targetErr);
        }

        /**
         * This method is invoked by JkLog when a new consumer is applied on.
         * JkLog provides the 2 {@link PrintStream} for outputting decorated log events
         */
        protected abstract void init(PrintStream out, PrintStream err);

        protected final PrintStream getTargetOut() {
            return targetOut;
        }

        protected final PrintStream getTargetErr() {
            return targetErr;
        }

        /**
         * Returns the decorated out {@link PrintStream} in order to be used as {@link System#out}
         */
        abstract PrintStream getOut();

        /**
         * Returns the decorated out {@link PrintStream} in order to be used as {@link System#err}
         */
        abstract PrintStream getErr();

        /**
         * Handle the specified decorator by outputting it on the streams injected in
         * {@link #init(PrintStream, PrintStream)} method.
         */
        abstract void handle(JkLogEvent event);

    }

    private static class NoOpDecorator extends JkLogDecorator {

        @Override
        protected void init(PrintStream out, PrintStream err) {
            // do nothing
        }

        @Override
        public PrintStream getOut() {
            return NO_OP_STREAM;
        }

        @Override
        public PrintStream getErr() {
            return NO_OP_STREAM;
        }

        @Override
        public void handle(JkLogEvent event) {
            // do nothing
        }
    }

    public static class JkState {

        private static JkLogDecorator decorator;

        private static PrintStream stream;

        private static PrintStream errorStream;

        private static Verbosity verbosity;

        private static AtomicInteger currentNestedTaskLevel;

        public static void save() {
            decorator = JkLog.decorator;
            verbosity = JkLog.verbosity;
            currentNestedTaskLevel = JkLog.currentNestedTaskLevel;
        }

        public static void restore() {
            if (currentNestedTaskLevel == null) {  // state has never been saved
                return;
            }
            JkLog.decorator = decorator;
            JkLog.verbosity = verbosity;
            JkLog.currentNestedTaskLevel = currentNestedTaskLevel;
        }

    }

}
