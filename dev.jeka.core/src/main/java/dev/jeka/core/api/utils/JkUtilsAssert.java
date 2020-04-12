package dev.jeka.core.api.utils;

/**
 * Utility class for dealing with assertions.
 */
public final class JkUtilsAssert {

    public static void argument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void state(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

}
