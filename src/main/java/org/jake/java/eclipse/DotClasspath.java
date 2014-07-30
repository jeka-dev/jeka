package org.jake.java.eclipse;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jake.file.JakeDir;
import org.jake.file.JakeDirSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DotClasspath {

	private final List<ClasspathEntry> classpathentries = new LinkedList<ClasspathEntry>();

	private DotClasspath(List<ClasspathEntry> classpathentries) {
		classpathentries.addAll(classpathentries);
	}

	public DotClasspath from(File dotClasspathFile) {
		return from(getDotClassPathAsDom(dotClasspathFile));
	}

	public DotClasspath from(Document document) {
		final NodeList nodeList = document.getElementsByTagName("classpathentry");
		final List<ClasspathEntry> classpathEntries = new LinkedList<ClasspathEntry>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			final Node node = nodeList.item(i);
			final Element element = (Element) node;
			classpathEntries.add(ClasspathEntry.from(element));
		}
		return new DotClasspath(classpathEntries);
	}

	private static Document getDotClassPathAsDom(File classpathFile) {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		if (!classpathFile.exists()) {
			throw new IllegalStateException(".classpath file not found.");
		}
		try {
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc;
			doc = dBuilder.parse(classpathFile);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (final Exception e) {
			throw new RuntimeException("Error while parsing .classpath file : ", e);
		}
	}

	public Sources sourceDirs(File baseDir, Sources.TestSegregator segregator) {
		final List<JakeDir> prods = new LinkedList<JakeDir>();
		final List<JakeDir> tests = new LinkedList<JakeDir>();
		for (final ClasspathEntry classpathEntry : classpathentries) {
			if (classpathEntry.kind.equals(ClasspathEntry.Kind.SRC) && !classpathEntry.isOptional()) {
				if (segregator.isTest(classpathEntry.path)) {
					tests.add(classpathEntry.srcAsJakeDir(baseDir));
				} else {
					prods.add(classpathEntry.srcAsJakeDir(baseDir));
				}
			}
		}
		return new Sources(JakeDirSet.of(prods), JakeDirSet.of(tests));
	}

	public List<Lib> libs(File baseFolder, Lib.ScopeSegregator libSegregator, Lib.ScopeSegregator conSegregator) {

	}


	private static class ClasspathEntry {

		public enum Kind {
			SRC, CON, LIB, UNKNOWN
		}

		private final Kind kind;

		private final String path;

		private final String excluding;

		private final String including;

		private final Map<String, String> attributes = new HashMap<String, String>();

		public ClasspathEntry(Kind kind, String path, String excluding, String including) {
			super();
			this.kind = kind;
			this.path = path;
			this.excluding = excluding;
			this.including = including;
		}

		public static ClasspathEntry from(Element classpathEntryEl) {
			final String kindString = classpathEntryEl.getAttribute("kind");
			final String path = classpathEntryEl.getAttribute("path");
			final String including = classpathEntryEl.getAttribute("including");
			final String excluding = classpathEntryEl.getAttribute("excluding");
			final Kind kind;
			if ("lib".equals(kindString)) {
				kind = Kind.LIB;
			} else if ("con".equals(kindString)) {
				kind = Kind.CON;
			} else if ("src".equals(kindString)) {
				kind = Kind.SRC;
			} else {
				kind = Kind.UNKNOWN;
			}
			final ClasspathEntry result = new ClasspathEntry(kind, path, excluding, including);
			final Element attributesEl = (Element) classpathEntryEl.getElementsByTagName("attributes");
			final NodeList nodeList = attributesEl.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				final Element attributeEl = (Element) nodeList.item(i);
				final String name = attributeEl.getAttribute("name");
				final String value = attributeEl.getAttribute("value");
				result.attributes.put(name, value);
			}
			return result;
		}

		public JakeDir srcAsJakeDir(File baseDir) {
			if (!this.kind.equals(Kind.SRC)) {
				throw new IllegalStateException("Can only get source dir from classpath entry of kind 'src'.");
			}
			final File dir = new File(baseDir, path);
			JakeDir jakeDir = JakeDir.of(dir);
			if (excluding != null) {
				final String[] patterns = excluding.split("|");
				jakeDir = jakeDir.exclude(patterns);
			}
			if (including != null) {
				final String[] patterns = including.split("|");
				jakeDir = jakeDir.exclude(patterns);
			}
			return jakeDir;
		}

		public boolean isOptional() {
			return "true".equals(this.attributes.get("optional"));
		}

		public List<File> conAsFiles(File baseDir, File jakeHome) {
			if (!this.kind.equals(Kind.CON)) {
				throw new IllegalStateException("Can only get files from classpath entry of kind 'con'.");
			}
			final File containerDir = new File(jakeHome, "eclipse/containers");
			if (!containerDir.exists()) {
				return Collections.emptyList();
			}
			final String folderName = path.replace('/', '_').replace('\\', '_');
			final File conFolder = new File(containerDir, folderName);
			if (!conFolder.exists()) {
				return Collections.emptyList();
			}
			final JakeDir dirView = JakeDir.of(conFolder).include("**/*.jar");
			final List<File> result = new LinkedList<File>();
			for (final File file : dirView.listFiles()) {
				result.add(file);
			}
			return result;
		}

		public File libAsFile(File baseDir) {
			final String projectName;
			final String pathInProject;
			if (path.startsWith("/")) {
				final int secondSlashIndex = path.indexOf("/", 1);
				projectName = path.substring(1, secondSlashIndex);
				pathInProject = path.substring(secondSlashIndex+1);
				final File otherProjectFolder = new File(baseDir.getParent(), projectName);
				return new File (otherProjectFolder, pathInProject);
			}
			return new File(baseDir, path);
		}

	}


}
