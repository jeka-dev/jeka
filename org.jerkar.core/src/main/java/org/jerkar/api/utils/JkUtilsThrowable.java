package org.jerkar.api.utils;

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
    public static RuntimeException unchecked(Exception e, String message) {

        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(message, e);
    }

    public static boolean nestedContains(Exception e, Class<Exception> exceptionClass,
            String message) {
        if (e.getClass().equals(exceptionClass) && message.equals(e.getMessage())) {
            return true;
        }
        if (e.getCause() == null) {
            return false;
        }
        return nestedContains((Exception) e.getCause(), exceptionClass, message);
    }

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

}
