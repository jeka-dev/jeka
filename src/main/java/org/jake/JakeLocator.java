package org.jake;

import java.io.File;

import org.jake.file.JakeDir;
import org.jake.java.JakeClassloader;
import org.jake.java.JakeClasspath;

public class JakeLocator {

	// cache
	private static File JAKE_JAR_FILE;

	public static File jakeJarFile() {
		if (JAKE_JAR_FILE != null) {
			return JAKE_JAR_FILE;
		}
		for (final File file : JakeClassloader.current().getChildClasspath()) {
			try {
				// TODO not optimized. Should be implemented on the JakeClasspath class.
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

	public static JakeClasspath baseBuildClasspath() {
		final File extDir = new File(jakeHome(), "ext/compile");
		JakeClasspath result = JakeClasspath.of(jakeJarFile());
		if (extDir.exists()) {
			result = result.with(JakeDir.of(extDir).include("**/*.jar"));
		}
		return result;
	}


}
