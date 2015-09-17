package org.jerkar.api.utils;

import java.io.File;
import java.io.IOException;

public final class JkUtilsJdk {

	public static File toolsJar() {
		final String jdkLocation = System.getProperty("java.home");
		final File javaHome = new File(jdkLocation);
		try {
			return new File(javaHome, "../lib/tools.jar").getCanonicalFile();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String runningJavaVersion() {
		final String fullVersion = System.getProperty("java.version");
		final int index = fullVersion.lastIndexOf(".");
		return fullVersion.substring(0, index);
	}

}
