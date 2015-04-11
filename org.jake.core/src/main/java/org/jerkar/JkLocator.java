package org.jerkar;

import java.io.File;

import org.apache.ivy.Ivy;

/**
 * Provides location related to the running Jake instance.
 * 
 * @author Jerome Angibaud
 */
public final class JkLocator {

	// cache
	private static File JAKE_JAR_FILE;

	private static File IVY_JAR_FILE;

	public static File jakeJarFile() {
		if (JAKE_JAR_FILE != null) {
			return JAKE_JAR_FILE;
		}
		//JAKE_JAR_FILE = JkClassLoader.system().parent().createChild(file).classloader().loadClass(JakeMain.class.getName());
		//return JAKE_JAR_FILE;
		for (final File file : JkClassLoader.current().childClasspath()) {
			try {
				// TODO not optimized. Should be implemented on the JkClasspath class.
				JkClassLoader.system().parent().createChild(file).classloader().loadClass(Main.class.getName());
				JAKE_JAR_FILE = file;
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
		throw new IllegalStateException("JakeLauncher not found in classpath");
	}

	/**
	 * Returns the directory where is installed the running Jake instance.
	 */
	public static File jakeHome() {
		return jakeJarFile().getParentFile();
	}

	public static File jakeUserHome() {
		final File result = new File(System.getProperty("user.home"),".jake");
		if (result.exists()) {
			JkLog.info("Create Jake user directory : " + result.getPath());
			result.mkdirs();
		}
		return result;
	}

	/**
	 * Returns the directory where lie optional libraries.
	 */
	public static File optionalLibsDir() {
		return new File(jakeHome(), "libs/optional");
	}

	/**
	 * Returns the directory where lie optional libraries.
	 */
	public static File libExtDir() {
		return new File(jakeHome(), "libs/ext");
	}




}
