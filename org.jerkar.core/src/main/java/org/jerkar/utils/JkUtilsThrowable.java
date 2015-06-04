package org.jerkar.utils;

/**
 * Utility class to deal with {@link Throwable}
 */
public final class JkUtilsThrowable {

	private JkUtilsThrowable() {
		// prevent instantiation
	}

	/**
	 * Returns the specified exception itself if it is an unchecked exception (extending {@link RuntimeException}).
	 * Otherwise returns a new {@link RuntimeException} wrapping the exception passed as argument.
	 */
	public static RuntimeException unchecked(Exception e) {
		if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		}
		return new RuntimeException(e);
	}

}
