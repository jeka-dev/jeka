package org.jerkar;

import java.io.File;

import org.apache.ivy.Ivy;

/**
 * Provides location related to the running Jerkar instance.
 * 
 * @author Jerome Angibaud
 */
public final class JkLocator {

	// cache
	private static File JERKAR_JAR_FILE;

	private static File IVY_JAR_FILE;

	public static File jerkararFile() {
		if (JERKAR_JAR_FILE != null) {
			return JERKAR_JAR_FILE;
		}
		for (final File file : JkClassLoader.current().childClasspath()) {
			try {
				// TODO not optimized. Should be implemented on the JkClasspath class.
				JkClassLoader.system().parent().createChild(file).classloader().loadClass(Main.class.getName());
				JERKAR_JAR_FILE = file;
				return file;
			} catch (final ClassNotFoundException e) {
				// Class just not there
			}
		}
		throw new IllegalStateException("Main not found in classpath");
	}

	public static File ivyJarFile() {
		if (IVY_JAR_FILE != null) {
			return IVY_JAR_FILE;
		}
		for (final File file : JkClassLoader.current().childClasspath()) {
			try {
				JkClassLoader.system().parent().createChild(file).classloader().loadClass(Ivy.class.getName());
				IVY_JAR_FILE = file;
				return file;
			} catch (final ClassNotFoundException e) {
				// Class just not there
			}
		}
		throw new IllegalStateException("Ivy not found in classpath");
	}

	/**
	 * Returns the directory where is installed the running Jerkar instance.
	 */
	public static File jerkarHome() {
		return jerkararFile().getParentFile();
	}

	public static File jerkarUserHome() {
		final File result = new File(System.getProperty("user.home"),".jerkar");
		if (result.exists()) {
			JkLog.info("Create Jerkar user directory : " + result.getPath());
			result.mkdirs();
		}
		return result;
	}

	/**
	 * Returns the directory where lie optional libraries.
	 */
	public static File optionalLibsDir() {
		return new File(jerkarHome(), "libs/optional");
	}

	/**
	 * Returns the directory where lie optional libraries.
	 */
	public static File libExtDir() {
		return new File(jerkarHome(), "libs/ext");
	}




}
