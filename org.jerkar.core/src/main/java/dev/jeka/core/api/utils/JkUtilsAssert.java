package dev.jeka.core.api.utils;

/**
 * Utility class for dealing with assertions.
 */
public final class JkUtilsAssert {

    /**
     * Throws an {@link IllegalArgumentException} if the specified object is <code>null</code>.
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the specified condition is <code>false</code>.
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

}
