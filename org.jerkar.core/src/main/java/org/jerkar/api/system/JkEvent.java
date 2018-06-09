package org.jerkar.api.system;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsThrowable;

import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public final class JkEvent implements Serializable {

    public enum Type {
        INFO, WARN, ERROR, TRACE, PROGRESS, START_TASK, END_TASK;
    }

    public enum Verbosity {
        MUTE, NORMAL, VERBOSE;
    }

    private static final Set<Consumer<JkEvent>> HANDLERS = new LinkedHashSet<>();

    private static OutputStream stream = JkUtilsIO.nopOuputStream();

    private static Verbosity verbosity = Verbosity.NORMAL;

    private static int currentNestedTaskLevel = 0;

    private static final ThreadLocal<LinkedList<Long>> START_TIMES = new ThreadLocal<>();

    private final String emittingClassName; // In case it is emitted from a static method

    private final Type type;

    private final String message;

    private final int nestedLevel;

    static {
        START_TIMES.set(new LinkedList<>());
    }

    private JkEvent(String emittingClassName, Type type, String message, int nestedLevel) {
        this.emittingClassName = emittingClassName;
        this.type = type;
        this.message = message;
        this.nestedLevel = nestedLevel;
    }

    private JkEvent(Object emittingInstanceOrClass, Type type, String message) {
        this(emittingInstance(emittingInstanceOrClass), type, message, currentNestedTaskLevel);
    }

    public static void register(Consumer<JkEvent> handler) {
        HANDLERS.add(handler);
    }

    public static void setVerbosity(Verbosity verbosityArg) {
        JkUtilsAssert.notNull(verbosityArg, "Verbosity can noot be set to null.");
        verbosity = verbosityArg;
    }

    public static List<Consumer<JkEvent>> handlers() {
        return Collections.unmodifiableList(new LinkedList<>(HANDLERS));
    }

    public static long getLastTaskStartTs() {
        final LinkedList<Long> times = START_TIMES.get();
        if (times.isEmpty()) {
            throw new IllegalStateException(
                    "This 'done' do no match to any 'start'. "
                            + "Please, use 'done' only to mention that the previous 'start' activity is done.");
        }
        return times.getLast();
    }

    private static void pollStartTs() {
        final LinkedList<Long> times = START_TIMES.get();
        if (times.isEmpty()) {
            throw new IllegalStateException(
                    "This 'done' do no match to any 'start'. "
                            + "Please, use 'done' only to mention that the previous 'start' activity is done.");
        }
        times.poll();
    }

    private static void startTimer() {
        LinkedList<Long> times = START_TIMES.get();
        times.push(System.currentTimeMillis());
    }

    public static void initializeInClassLoader(ClassLoader classLoader) {
        try {
            Class<?> targetClass = classLoader.loadClass(JkEvent.class.getName());
            Field handlerField = targetClass.getDeclaredField("HANDLERS");
            handlerField.setAccessible(true);
            Set<Consumer<JkEvent>> targetHandlers = (Set<Consumer<JkEvent>>) handlerField.get(null);
            targetHandlers.addAll(HANDLERS);
            JkUtilsReflect.setFieldValue(null, targetClass.getDeclaredField("currentNestedTaskLevel"),
                    currentNestedTaskLevel);
            JkUtilsReflect.setFieldValue(null, targetClass.getDeclaredField("verbosity"),
                    JkUtilsIO.cloneBySerialization(verbosity, classLoader));
            JkUtilsReflect.setFieldValue(null,targetClass.getDeclaredField("stream"), stream);
        } catch (ReflectiveOperationException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    public static OutputStream stream() {
        return stream;
    }

    public static void info(Object emittingInstanceOrClass, String message) {
        consume(new JkEvent(emittingInstanceOrClass, Type.INFO, message));
    }

    public static void info(String message) {
        consume(new JkEvent(null, Type.INFO, message));
    }

    public static void warn(Object emittingInstanceOrClass, String message) {
        consume(new JkEvent(emittingInstanceOrClass, Type.WARN, message));
    }

    public static void warn(String message) {
        consume(new JkEvent(null, Type.WARN, message));
    }

    public static void trace(Object emittingInstanceOrClass, String message) {
        if (verbosity() == Verbosity.VERBOSE) {
            consume(new JkEvent(emittingInstanceOrClass, Type.TRACE, message));
        }
    }

    public static void trace(String message) {
       trace(null,  message);
    }

    public static void error(Object emittingInstanceOrClass, String message) {
        consume(new JkEvent(emittingInstanceOrClass, Type.ERROR, message));
    }

    public static void error(String message) {
        consume(new JkEvent(null, Type.ERROR, message));
    }

    public static void start(Object emittingInstanceOrClass, String message) {
        startTimer();
        consume(new JkEvent(emittingInstanceOrClass, Type.START_TASK, message));
        currentNestedTaskLevel++;
    }

    public static void start(String message) {
        start(null, message);
    }

    public static void end(Object emittingInstanceOrClass, String message) {
        consume(new JkEvent(emittingInstanceOrClass, Type.END_TASK, message));
        pollStartTs();
        currentNestedTaskLevel--;
    }

    public static void end(String message) {
        end(null, message);
    }

    public static void progress(Object emittingInstanceOrClass, String unitProgressSymbol) {
        consume(new JkEvent(emittingInstanceOrClass, Type.PROGRESS, unitProgressSymbol));
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
        HANDLERS.forEach(handler -> {
            if (event.getClass().getClassLoader() != handler.getClass().getClassLoader()) {
                final Object evt = JkUtilsIO.cloneBySerialization(event, handler.getClass().getClassLoader());
                try {
                    Method accept = handler.getClass().getMethod("accept", evt.getClass());
                    accept.setAccessible(true);
                    accept.invoke(handler, evt);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            } else {
                handler.accept((JkEvent) event);
            }

        });
    }

    public Object emittingClassName() {
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

    public static Verbosity verbosity() {
        return verbosity;
    }


}
