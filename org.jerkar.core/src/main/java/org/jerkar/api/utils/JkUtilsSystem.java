package org.jerkar.api.utils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

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

	/**
	 * Returns the classpath of this classloader without mentioning classpath of
	 * the parent classloaders.
	 */
	public static List<File> classloaderEntries(URLClassLoader classLoader) {
		final List<File> result = new ArrayList<File>();
		for (final URL url : classLoader.getURLs()) {
			result.add(new File(url.getFile().replaceAll("%20", " ")));
		}
		return result;
	}


}
