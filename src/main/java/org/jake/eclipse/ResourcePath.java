package org.jake.eclipse;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

class ResourcePath {
	
	private final String projectName;
	
	private final String pathWithinProject;

	private ResourcePath(String projectName, String pathWithinProject) {
		super();
		this.projectName = projectName;
		this.pathWithinProject = pathWithinProject;
	}
	
	public static ResourcePath fromClassentry(String classpathEntry) {
		final String projectName;
		final String pathInProject;
		if (classpathEntry.startsWith("/")) {
			int secondSlashIndex = classpathEntry.indexOf("/", 1);
			projectName = classpathEntry.substring(1, secondSlashIndex);
			pathInProject = classpathEntry.substring(secondSlashIndex+1);
		} else {
			projectName = null;
			pathInProject = classpathEntry;
		}
		return new ResourcePath(projectName, pathInProject);
	}
	
	public File toFile(File projectFolder) {
		File parent = projectFolder.getParentFile();
		if (projectName == null) {
			return new File(pathWithinProject);
		}
		else {
			File otherProjectFolder = new File(parent, projectName);
			return new File (otherProjectFolder, pathWithinProject);
		}
	}
	
	public static List<File> toFiles(Iterable<ResourcePath> resourcePaths, File projectFolder) {
		List<File> result = new LinkedList<File>();
		for(ResourcePath resourcePath : resourcePaths) {
			result.add(resourcePath.toFile(projectFolder));
		}
		return result;
		
	}
	
	

}
