package org.jake.java.eclipse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class Project {

	public static Project of(File dotProjectFile) {
		final Document document = getDotProjectAsDom(dotProjectFile);
		return from(document);
	}

	public static Project findProjectNamed(File parent, String projectName) {
		for (final File file : parent.listFiles()) {
			final File dotProject = new File(file, ".project");
			if( !(dotProject.exists())) {
				continue;
			}
			final Project project = Project.of(dotProject);
			if (projectName.equals(project.name)) {
				return project;
			}
		}
		return null;
	}

	public static Map<String, File> findProjects(File parent) {
		final Map<String, File> map = new HashMap<String, File>();
		for (final File file : parent.listFiles()) {
			final File dotProject = new File(file, ".project");
			if( !(dotProject.exists())) {
				continue;
			}
			final Project project = Project.of(dotProject);
			map.put(project.name, file);
		}
		return map;
	}

	final String name;

	private Project(String name) {
		this.name = name;
	}

	private static Project from(Document document) {
		final NodeList nodeList = document.getElementsByTagName("name");
		final String name = nodeList.item(0).getNodeValue();
		return new Project(name);
	}

	private static Document getDotProjectAsDom(File dotProjectFile) {
		if (!dotProjectFile.exists()) {
			throw new IllegalStateException(dotProjectFile.getAbsolutePath() + " file not found.");
		}
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc;
			doc = dBuilder.parse(dotProjectFile);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (final Exception e) {
			throw new RuntimeException("Error while parsing .classpath file : ", e);
		}
	}

}
