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

    // Must not be replaced by EventLogHandler cause serialisation/classloader issues.
    private static Consumer<JkLogEvent> consumer;

    private static OutputStream stream = JkUtilsIO.nopPrintStream();

    private static OutputStream errorStream = JkUtilsIO.nopOuputStream();

    private static Verbosity verbosity = Verbosity.NORMAL;

    // Field accessed though reflection
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

    public static void register(EventLogHandler eventLogHandler) {
        consumer = eventLogHandler;
        stream = eventLogHandler.getOutStream();
        errorStream = eventLogHandler.getErrorStream();
    }

    public static void registerHierarchicalConsoleHandler() {
        register(new JkHierarchicalConsoleLogHandler());
    }

    public static void setVerbosity(Verbosity verbosityArg) {
        JkUtilsAssert.notNull(verbosityArg, "Verbosity can noot be set to null.");
        verbosity = verbosityArg;
    }

    public static int getCurrentNestedLevel() {
        return currentNestedTaskLevel.get();
    }

    public static void initializeInClassLoader(ClassLoader classLoader) {
        try {
            Class<?> targetClass = classLoader.loadClass(JkLog.class.getName());
            JkUtilsReflect.setFieldValue(null, targetClass.getDeclaredField("consumer"), consumer);
            JkUtilsReflect.setFieldValue(null,targetClass.getDeclaredField("stream"), stream);
            JkUtilsReflect.setFieldValue(null,targetClass.getDeclaredField("errorStream"), errorStream);
            JkUtilsReflect.setFieldValue(null, targetClass.getDeclaredField("currentNestedTaskLevel"),
                    currentNestedTaskLevel);
            JkUtilsReflect.setFieldValue(null, targetClass.getDeclaredField("verbosity"),
                    JkUtilsIO.cloneBySerialization(verbosity, classLoader));
        } catch (ReflectiveOperationException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    public static OutputStream getOutputStream() {
        if (Verbosity.MUTE == verbosity()) {
            return JkUtilsIO.nopPrintStream();
        }
        return stream;
    }

    public static OutputStream getErrorStream() {
        if (Verbosity.MUTE == verbosity()) {
            return JkUtilsIO.nopPrintStream();
        }
        return errorStream;
    }

    public static void info(String message) {
        consume(JkLogEvent.ofRegular(Type.INFO, message));
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
    public static void startTask(String message) {
        consume(JkLogEvent.ofRegular(Type.START_TASK, message));
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

    public static Consumer<JkLogEvent> getLogConsumer() {
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

    public interface EventLogHandler extends Consumer<JkLogEvent> {

        OutputStream getOutStream();

        OutputStream getErrorStream();

    }

}
