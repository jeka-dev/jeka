package org.jake.eclipse;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class JakeEclipse {
	
	public static boolean isDotClasspathPresent(File projectFolder) {
		return new File(projectFolder, ".classpath").exists();
	}
	
	public static List<File> buildPath(File projectFolder) {
		List<File> result = new LinkedList<File>();
		EclipseClasspath eclipseClasspath = EclipseClasspath.fromFile(new File(projectFolder, ".classpath"));
		List<ResourcePath> paths = eclipseClasspath.getLibEntries();
		result.addAll(ResourcePath.toFiles(paths, projectFolder));
		return result;
	}
	
	

}
