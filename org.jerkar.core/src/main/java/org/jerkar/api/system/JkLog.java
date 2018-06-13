package org.jerkar.api.system;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsThrowable;

import javax.xml.ws.Provider;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class JkLog implements Serializable {

    public enum Type {
        INFO, WARN, ERROR, TRACE, PROGRESS, TASK, START_TASK, END_TASK;
    }

    public enum Verbosity {
        MUTE, NORMAL, VERBOSE;
    }

    private static Consumer<JkLogEvent> consumer;

    private static OutputStream stream = JkUtilsIO.nopPrintStream();

    private static OutputStream errorStream = JkUtilsIO.nopOuputStream();

    private static Verbosity verbosity = Verbosity.NORMAL;

    private static int currentNestedTaskLevel = 0;

    private static final ThreadLocal<LinkedList<Long>> START_TIMES = new ThreadLocal<>();

    static {
        START_TIMES.set(new LinkedList<>());
    }

    public static void register(EventLogHandler eventLogHandler) {
        consumer = eventLogHandler;
        stream = eventLogHandler.outStream();
        errorStream = eventLogHandler.errorStream();
    }

    public static void setVerbosity(Verbosity verbosityArg) {
        JkUtilsAssert.notNull(verbosityArg, "Verbosity can noot be set to null.");
        verbosity = verbosityArg;
    }

    public static long getElapsedNanoSecondsFromStartOfCurrentTask() {
        final LinkedList<Long> times = START_TIMES.get();
        if (times.isEmpty()) {
            throw new IllegalStateException(
                    "This 'end' do no match to any 'start'. "
                            + "Please, use 'end' only to mention that the previous 'start' activity is done.");
        }
        return System.nanoTime() - times.getLast();
    }

    public static int currentNestedLevel() {
        return currentNestedTaskLevel;
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

    public static OutputStream stream() {
        return stream;
    }

    public static OutputStream errorStream() {
        return errorStream;
    }

    public static void info(Object emittingInstanceOrClass, String message) {
        consume(JkLogEvent.regular(emittingInstanceOrClass, Type.INFO, message));
    }

    public static void info(String message) {
        info(null, message);
    }

    public static void warn(Object emittingInstanceOrClass, String message) {
        consume(JkLogEvent.regular(emittingInstanceOrClass, Type.WARN, message));
    }

    public static void warn(String message) {
        warn(null, message);
    }

    public static void trace(Object emittingInstanceOrClass, String message) {
        if (verbosity() == Verbosity.VERBOSE) {
            consume(JkLogEvent.regular(emittingInstanceOrClass, Type.TRACE, message));
        }
    }

    public static void trace(String message) {
       trace(null,  message);
    }

    public static void error(Object emittingInstanceOrClass, String message) {
        consume(JkLogEvent.regular(emittingInstanceOrClass, Type.ERROR, message));
    }

    public static void error(String message) {
        error(null,message);
    }

    public static void execute(Object emmittingClassOrInstance, String message, Runnable task) {
        consume(JkLogEvent.regular(emmittingClassOrInstance, Type.START_TASK, message));
        currentNestedTaskLevel++;
        final long startTs = System.nanoTime();
        task.run();
        long durationMs = (System.nanoTime() - startTs) / 1000000;
        currentNestedTaskLevel--;
        consume((JkLogEvent.endTask(emmittingClassOrInstance, durationMs)));

    }

    public static void execute( String message, Runnable task) {
        execute(null, message, task);
    }

    private static Object emittingClass(Object emittingInstanceOrClass) {
        if (emittingInstanceOrClass == null) {
            return null;
        }
        return emittingInstanceOrClass.getClass().equals(Class.class) ? emittingInstanceOrClass : emittingInstanceOrClass.getClass();
    }

    private static String emittingInstance(Object emittingInstanceOrClass) {
        if (emittingInstanceOrClass == null) {
            return null;
        }
        return emittingInstanceOrClass.getClass().equals(Class.class) ?
                ((Class<?>) emittingInstanceOrClass).getName() :
                emittingInstanceOrClass.getClass().getName();
    }

    private static void consume(Object event) {
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

    public static class JkLogEvent implements Serializable {

        private JkLogEvent(String emittingClassName, Type type, String message, Runnable task, long duration) {
            this.emittingClassName = emittingClassName;
            this.type = type;
            this.message = message;
            this.task = task;
            this.duration = duration;
        }

        static JkLogEvent regular(Object emittingInstanceOrClass, Type type, String message) {
            return new JkLogEvent(emittingInstance(emittingInstanceOrClass), type, message, null, 0);
        }

        static JkLogEvent endTask(Object emittingInstanceOrClass, long duration) {
            return new JkLogEvent(emittingInstance(emittingInstanceOrClass), Type.END_TASK, "", null, duration);
        }

        private final String emittingClassName; // In case it is emitted from a static method

        private final Type type;

        private final String message;

        private final Runnable task;

        private final long duration;

        public String emittingClassName() {
            return emittingClassName;
        }

        public Type type() {
            return type;
        }

        public String message() {
            return message;
        }

        public Runnable task() {
            return task;
        }

        public long durationMs() {
            return duration;
        }
    }

    public static Verbosity verbosity() {
        return verbosity;
    }

    public interface EventLogHandler extends Consumer<JkLogEvent> {

        OutputStream outStream();

        OutputStream errorStream();

    }


}
