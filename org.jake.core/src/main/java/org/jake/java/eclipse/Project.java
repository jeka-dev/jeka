package org.jake.java.eclipse;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
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

	final Set<String> natures;

	private Project(String name, Set<String> natures) {
		this.name = name;
		this.natures = Collections.unmodifiableSet(natures);
	}

	private static Project from(Document document) {
		final NodeList nodeList = document.getElementsByTagName("name");
		final Node node = nodeList.item(0);
		final String name = node.getTextContent();
		final Set<String> natures = new HashSet<String>();
		final NodeList natureNodes = document.getElementsByTagName("nature");
		for (int i=0; i < natureNodes.getLength(); i++) {
			natures.add(natureNodes.item(i).getTextContent());
		}
		return new Project(name, natures);
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
