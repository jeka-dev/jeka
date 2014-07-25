package org.jake.java.eclipse;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jake.file.JakeDirView;

class ResourcePath {
	
	private final String projectName;
	
	private final String pathWithinProject;
	
	private final boolean testScoped;
	
	private final File file;

	private ResourcePath(String projectName, String pathWithinProject, File file, boolean testScoped) {
		super();
		this.projectName = projectName;
		this.pathWithinProject = pathWithinProject;
		this.testScoped = testScoped;
		this.file = file;
	}
	
	public static ResourcePath fromClasspathEntryLib(String classpathEntry, boolean testScoped) {
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
		return new ResourcePath(projectName, pathInProject, null, testScoped);
	}
	
	public static List<ResourcePath> fromClasspathEntryCon(File jakeJarfile, String path) {
		File containerDir = new File(jakeJarfile.getParent(), "eclipse/containers");
		if (!containerDir.exists()) {
			return Collections.emptyList();
		}
		String folderName = path.replace('/', '_').replace('\\', '_');
		File conFolder = new File(containerDir, folderName);
		if (!conFolder.exists()) {
			return Collections.emptyList();
		}
		JakeDirView dirView = JakeDirView.of(conFolder).include("**/*.jar");
		boolean testScoped = path.toLowerCase().contains("junit");
		List<ResourcePath> result = new LinkedList<ResourcePath>();
		for (File file : dirView.listFiles()) {
			result.add(new ResourcePath(null, null, file, testScoped));
		}
		return result;
	}
	
	
	
	public File toFile(File projectFolder) {
		if (file != null) {
			return file;
		}
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
	
	public boolean isTestScoped() {
		return testScoped;
	}
	
	

}
