package org.jake.java.eclipse;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jake.JakeDir;
import org.jake.JakeDirSet;
import org.jake.JakeException;
import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.java.eclipse.Lib.Scope;
import org.jake.utils.JakeUtilsString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class DotClasspath {

	private final List<ClasspathEntry> classpathentries = new LinkedList<ClasspathEntry>();

	private DotClasspath(List<ClasspathEntry> classpathentries) {
		this.classpathentries.addAll(classpathentries);
	}

	public static DotClasspath from(File dotClasspathFile) {
		final Document document = getDotClassPathAsDom(dotClasspathFile);
		return from(document);
	}

	private static DotClasspath from(Document document) {
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
		if (!classpathFile.exists()) {
			throw new IllegalStateException(classpathFile.getAbsolutePath() + " file not found.");
		}
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
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

	public List<Lib> libs(File containersHome, File baseDir, Lib.ScopeSegregator libSegregator) {
		final List<Lib> result = new LinkedList<Lib>();
		for (final ClasspathEntry classpathEntry : classpathentries) {
			if (classpathEntry.kind.equals(ClasspathEntry.Kind.CON) ) {
				final Scope scope = libSegregator.scoprOfCon(classpathEntry.path);
				if (classpathEntry.path.startsWith(ClasspathEntry.JRE_CONTAINER_PREFIX)) {
					continue;
				}
				for (final File file : classpathEntry.conAsFiles(baseDir, containersHome)) {
					result.add(new Lib(file, scope));
				}
			} else if (classpathEntry.kind.equals(ClasspathEntry.Kind.LIB)) {
				final Scope scope = libSegregator.scopeOfLib(classpathEntry.path);
				result.add(new Lib(classpathEntry.libAsFile(baseDir), scope));
			} else if (classpathEntry.kind.equals(ClasspathEntry.Kind.VAR)) {
				final String var = JakeUtilsString.substringBeforeFirst(classpathEntry.path, "/");
				final String optionName = "eclipse.var." + var;
				final String varFile = JakeOptions.get(optionName);
				if (varFile == null) {
					throw new JakeException("No option found with name " + optionName
							+ ". It is needed in order to build this project as it is mentionned in Eclipse .classpath."
							+ " Please set this option either in command line as -" + optionName
							+ "=/absolute/path/for/this/var or in [jake_home]/options.properties" );
				}
				final File file = new File(varFile, JakeUtilsString.substringAfterFirst(classpathEntry.path, "/") );
				if (!file.exists()) {
					JakeLog.warn("Can't find Eclipse classpath entry : " + file.getAbsolutePath());
				}
				final Scope scope = libSegregator.scopeOfLib(classpathEntry.path);
				result.add(new Lib(file, scope));
			}
		}
		return result;
	}


	private static class ClasspathEntry {

		public final static String JRE_CONTAINER_PREFIX = "org.eclipse.jdt.launching.JRE_CONTAINER";

		public enum Kind {
			SRC, CON, LIB, VAR, UNKNOWN
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
			} else if ("var".equals(kindString)) {
				kind = Kind.VAR;
			} else {
				kind = Kind.UNKNOWN;
			}
			final ClasspathEntry result = new ClasspathEntry(kind, path, excluding, including);
			final NodeList nodeList = classpathEntryEl.getElementsByTagName("attributes");
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
			if (!excluding.isEmpty()) {
				final String[] patterns = excluding.split("\\|");
				jakeDir = jakeDir.exclude(patterns);
			}
			if (!including.isEmpty()) {
				final String[] patterns = including.split("\\|");
				jakeDir = jakeDir.include(patterns);
			}
			return jakeDir;
		}

		public boolean isOptional() {
			return "true".equals(this.attributes.get("optional"));
		}

		public List<File> conAsFiles(File baseDir, File containersDir) {
			if (!this.kind.equals(Kind.CON)) {
				throw new IllegalStateException("Can only get files from classpath entry of kind 'con'.");
			}
			if (!containersDir.exists()) {
				JakeLog.warn("Eclipse containers directory " + containersDir.getPath() + " does not exists... ignogre.");
				return Collections.emptyList();
			}
			final String folderName = path.replace('/', '_').replace('\\', '_');
			final File conFolder = new File(containersDir, folderName);
			if (!conFolder.exists()) {
				JakeLog.warn("Eclipse containers directory " + conFolder.getPath() + " does not exists... ignogre.");
				return Collections.emptyList();
			}
			final JakeDir dirView = JakeDir.of(conFolder).include("**/*.jar");
			final List<File> result = new LinkedList<File>();
			for (final File file : dirView.files()) {
				result.add(file);
			}
			return result;
		}

		public File libAsFile(File baseDir) {
			final String projectName;
			final String pathInProject;
			final File pathAsFile = new File(path);
			if (pathAsFile.isAbsolute() && pathAsFile.exists()) {
				return pathAsFile;
			}
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