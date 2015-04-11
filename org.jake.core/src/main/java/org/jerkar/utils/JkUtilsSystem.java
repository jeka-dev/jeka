package org.jerkar.utils;

public final class JkUtilsSystem {

	private JkUtilsSystem() {}

	public static final boolean IS_WINDOWS = isWindows();

	private static final boolean isWindows() {
		final String osName = System.getProperty("os.name");
		if (osName == null) {
			return false;
		}
		return osName.startsWith("Windows");
	}


}
