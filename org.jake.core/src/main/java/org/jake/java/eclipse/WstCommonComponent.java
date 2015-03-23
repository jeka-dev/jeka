package org.jake.java.eclipse;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jake.JakeException;
import org.jake.JakeLog;
import org.jake.JakeOptions;
import org.jake.utils.JakeUtilsString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class WstCommonComponent {

	private static final String VAR_PREFIX = "module:/classpath/var/";

	private static final String lib_PREFIX = "module:/classpath/lib/";

	public static final WstCommonComponent of(File projectDir) {
		return new WstCommonComponent(parse(projectDir));
	}

	private final List<File> dependentModules;

	private WstCommonComponent(List<File> files) {
		this.dependentModules = Collections.unmodifiableList(files);
	}

	public List<File> dependentModules() {
		return this.dependentModules;
	}

	private static List<File> parse(File projectDir) {
		final File file = new File(projectDir, ".settings/org.eclipse.wst.common.component");
		if (!file.exists()) {
			return Collections.emptyList();
		}
		final Document document = getFileAsDom(file);
		return from(projectDir, document);
	}

	private static Document getFileAsDom(File file) {
		if (!file.exists()) {
			throw new IllegalStateException(file.getAbsolutePath() + " file not found.");
		}
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc;
			doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (final Exception e) {
			throw new RuntimeException("Error while parsing .settings/org.eclipse.wst.common.component file : ", e);
		}
	}

	private static List<File> from(File projectDir, Document document) {
		final NodeList nodeList = document.getElementsByTagName("classpathentry");
		final List<File> result = new LinkedList<File>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			final Node node = nodeList.item(i);
			final Element element = (Element) node;
			final String handle = element.getAttribute("handle");
			final File file = handleToFile(projectDir, handle);
			if (file != null && file.exists()) {
				result.add(file);
			}
		}
		return result;
	}

	private static File handleToFile(File projectDir, String handle) {
		final File result;
		if (handle.startsWith(VAR_PREFIX)) {
			final String rest = handle.substring(VAR_PREFIX.length());
			final String varName = JakeUtilsString.substringBeforeFirst(rest, "/");
			final String key = JakeEclipseBuild.OPTION_VAR_PREFIX + varName;
			if (JakeOptions.containsKey(varName)) {
				final String varValue = JakeOptions.get(key);
				final String relativeLocation = JakeUtilsString.substringAfterFirst(rest, "/");
				result = new File(varValue + File.separator + relativeLocation);
			} else {
				throw new JakeException("Can't find option " + key + " to resolve file path " +
						handle + " in .settings/org.eclipse.wst.common.component");
			}
		} else if (handle.startsWith(lib_PREFIX)) {
			final String rest = handle.substring(lib_PREFIX.length());
			final File file = new File(rest);
			if (file.exists() && file.isAbsolute()) {
				result = file;
			} else {
				result = new File(projectDir.getParentFile(), rest);
			}
		} else {
			JakeLog.warn("Ignoring handle " + handle);
			result = null;
		}
		return result;
	}


}
