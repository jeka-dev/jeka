package org.jerkar.utils;

/**
 * Utility class for dealing with generic Object class instances.
 * 
 * @author Jerome Angiabud
 */
public final class JkUtilsObject {

	/**
	 * Returns the object1 if object1 is not <code>null</code>, otherwise returns object2.
	 */
	public static <T> T firstNonNull(T object1, T object2) {
		if (object1 == null) {
			if (object2 == null) {
				throw new IllegalArgumentException("Both objects can't be null.");
			}
			return object2;
		}
		return object1;
	}

	/**
	 * Returns <code>true</true> if both object are <code>null</code> or the two obects are equals.
	 * Returns <code>false</code> otherwise.
	 */
	public static boolean equals(Object object1, Object object2) {
		if (object1 == null) {
			if (object2 == null) {
				return true;
			}
			return false;
		}
		return object1.equals(object2);
	}

	/**
	 * Returns the hash code of the specified object or 0 if it's <code>null</code>.
	 */
	public static int hashCode(Object object) {
		if (object == null) {
			return 0;
		}
		return object.hashCode();
	}

	/**
	 * Null safe for {@link Object#toString()}. If the specified object is <code>null</code> than this method returns "null".
	 */
	public static String toString(Object object) {
		if (object == null) {
			return "null";
		}
		return object.toString();
	}


}
