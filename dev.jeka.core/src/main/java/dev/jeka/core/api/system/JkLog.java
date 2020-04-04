package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.*;

import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Provides static methods for logging events. <p>
 *
 * Events are simply forwarded to an event consumer which has the responsibility to handle it. A basic handler may
 * simply displays event on the console. That is the case for the Jeka tool.
 * By default, logging events turns in a no-op.<p>
 *
 * This class provides task concept for hierarchical log. A log event happening during a task will be assigned a nested
 * task level (task can be nested).
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

    private static JkEventLogConsumer consumer;

    private static OutputStream stream = JkUtilsIO.nopPrintStream();

    private static OutputStream errorStream = JkUtilsIO.nopOuputStream();

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
     * By default events are not consumed, meaning nothing appends when {@link #info(String)},
     * {@link #error(String)}, {@link #warn(String)} or {@link #trace(String)} are invoked.
     * Thus users have to set explicitly a consumer using this method or {@link #setHierarchicalConsoleConsumer()}.
     */
    public static void setConsumer(JkEventLogConsumer consumerArg) {
        if (consumer != null) {
            consumer.restore();
        }
        consumerArg.init();
        consumer = consumerArg;
        stream = consumerArg.getOutStream();
        errorStream = consumerArg.getErrorStream();

    }

    /**
     * This set the default consumer. This consumer displays logs in the console in a hierarchical style.
     */
    public static void setHierarchicalConsoleConsumer() {
        setConsumer(new JkHierarchicalConsoleLogConsumer());
    }

    public static void setVerbosity(Verbosity verbosityArg) {
        JkUtilsAssert.notNull(verbosityArg, "Verbosity can noot be set to null.");
        verbosity = verbosityArg;
    }

    public static int getCurrentNestedLevel() {
        return currentNestedTaskLevel.get();
    }

    public static OutputStream getOutputStream() {
        if (Verbosity.MUTE == verbosity()) {
            return JkUtilsIO.nopPrintStream();
        }
        return JkUtilsObject.firstNonNull(stream, JkUtilsIO.nopOuputStream());
    }

    public static OutputStream getErrorStream() {
        if (Verbosity.MUTE == verbosity()) {
            return JkUtilsIO.nopPrintStream();
        }
        return JkUtilsObject.firstNonNull(errorStream, JkUtilsIO.nopOuputStream());
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
                throw new JkException("No start task found matching with this endTask. Check that you don't have " +
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
        if (consumer == null) {
            return;
        }
        if (!shouldPrint(event.getType()) ){
            return;
        }

        // This is necessary for avoing class cast exception when run in other classloader (unit tests)
        if (event.getClass().getClassLoader() != consumer.getClass().getClassLoader()) {  // survive to classloader change
            final Object evt = JkUtilsIO.cloneBySerialization(event, consumer.getClass().getClassLoader());
            try {
                Method accept = consumer.getClass().getMethod("accept", evt.getClass());
                accept.setAccessible(true);
                accept.invoke(consumer, evt);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            consumer.accept((JkLogEvent) event);
        }
    }

    public static JkEventLogConsumer getConsumer() {
        return consumer;
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

    public interface JkEventLogConsumer extends Consumer<JkLogEvent>, Serializable {

        void init();

        void restore();

        OutputStream getOutStream();

        OutputStream getErrorStream();

    }

    public static class JkState {

        private static JkEventLogConsumer consumer;

        private static OutputStream stream;

        private static OutputStream errorStream;

        private static Verbosity verbosity;

        private static AtomicInteger currentNestedTaskLevel;

        public static void save() {
            consumer = JkLog.consumer;
            stream = JkLog.stream;
            errorStream = JkLog.errorStream;
            verbosity = JkLog.verbosity;
            currentNestedTaskLevel = JkLog.currentNestedTaskLevel;
        }

        public static void restore() {
            if (currentNestedTaskLevel == null) {  // state has never been saved
                return;
            }
            JkLog.consumer = consumer;
            JkLog.stream = stream;
            JkLog.errorStream = errorStream;
            JkLog.verbosity = verbosity;
            JkLog.currentNestedTaskLevel = currentNestedTaskLevel;
        }


    }

}
