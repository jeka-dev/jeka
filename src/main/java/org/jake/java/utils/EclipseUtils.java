package org.jake.java.utils;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jake.file.FileList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class EclipseUtils {
	
	private EclipseUtils() {}
	
	public static FileList getLibClasspathEntry(File projectDir) {
		FileList result = FileList.empty();
		Document document = getDotClassPathAsDom(projectDir);
		NodeList nodeList = document.getElementsByTagName("classpathentry");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Element element = (Element) node;
			if (element.getAttribute("kind").equals("lib")) {
				String path = element.getAttribute("path");
				File file = new File(path);
				if (!file.isAbsolute()) {
					file = new File(projectDir.getParentFile(), path);
				}
				result.addSingle(file);
			}
		}
		return result;
	} 
	
	private static Document getDotClassPathAsDom(File projectDir) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		File classpathFile = new File(projectDir, ".classpath");
		if (classpathFile.exists()) {
			throw new IllegalStateException(".classpath file not found.");
		}
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc;
			doc = dBuilder.parse(classpathFile);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			throw new RuntimeException("Error while parsing .classpath file : ", e);
		
		}
	}

}
