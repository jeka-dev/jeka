package org.jake.java.eclipse;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jake.java.DependencyResolver;

public class JakeEclipse {
	
	public static boolean isDotClasspathPresent(File projectFolder) {
		return new File(projectFolder, ".classpath").exists();
	}
	
	private static List<File> buildPath(File projectFolder) {
		List<File> result = new LinkedList<File>();
		EclipseClasspath eclipseClasspath = EclipseClasspath.fromFile(new File(projectFolder, ".classpath"));
		List<ResourcePath> paths = eclipseClasspath.getLibEntries();
		result.addAll(ResourcePath.toFiles(paths, projectFolder));
		return result;
	}
	
	public static DependencyResolver dependencyResolver(File projectFolder) {
		final List<File> path = buildPath(projectFolder); 
		return new DependencyResolver() {
			
			@Override
			public List<File> test() {
				return path;
			}
			
			@Override
			public List<File> runtime() {
				return path;
			}
			
			@Override
			public List<File> compile() {
				return path;
			}
		};
	}
	
	

}
