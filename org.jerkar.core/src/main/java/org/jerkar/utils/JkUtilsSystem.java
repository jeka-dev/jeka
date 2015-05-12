package org.jerkar.utils;

/**
 * Utility class to deal with the underlying system.
 * 
 * @author Jerome Angibaud
 */
public final class JkUtilsSystem {

	private JkUtilsSystem() {}

	/**
	 * Flag valuing <code>true</code> if the running underlying system is Windows.
	 */
	public static final boolean IS_WINDOWS = isWindows();

	private static final boolean isWindows() {
		final String osName = System.getProperty("os.name");
		if (osName == null) {
			return false;
		}
		return osName.startsWith("Windows");
	}


}
