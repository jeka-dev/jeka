package dev.jeka.core.api.utils;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class to deal with {@link Throwable}
 */
public final class JkUtilsThrowable {

    private JkUtilsThrowable() {
        // prevent instantiation
    }

    /**
     * Returns the specified exception itself if it is an unchecked exception
     * (extending {@link RuntimeException}). Otherwise returns a new
     * {@link RuntimeException} wrapping the exception passed as argument.
     */
    public static RuntimeException unchecked(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

    /**
     * Same as {@link #unchecked(Exception)} but specifying an error message.
     */
    public static RuntimeException unchecked(Throwable e, String message) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(message, e);
    }

    /**
     * Returns <code>true</code> if the specified throwable has the specified cause exception class
     * along the specified message in its chain.
     */
    public static boolean nestedContains(Throwable throwable, Class<Exception> exceptionClass,
            String message) {
        if (throwable.getClass().equals(exceptionClass) && message.equals(throwable.getMessage())) {
            return true;
        }
        if (throwable.getCause() == null) {
            return false;
        }
        return nestedContains(throwable.getCause(), exceptionClass, message);
    }

    /**
     * Returns <code>true</code> if the specified throwable has the specified cause exception class
     * in its chain.
     */
    public static boolean isInCause(Throwable throwable, Class<? extends Throwable> causeClass) {
        if (causeClass.isAssignableFrom(throwable.getClass())) {
            return true;
        }
        final Throwable cause = throwable.getCause();
        if (cause == null) {
            return false;
        }
        return isInCause(cause, causeClass);
    }

    public interface ThrowingConsumer<T, E extends Throwable> {
        void accept(T t) throws E;

        static <T, E extends Throwable> Consumer<T> unchecked(ThrowingConsumer<T, E> consumer) {
            return (t) -> {
                try {
                    consumer.accept(t);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

}
