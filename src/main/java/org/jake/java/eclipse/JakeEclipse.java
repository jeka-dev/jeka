package org.jake.java.eclipse;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.JakeLocator;
import org.jake.java.DependencyResolver;

public class JakeEclipse {
	
	public static boolean isDotClasspathPresent(File projectFolder) {
		return new File(projectFolder, ".classpath").exists();
	}
	
	private static List<ResourcePath> buildPath(File projectFolder, File jakeJarFile) {
		EclipseClasspath eclipseClasspath = EclipseClasspath.fromFile(new File(projectFolder, ".classpath"), jakeJarFile);
		return eclipseClasspath.getLibEntries();
	}
	
	public static DependencyResolver dependencyResolver(File projectFolder) {
		final List<ResourcePath> path = buildPath(projectFolder, JakeLocator.getJakeJarFile()); 
		final List<File> compilePath = new LinkedList<File>(); 
		final List<File> runtimePath = new LinkedList<File>();
		final List<File> testPath = new LinkedList<File>();
		for (ResourcePath resourcePath : path) {
			File file = resourcePath.toFile(projectFolder);
			if (resourcePath.isTestScoped()) {
				testPath.add(file);
			} else if (isCompileOnly(file)) {
				compilePath.add(file);
				testPath.add(file);
			} else {
				compilePath.add(file);
				runtimePath.add(file);
				testPath.add(file);
			}
		}
		return new DependencyResolver() {
			
			@Override
			public List<File> test() {
				return testPath;
			}
			
			@Override
			public List<File> runtime() {
				return runtimePath;
			}
			
			@Override
			public List<File> compile() {
				return compilePath;
			}
		};
	}
	
	private static final boolean isCompileOnly(File file) {
		if (file.getName().toUpperCase().equals("lombok.jar")) {
			return true;
		}
		return false;
	}
	
	

}
