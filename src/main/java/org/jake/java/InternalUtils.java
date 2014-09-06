package org.jake.java;

import java.io.File;
import java.io.IOException;

class InternalUtils {

	public static File toolsJar() {
		final String jdkLocation = System.getProperty("java.home");
		final File javaHome = new File(jdkLocation);
		try {
			return new File(javaHome, "../lib/tools.jar").getCanonicalFile();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}
