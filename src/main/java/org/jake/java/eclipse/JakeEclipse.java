package org.jake.java.eclipse;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeLocator;
import org.jake.java.JakeJavaDependencyResolver;
import org.jake.java.JakeLocalDependencyResolver;

public class JakeEclipse {

	public static boolean isDotClasspathPresent(File projectFolder) {
		return new File(projectFolder, ".classpath").exists();
	}

	private static List<ResourcePath> buildPath(File projectFolder, File jakeJarFile) {
		final EclipseClasspath eclipseClasspath = EclipseClasspath.fromFile(new File(projectFolder, ".classpath"), jakeJarFile);
		return eclipseClasspath.getLibEntries();
	}

	public static JakeJavaDependencyResolver dependencyResolver(File projectFolder) {
		final List<ResourcePath> path = buildPath(projectFolder, JakeLocator.getJakeJarFile());
		final List<File> compileAndRuntimeLibPath = new LinkedList<File>();
		final List<File> runtimeOnlyLibPath = new LinkedList<File>();
		final List<File> testLibPath = new LinkedList<File>();
		final List<File> compileOnlyLibPath = new LinkedList<File>();
		for (final ResourcePath resourcePath : path) {
			final File file = resourcePath.toFile(projectFolder);
			if (resourcePath.isTestScoped()) {
				testLibPath.add(file);
			} else if (isCompileOnly(file)) {
				compileOnlyLibPath.add(file);
				testLibPath.add(file);
			} else {
				compileAndRuntimeLibPath.add(file);
			}
		}
		return new JakeLocalDependencyResolver(compileAndRuntimeLibPath, runtimeOnlyLibPath,
				testLibPath, compileOnlyLibPath);
	}

	private static final boolean isCompileOnly(File file) {
		if (file.getName().toLowerCase().equals("lombok.jar")) {
			return true;
		}
		return false;
	}

}
