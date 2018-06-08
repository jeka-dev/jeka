package org.jerkar.api.system;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsThrowable;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;

public final class JkEvent {

    public enum Type {
        INFO, WARN, ERROR, TRACE, PROGRESS, START_TASK, END_TASK;
    }

    public enum Verbosity {
        MUTE, NORMAL, VERBOSE;
    }

    private static final Set<JkEventHandler> HANDLERS = new LinkedHashSet<>();

    private static OutputStream stream = JkUtilsIO.nopOuputStream();

    private static Verbosity verbosity;

    private static int currentNestedTaskLevel = 0;

    private final Object emittingInstance;

    private final Object emittingClass; // In case it is emitted from a static method

    private final Type type;

    private final String message;

    private final int nestedLevel;

    private JkEvent(Object emittingInstance, Object emittingClass, Type type, String message, int nestedLevel) {
        this.emittingInstance = emittingInstance;
        this.emittingClass = emittingClass;
        this.type = type;
        this.message = message;
        this.nestedLevel = nestedLevel;
    }

    private JkEvent(Object emittingInstanceOrClass, Type type, String message) {
        this(emittingInstance(emittingInstanceOrClass), emittingClass(emittingInstanceOrClass), type, message, currentNestedTaskLevel);
    }

    public static void register(JkEventHandler handler) {
        HANDLERS.add(handler);
    }

    public static void setVerbosity(Verbosity verbosityArg) {
        JkUtilsAssert.notNull(verbosityArg, "Verbosity can noot be set to null.");
        verbosity = verbosityArg;
    }

    public static List<JkEventHandler> handlers() {
        return Collections.unmodifiableList(new LinkedList<>(HANDLERS));
    }

    public static void initializeInClassLoader(ClassLoader classLoader) {
        try {
            Class<?> targetClass = classLoader.loadClass(JkEvent.class.getName());
            Field handlerField = targetClass.getDeclaredField("HANDLERS");
            handlerField.setAccessible(true);
            Set<JkEventHandler> targetHandlers = (Set<JkEventHandler>) handlerField.get(null);
            targetHandlers.addAll(HANDLERS);
            JkUtilsReflect.setFieldValue(null, targetClass.getDeclaredField("currentNestedTaskLevel"),
                    currentNestedTaskLevel);
            JkUtilsReflect.setFieldValue(null, targetClass.getDeclaredField("verbosity"), verbosity);
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
        consume(new JkEvent(emittingInstanceOrClass, Type.TRACE, message));
    }

    public static void trace(String message) {
        consume(new JkEvent(null, Type.TRACE, message));
    }

    public static void error(Object emittingInstanceOrClass, String message) {
        consume(new JkEvent(emittingInstanceOrClass, Type.ERROR, message));
    }

    public static void error(String message) {
        consume(new JkEvent(null, Type.ERROR, message));
    }

    public static void start(Object emittingInstanceOrClass, String message) {
        currentNestedTaskLevel++;
        consume(new JkEvent(emittingInstanceOrClass, Type.START_TASK, message));
    }

    public static void start(String message) {
        currentNestedTaskLevel++;
        consume(new JkEvent(null, Type.START_TASK, message));
    }

    public static void end(Object emittingInstanceOrClass, String message) {
        consume(new JkEvent(emittingInstanceOrClass, Type.END_TASK, message));
        currentNestedTaskLevel--;
    }

    public static void end(String message) {
        consume(new JkEvent(null, Type.END_TASK, message));
        currentNestedTaskLevel--;
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

    private static Object emittingInstance(Object emittingInstanceOrClass) {
        if (emittingInstanceOrClass == null) {
            return null;
        }
        return emittingInstanceOrClass.getClass().equals(Class.class) ? null : emittingInstanceOrClass;
    }

    private static void consume(JkEvent event) {
        HANDLERS.forEach(handler -> handler.handle(event));
    }

    public Object emittingInstance() {
        return emittingInstance;
    }

    public Object emittingClass() {
        return emittingClass;
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

    public interface JkEventHandler {

        void handle(JkEvent event);

        OutputStream stream();
    }
}
