package org.jerkar.tool;

import java.io.File;

import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;

/**
 * Provides location related to the running Jerkar instance.
 * 
 * @author Jerome Angibaud
 */
public final class JkLocator {



	/**
	 * Returns the directory where is installed the running Jerkar instance.
	 */
	public static File jerkarHome() {
		return JkClasspath.jerkarJarFile().getParentFile();
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


}


