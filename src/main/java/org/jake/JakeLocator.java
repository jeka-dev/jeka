package org.jake;

import java.io.File;

import org.jake.java.JakeClassLoader;

public class JakeLocator {

	// cache
	private static File JAKE_JAR_FILE;

	public static File jakeJarFile() {
		if (JAKE_JAR_FILE != null) {
			return JAKE_JAR_FILE;
		}
		for (final File file : JakeClassLoader.current().childClasspath()) {
			try {
				// TODO not optimized. Should be implemented on the JakeClasspath class.
				JakeClassLoader.system().parent().createChild(file).classloader().loadClass(JakeLauncher.class.getName());
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

	public static File optionalLibsDir() {
		return new File(jakeHome(), "libs/optional");
	}


}
