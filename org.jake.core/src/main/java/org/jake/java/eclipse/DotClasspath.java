package org.jake.java.eclipse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jake.JakeBuildResolver;
import org.jake.JakeDir;
import org.jake.JakeDirSet;
import org.jake.JakeException;
import org.jake.JakeLocator;
import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.depmanagement.JakeScope;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.eclipse.DotClasspath.ClasspathEntry.Kind;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class DotClasspath {

	private static final String CLASSPATHENTRY = "classpathentry";

	private final List<ClasspathEntry> classpathentries = new LinkedList<ClasspathEntry>();

	private DotClasspath(List<ClasspathEntry> classpathentries) {
		this.classpathentries.addAll(classpathentries);
	}

	public static DotClasspath from(File dotClasspathFile) {
		final Document document = getDotClassPathAsDom(dotClasspathFile);
		return from(document);
	}

	private static DotClasspath from(Document document) {
		final NodeList nodeList = document.getElementsByTagName(CLASSPATHENTRY);
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

	public String outputPath() {
		for (final ClasspathEntry classpathEntry : classpathentries) {
			if (classpathEntry.kind.equals(ClasspathEntry.Kind.OUTPUT)) {
				return classpathEntry.path;
			}
		}
		return null;
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

	public List<Lib> libs(File baseDir, ScopeResolver libSegregator) {
		final List<Lib> result = new LinkedList<Lib>();
		final Map<String, File> projects = Project.findProjects(baseDir.getParentFile());
		for (final ClasspathEntry classpathEntry : classpathentries) {

			if (classpathEntry.kind.equals(ClasspathEntry.Kind.CON) ) {
				final JakeScope scope = libSegregator.scopeOfCon(classpathEntry.path);
				if (classpathEntry.path.startsWith(ClasspathEntry.JRE_CONTAINER_PREFIX)) {
					continue;
				}
				for (final File file : classpathEntry.conAsFiles(baseDir)) {
					result.add(Lib.file(file, scope, classpathEntry.exported));
				}

			} else if (classpathEntry.kind.equals(ClasspathEntry.Kind.LIB)) {
				final JakeScope scope = libSegregator.scopeOfLib(ClasspathEntry.Kind.LIB, classpathEntry.path);
				result.add(Lib.file(classpathEntry.libAsFile(baseDir, projects), scope, classpathEntry.exported));

			} else if (classpathEntry.kind.equals(ClasspathEntry.Kind.VAR)) {
				final String var = JakeUtilsString.substringBeforeFirst(classpathEntry.path, "/");
				final String optionName = JakeBuildPluginEclipse.OPTION_VAR_PREFIX + var;
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
				final JakeScope scope = libSegregator.scopeOfLib(Kind.VAR, classpathEntry.path);
				result.add(Lib.file(file, scope, classpathEntry.exported));

			} else if (classpathEntry.kind.equals(ClasspathEntry.Kind.SRC)) {
				if (classpathEntry.isProjectSrc(baseDir.getParentFile(), projects)) {
					final String projectPath = classpathEntry.projectRelativePath(baseDir, projects);
					result.add(Lib.project(projectPath, JakeJavaBuild.COMPILE, classpathEntry.exported));
				}

			}
		}
		return result;
	}



	static class ClasspathEntry {


		public final static String JRE_CONTAINER_PREFIX = "org.eclipse.jdt.launching.JRE_CONTAINER";

		public enum Kind {
			SRC, CON, LIB, VAR, OUTPUT, UNKNOWN
		}

		private final Kind kind;

		private final boolean exported;

		private final String path;

		private final String excluding;

		private final String including;

		private final Map<String, String> attributes = new HashMap<String, String>();

		public ClasspathEntry(Kind kind, String path, String excluding, String including, boolean exported) {
			super();
			this.kind = kind;
			this.path = path;
			this.excluding = excluding;
			this.including = including;
			this.exported = exported;
		}

		public static ClasspathEntry of(Kind kind, String path) {
			return new ClasspathEntry(kind, path, null, null, false);
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
			} else if ("output".equals(kindString)) {
				kind = Kind.OUTPUT;
			} else {
				kind = Kind.UNKNOWN;
			}
			final String exportedString = classpathEntryEl.getAttribute("exported");
			final boolean export = "true".equals(exportedString);
			final ClasspathEntry result = new ClasspathEntry(kind, path, excluding, including, export);
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

		public boolean sameTypeAndPath(ClasspathEntry other)  {
			if (!this.kind.equals(other.kind)) {
				return false;
			}
			return this.path.equals(other.path);
		}

		public List<File> conAsFiles(File baseDir) {
			if (!this.kind.equals(Kind.CON)) {
				throw new IllegalStateException("Can only get files from classpath entry of kind 'con'.");
			}
			if (!Lib.CONTAINER_DIR.exists()  && !Lib.CONTAINER_USER_DIR.exists() ) {
				JakeLog.warn("Eclipse containers directory " + Lib.CONTAINER_USER_DIR.getPath()
						+ " or  " + Lib.CONTAINER_DIR.getPath() + " does not exists... Ignore");
				return Collections.emptyList();
			}
			final String folderName = path.replace('/', '_').replace('\\', '_');
			File conFolder = new File(Lib.CONTAINER_USER_DIR, folderName);
			if (!conFolder.exists()) {
				conFolder = new File(Lib.CONTAINER_DIR, folderName);
				if (!conFolder.exists()) {
					JakeLog.warn("Eclipse containers directory " + conFolder.getPath()
							+ " or " + new File(Lib.CONTAINER_USER_DIR, folderName).getPath()
							+ "  do not exists... ignogre.");
					return Collections.emptyList();
				}
			}
			final JakeDir dirView = JakeDir.of(conFolder).include("**/*.jar");
			final List<File> result = new LinkedList<File>();
			for (final File file : dirView.files()) {
				result.add(file);
			}
			return result;
		}

		public File libAsFile(File baseDir, Map<String, File> projectLocationMap) {
			final String pathInProject;
			final File pathAsFile = new File(path);
			if (pathAsFile.isAbsolute()) {
				return pathAsFile;
			}
			if (path.startsWith("/")) {
				final int secondSlashIndex = path.indexOf("/", 1);
				pathInProject = path.substring(secondSlashIndex+1);
				final File otherProjectDir = projectLocation(baseDir.getParentFile(), projectLocationMap);
				return new File (otherProjectDir, pathInProject);
			}
			return new File(baseDir, path);
		}

		public boolean isProjectSrc(File parent, Map<String, File> projectLocationMap) {
			return path.startsWith("/");
		}

		public String projectRelativePath(File baseDir, Map<String, File> projectLocationMap) {
			final File projectDir = projectLocation(baseDir.getParentFile(), projectLocationMap);
			return "../" + projectDir.getName();
		}

		private File projectLocation(File parent, Map<String, File> projectLocationMap) {
			final int secondSlashIndex = path.indexOf("/", 1);
			final String projectName;
			if (secondSlashIndex == -1) {
				projectName = path.substring(1);
			} else {
				projectName = path.substring(1, secondSlashIndex);
			}
			final File otherProjectDir = projectLocationMap.get(projectName);
			if (otherProjectDir == null) {
				throw new IllegalStateException("Project " + projectName + " not found in " + parent.getPath());
			}
			return otherProjectDir;
		}

	}

	static void generate(JakeJavaBuild build, File outputFile) throws IOException, XMLStreamException, FactoryConfigurationError {
		final FileWriter fileWriter = new FileWriter(outputFile);
		final XMLStreamWriter writer = XMLOutputFactory.newInstance()
				.createXMLStreamWriter(fileWriter);
		writer.writeStartDocument("UTF-8", "1.0");
		writer.writeCharacters("\n");
		writer.writeStartElement("classpath");
		writer.writeCharacters("\n");

		// Build sources
		if (build.baseDir(JakeBuildResolver.BUILD_SOURCE_DIR).exists()) {
			writer.writeCharacters("\t");
			writer.writeEmptyElement(CLASSPATHENTRY);
			writer.writeAttribute("kind", "src");
			writer.writeAttribute("path", JakeBuildResolver.BUILD_SOURCE_DIR);
			writer.writeCharacters("\n");
		}

		// Sources
		final Set<String> sourcePaths = new HashSet<String>();
		for (final JakeDir jakeDir : build.sourceDirs().and(build.resourceDirs())
				.jakeDirs()) {
			if (!jakeDir.root().exists() ) {
				continue;
			}
			final String path = JakeUtilsFile.getRelativePath(build.baseDir(""), jakeDir.root())
					.replace(File.separator,  "/");
			if (sourcePaths.contains(path)) {
				continue;
			}
			sourcePaths.add(path);
			writer.writeCharacters("\t");
			writer.writeEmptyElement(CLASSPATHENTRY);
			writer.writeAttribute("kind", "src");
			writer.writeAttribute("path", path);
			writer.writeCharacters("\n");
		}



		// Test Sources
		for (final JakeDir jakeDir : build.testResourceDirs().and(build.testResourceDirs())
				.jakeDirs()) {
			if (!jakeDir.root().exists() ) {
				continue;
			}
			final String path = JakeUtilsFile.getRelativePath(build.baseDir(""), jakeDir.root())
					.replace(File.separator,  "/");
			if (sourcePaths.contains(path)) {
				continue;
			}
			sourcePaths.add(path);
			writer.writeCharacters("\t");
			writer.writeEmptyElement(CLASSPATHENTRY);
			writer.writeAttribute("kind", "src");
			writer.writeAttribute("output", JakeUtilsFile.getRelativePath(build.baseDir(""),
					build.testClassDir()).replace(File.separator, "/"));
			writer.writeAttribute("path", path);
			writer.writeCharacters("\n");
		}

		// Write entry for Jake
		writeJakeEntry(writer);

		// Write entry for JDK
		writer.writeCharacters("\t");
		writer.writeEmptyElement(CLASSPATHENTRY);
		writer.writeAttribute("kind", "con");
		writer.writeAttribute("path", "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-"
				+ build.sourceJavaVersion() );
		writer.writeCharacters("\n");

		// Write entries for regular deps
		for (final File file : build.dependencyResolver().get(JakeJavaBuild.RUNTIME,
				JakeJavaBuild.PROVIDED, JakeJavaBuild.TEST)) {
			writeClassEntry(writer, file);
		}

		// Write output
		writer.writeCharacters("\t");
		writer.writeEmptyElement(CLASSPATHENTRY);
		writer.writeAttribute("kind", "output");
		writer.writeAttribute("path", JakeUtilsFile.getRelativePath(build.baseDir(""), build.classDir())
				.replace(File.separator,  "/"));
		writer.writeCharacters("\n");
		writer.writeEndDocument();
		writer.flush();
		writer.close();
	}

	private static void writeClassEntry(XMLStreamWriter writer, File file) throws XMLStreamException {
		writer.writeCharacters("\t");
		writer.writeEmptyElement(CLASSPATHENTRY);
		final VarReplacement varReplacement = new VarReplacement(file);
		if (varReplacement.replaced) {
			writer.writeAttribute("kind", "var");
		} else {
			writer.writeAttribute("kind", "lib");
		}
		writer.writeAttribute("path", varReplacement.path);
		writer.writeCharacters("\n");
	}

	private static void writeJakeEntry(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeCharacters("\t");
		writer.writeEmptyElement(CLASSPATHENTRY);
		final File file = JakeLocator.jakeJarFile();
		final VarReplacement varReplacement = new VarReplacement(file);
		if (varReplacement.replaced) {
			writer.writeAttribute("kind", "var");
		} else {
			writer.writeAttribute("kind", "lib");
		}
		writer.writeAttribute("path", varReplacement.path);
		final String sourceFileName = JakeUtilsString.substringAfterLast(file.getName(), ".jar") + "-sources.jar";
		final File sourceFile = new File(JakeLocator.jakeHome(), "libs/sources/"+ sourceFileName);
		if (sourceFile.exists()) {
			final VarReplacement sourceVarReplacement = new VarReplacement(sourceFile);
			writer.writeAttribute("sourcepath", sourceVarReplacement.path);
		}
		writer.writeCharacters("\n");
	}

	private static class VarReplacement {

		public final boolean replaced;

		public final String path;

		public VarReplacement(File file) {
			final Map<String, String> map = JakeOptions.getAllStartingWith(JakeBuildPluginEclipse.OPTION_VAR_PREFIX);
			boolean replaced = false;
			String path = JakeUtilsFile.canonicalPath(file).replace(File.separator, "/");
			for (final Map.Entry<String, String> entry : map.entrySet()) {
				final File varDir = new File(entry.getValue());
				if (JakeUtilsFile.isAncestor(varDir, file)) {
					final String relativePath = JakeUtilsFile.getRelativePath(varDir, file);
					replaced = true;
					path = entry.getKey().substring(JakeBuildPluginEclipse.OPTION_VAR_PREFIX.length())
							+ "/" + relativePath;
					path = path.replace(File.separator, "/");
					break;
				}
			}
			this.path = path;
			this.replaced = replaced;
		}

	}

}