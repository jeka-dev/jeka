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


}
