package org.jake;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.jake.java.utils.JakeUtilsClassloader;
import org.jake.utils.JakeUtilsIterable;

public class JakeLocator {
	
	// cache
	private static File JAKE_JAR_FILE;
	
	public static File getJakeJarFile() {
		if (JAKE_JAR_FILE != null) {
			return JAKE_JAR_FILE;
		}
		URL[] urls = JakeUtilsClassloader.current().getURLs();
		for (URL url : urls) {
			File file = new File(url.getFile());
			URLClassLoader classLoader = JakeUtilsClassloader.createFrom(
					JakeUtilsIterable.single(file), ClassLoader
							.getSystemClassLoader().getParent());
			try {
				classLoader.loadClass(JakeLauncher.class.getName());
				JAKE_JAR_FILE = file;
				return file;
			} catch (ClassNotFoundException e) {
				// Class just not there
			}
		}
		throw new IllegalStateException("JakeLauncher not found in classpath");
	}


}
