package org.jerkar.api.system;

import org.jerkar.api.utils.*;

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
 * simply displays event on the console. That is the case for the Jerkar tool.
 * By default, logging events turns in a no-op.<p>
 *
 * This class provides task concept for hierarchical log. A log event happening during a task will be assigned a nested
 * task level (task can be nested).
 */
public final class JkLog implements Serializable {

    public enum Type {
        INFO, WARN, ERROR, TRACE, PROGRESS, TASK, START_TASK, END_TASK;
    }

    public enum Verbosity {
        MUTE, NORMAL, VERBOSE, QUITE_VERBOSE;

        public boolean isVerbose() {
            return this == VERBOSE || this == QUITE_VERBOSE;
        }
    }

    // Must not be replaced by EventLogHandler cause serialisation/classloader issues.
    private static Consumer<JkLogEvent> consumer;

    private static OutputStream stream = JkUtilsIO.nopPrintStream();

    private static OutputStream errorStream = JkUtilsIO.nopOuputStream();

    private static Verbosity verbosity = Verbosity.NORMAL;

    private static AtomicInteger currentNestedTaskLevel = new AtomicInteger(0);

    private static final ThreadLocal<LinkedList<Long>> START_TIMES = new ThreadLocal<>();

    static {
        START_TIMES.set(new LinkedList<>());
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

    public static void execute(String message, Runnable task) {
        consume(JkLogEvent.ofRegular(Type.START_TASK, message));
        currentNestedTaskLevel.incrementAndGet();
        final long startTs = System.nanoTime();
        task.run();
        long durationMs = (System.nanoTime() - startTs) / 1000000;
        currentNestedTaskLevel.decrementAndGet();
        consume((JkLogEvent.ofEndTask(durationMs)));
    }

    public static void startTask(String message) {
        consume(JkLogEvent.ofRegular(Type.START_TASK, message));
        currentNestedTaskLevel.incrementAndGet();
        START_TIMES.get().push(System.nanoTime());
    }

    public static void endTask(String message) {
        currentNestedTaskLevel.decrementAndGet();
        consume(JkLogEvent.ofRegular(Type.END_TASK, message));
        START_TIMES.get().pollLast();
    }

    public static void endTask() {
        currentNestedTaskLevel.decrementAndGet();
        Long startTime = START_TIMES.get().pollLast();
        if (startTime == null) {
            throw new JkException("No start task found matching with this endTask. Check that you don't have " +
                    "used an 'endTask' one too many in your code.");
        }
        Long durationMillis = JkUtilsTime.durationInMillis(startTime);
        consume(JkLogEvent.ofRegular(Type.END_TASK, "Done in " + durationMillis + " milliseconds."));

    }

    public static boolean isVerbose() {
        return verbosity == Verbosity.VERBOSE;
    }

    private static void consume(Object event) {
        if (Verbosity.MUTE == verbosity()) {
            return;
        }
        if (consumer == null) {
            return;
        }
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

        static JkLogEvent ofEndTask(long duration) {
            return new JkLogEvent(Type.END_TASK, "",  duration);
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
