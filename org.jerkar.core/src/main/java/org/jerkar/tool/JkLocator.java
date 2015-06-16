package org.jerkar.tool;

import java.io.File;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Provides location related to the running Jerkar instance.
 * 
 * @author Jerome Angibaud
 */
public final class JkLocator {

	// cache
	private static File JERKAR_JAR_FILE;

	private static String version;

	public static File jerkarJarFile() {
		if (JERKAR_JAR_FILE != null) {
			return JERKAR_JAR_FILE;
		}
		for (final File file : JkClassLoader.current().childClasspath()) {
			try {
				// TODO not optimized. Should be implemented on the JkClasspath class.
				JkClassLoader.system().parent().child(file).classloader().loadClass(Main.class.getName());
				JERKAR_JAR_FILE = file;
				return file;
			} catch (final ClassNotFoundException e) {
				// Class just not there
			}
		}
		throw new IllegalStateException("Main not found in classpath");
	}



	/**
	 * Returns the directory where is installed the running Jerkar instance.
	 */
	public static File jerkarHome() {
		return jerkarJarFile().getParentFile();
	}

	/**
	 * Returns the temporary directory used by Jerkar. This directory can contains
	 * caches for example.
	 */
	public static File jerkarTempDir() {
		final File result = new File(jerkarHome(),"temp");
		if (!result.exists()) {
			result.mkdirs();
		}
		return result;
	}

	public static File jerkarUserHome() {
		final File result = new File(JkUtilsFile.userHome(),".jerkar");
		if (!result.exists()) {
			JkLog.info("Create Jerkar user directory : " + result.getPath());
			result.mkdirs();
		}
		return result;
	}

	/**
	 * Returns the directory where lie optional libraries.
	 */
	public static File libExtDir() {
		return new File(jerkarHome(), "libs/ext");
	}

	public static String jerkarVersion() {
		if (JkUtilsString.isBlank(version)) {
			final Class<?> clazz = JkLocator.class;
			final String className = clazz.getSimpleName() + ".class";
			final String classPath = clazz.getResource(className).toString();
			if (!classPath.startsWith("jar")) {
				// Class not from JAR
				final String relativePath = clazz.getName().replace('.', File.separatorChar) + ".class";
				final String classFolder = classPath.substring(0, classPath.length() - relativePath.length() - 1);
				final String manifestPath = classFolder + "/META-INF/MANIFEST.MF";
				JkLog.trace("manifestPath=" +  manifestPath);
				version = readVersionFrom(manifestPath);
			} else {
				final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
				JkLog.trace("manifestPath=" + manifestPath);
				version = readVersionFrom(manifestPath);
			}
		}
		return version;
	}

	private static String readVersionFrom(String manifestPath) {
		Manifest manifest = null;
		try {
			manifest = new Manifest(new URL(manifestPath).openStream());
			final Attributes attrs = manifest.getMainAttributes();
			return attrs.getValue("Version");
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}


