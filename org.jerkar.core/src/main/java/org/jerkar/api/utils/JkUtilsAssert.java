package org.jerkar.api.utils;

public class JkUtilsAssert {

	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void isTrue(boolean condition, String message) {
		if(!condition) {
			throw new IllegalArgumentException(message);
		}
	}

}
