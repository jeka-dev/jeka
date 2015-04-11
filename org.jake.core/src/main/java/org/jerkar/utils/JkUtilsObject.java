package org.jerkar.utils;

public final class JkUtilsObject {

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
