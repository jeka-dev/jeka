package org.jake.utils;

public class JakeUtilsAssert {

	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

}
