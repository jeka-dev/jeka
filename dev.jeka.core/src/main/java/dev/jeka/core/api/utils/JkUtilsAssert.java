package dev.jeka.core.api.utils;

/**
 * Utility class for dealing with assertions.
 */
public final class JkUtilsAssert {

    public static void argument(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    public static void state(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new IllegalStateException(String.format(message, args));
        }
    }

}
