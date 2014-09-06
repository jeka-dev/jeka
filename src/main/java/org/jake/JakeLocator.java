package org.jake;

import java.io.File;

import org.jake.java.JakeClassloader;

public class JakeLocator {

	// cache
	private static File JAKE_JAR_FILE;

	public static File jakeJarFile() {
		if (JAKE_JAR_FILE != null) {
			return JAKE_JAR_FILE;
		}
		for (final File file : JakeClassloader.current().getChildClasspath()) {
			try {
				JakeClassloader.system().parent().createChild(file).classloader().loadClass(JakeLauncher.class.getName());
				JAKE_JAR_FILE = file;
				return file;
			} catch (final ClassNotFoundException e) {
				// Class just not there
			}
		}
		throw new IllegalStateException("JakeLauncher not found in classpath");
	}

	public static File jakeHome() {
		return jakeJarFile().getParentFile();
	}




}
