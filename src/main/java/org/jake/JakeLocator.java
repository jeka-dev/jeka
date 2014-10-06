package org.jake;

import java.io.File;

/**
 * Provides location related to running Jake instance.
 * 
 * @author Jerome Angibaud
 */
public final class JakeLocator {

	// cache
	private static File JAKE_JAR_FILE;

	public static File jakeJarFile() {
		if (JAKE_JAR_FILE != null) {
			return JAKE_JAR_FILE;
		}
		//JAKE_JAR_FILE = JakeClassLoader.system().parent().createChild(file).classloader().loadClass(JakeMain.class.getName());
		//return JAKE_JAR_FILE;
		for (final File file : JakeClassLoader.current().childClasspath()) {
			try {
				// TODO not optimized. Should be implemented on the JakeClasspath class.
				JakeClassLoader.system().parent().createChild(file).classloader().loadClass(Main.class.getName());
				JAKE_JAR_FILE = file;
				return file;
			} catch (final ClassNotFoundException e) {
				// Class just not there
			}
		}
		throw new IllegalStateException("JakeLauncher not found in classpath");
	}

	/**
	 * Returns the directory where is installed the running Jake instance.
	 */
	public static File jakeHome() {
		return jakeJarFile().getParentFile();
	}

	/**
	 * Returns the directory where lie optional libraries.
	 */
	public static File optionalLibsDir() {
		return new File(jakeHome(), "libs/optional");
	}


}
