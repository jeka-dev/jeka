package org.jake.utils;

public class JakeUtilsAssert {

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
