package org.jerkar.tool;

import java.io.File;

import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;

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
	 * Returns the temporary directory used by Jerkar for its internal use.
	 */
	public static File jerkarTempDir() {
		final File result = new File(jerkarUserHome(),"temp");
		if (!result.exists()) {
			result.mkdirs();
		}
		return result;
	}

	/**
	 * Returns the jerkar user directory.
	 */
	public static File jerkarUserHome() {
		final File result = new File(JkUtilsFile.userHome(),".jerkar");
		if (!result.exists()) {
			JkLog.info("Create Jerkar user directory : " + result.getPath());
			result.mkdirs();
		}
		return result;
	}

	/**
	 * Returns the location of the artifact repository cache.
	 */
	public static File jerkarRepositoryCache() {
		final String jerkarCacheOption = JkOptions.get("jerkar.repository.cache");
		final File result;
		if (!JkUtilsString.isBlank(jerkarCacheOption)) {
			result = new File(jerkarCacheOption);
		} else {
			result = new File(jerkarUserHome(), "repo-cache");
		}
		result.mkdirs();
		return result;
	}

	/**
	 * Returns the directory where lie optional libraries.
	 */
	public static File libExtDir() {
		return new File(jerkarHome(), "libs/ext");
	}


}


