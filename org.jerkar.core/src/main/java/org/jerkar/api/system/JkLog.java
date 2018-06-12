package org.jerkar.api.system;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsThrowable;

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public final class JkLog implements Serializable {

    public enum Type {
        INFO, WARN, ERROR, TRACE, PROGRESS, START_TASK, END_TASK;
    }

    public enum Verbosity {
        MUTE, NORMAL, VERBOSE;
    }

    private static Consumer<JkLogEvent> consumer;

    private static PrintStream stream = JkUtilsIO.nopPrintStream();

    private static PrintStream errorStream = JkUtilsIO.nopPrintStream();

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

    private static void removeLastStartTs() {
        final LinkedList<Long> times = START_TIMES.get();
        if (times.isEmpty()) {
            throw new IllegalStateException(
                    "This 'done' do no match to any 'start'. "
                            + "Please, use 'done' only to mention that the previous 'start' activity is done.");
        }
        times.removeLast();
    }

    private static void startTimer() {
        LinkedList<Long> times = START_TIMES.get();
        times.add(System.nanoTime());
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

    public static PrintStream stream() {
        return stream;
    }

    public static PrintStream errorStream() {
        return errorStream;
    }

    public static void info(Object emittingInstanceOrClass, String message) {
        consume(new JkLogEvent(emittingInstanceOrClass, Type.INFO, message));
    }

    public static void info(String message) {
        info(null, message);
    }

    public static void warn(Object emittingInstanceOrClass, String message) {
        consume(new JkLogEvent(emittingInstanceOrClass, Type.WARN, message));
    }

    public static void warn(String message) {
        warn(null, message);
    }

    public static void trace(Object emittingInstanceOrClass, String message) {
        if (verbosity() == Verbosity.VERBOSE) {
            consume(new JkLogEvent(emittingInstanceOrClass, Type.TRACE, message));
        }
    }

    public static void trace(String message) {
       trace(null,  message);
    }

    public static void error(Object emittingInstanceOrClass, String message) {
        consume(new JkLogEvent(emittingInstanceOrClass, Type.ERROR, message));
    }

    public static void error(String message) {
        error(null,message);
    }

    public static void start(Object emittingInstanceOrClass, String message) {
        startTimer();
        consume(new JkLogEvent(emittingInstanceOrClass, Type.START_TASK, message));
        currentNestedTaskLevel++;
    }

    public static void start(String message) {
        start(null, message);
    }

    public static void end(Object emittingInstanceOrClass, String message) {
        currentNestedTaskLevel--;
        consume(new JkLogEvent(emittingInstanceOrClass, Type.END_TASK, message));
        removeLastStartTs();
    }

    public static void end(String message) {
        end(null, message);
    }

    public static void progress(Object emittingInstanceOrClass, String unitProgressSymbol) {
        progress(null, unitProgressSymbol);
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
        if (event.getClass().getClassLoader() != consumer.getClass().getClassLoader()) {
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

        private JkLogEvent(String emittingClassName, Type type, String message, int nestedLevel) {
            this.emittingClassName = emittingClassName;
            this.type = type;
            this.message = message;
            this.nestedLevel = nestedLevel;
        }

        private JkLogEvent(Object emittingInstanceOrClass, Type type, String message) {
            this(emittingInstance(emittingInstanceOrClass), type, message, currentNestedTaskLevel);
        }

        private final String emittingClassName; // In case it is emitted from a static method

        private final Type type;

        private final String message;

        private final int nestedLevel;

        public String emittingClassName() {
            return emittingClassName;
        }

        public Type type() {
            return type;
        }

        public String message() {
            return message;
        }

        public int nestedLevel() {
            return nestedLevel;
        }
    }

    public static Verbosity verbosity() {
        return verbosity;
    }

    public interface EventLogHandler extends Consumer<JkLogEvent> {

        PrintStream outStream();

        PrintStream errorStream();

    }


}
