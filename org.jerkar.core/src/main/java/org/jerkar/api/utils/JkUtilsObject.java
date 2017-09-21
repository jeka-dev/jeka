package org.jerkar.api.utils;

/**
 * Utility class for dealing with generic Object class instances.
 *
 * @author Jerome Angiabud
 */
public final class JkUtilsObject {

    /**
     * Returns the objects of the specified array that is not <code>null</code>.
     * Throw an {@link IllegalArgumentException} if all array elements are
     * <code>null</code> or the specified array is empty.
     */
    @SuppressWarnings("unchecked")
    public static <T> T firstNonNull(T... items) {
        for (final T item : items) {
            if (item != null) {
                return item;
            }
        }
        throw new IllegalArgumentException("Both objects can't be null.");
    }

    /**
     * Returns <code>true</true> if both object are <code>null</code> or the two
     * obects are equals. Returns <code>false</code> otherwise.
     */
    public static boolean equals(Object object1, Object object2) {
        if (object1 == null) {
            return object2 == null;
        }
        return object1.equals(object2);
    }

    /**
     * Returns the hash code of the specified object or 0 if it's
     * <code>null</code>.
     */
    public static int hashCode(Object object) {
        if (object == null) {
            return 0;
        }
        return object.hashCode();
    }

    /**
     * Null safe for {@link Object#toString()}. If the specified object is
     * <code>null</code> than this method returns "null".
     */
    public static String toString(Object object) {
        if (object == null) {
            return "null";
        }
        return object.toString();
    }

}
