package dev.jeka.core.api.tooling.eclipse;


import dev.jeka.core.api.system.JkLog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class WstCommonComponent {

    public static final String FILE = ".settings/org.eclipse.wst.common.component";

    private static final String VAR_PREFIX = "module:/classpath/var/";

    private static final String LIB_PREFIX = "module:/classpath/lib/";

    public static WstCommonComponent of(Path projectDir) {
        return new WstCommonComponent(parse(projectDir));
    }

    private final List<DotClasspathModel.ClasspathEntry> dependentModules;

    private WstCommonComponent(List<DotClasspathModel.ClasspathEntry> files) {
        this.dependentModules = Collections.unmodifiableList(files);
    }

    public List<DotClasspathModel.ClasspathEntry> dependentModules() {
        return this.dependentModules;
    }

    public boolean contains(DotClasspathModel.ClasspathEntry candidate) {
        for (final DotClasspathModel.ClasspathEntry entry : this.dependentModules) {
            if (entry.sameTypeAndPath(candidate)) {
                return true;
            }
        }
        return false;
    }

    public static boolean existIn(Path projectDir) {
        return Files.exists(componentFile(projectDir));
    }

    private static Path componentFile(Path projectDir) {
        return projectDir.resolve(FILE);
    }

    private static List<DotClasspathModel.ClasspathEntry> parse(Path projectDir) {
        final Path file = componentFile(projectDir);
        if (!Files.exists(file)) {
            throw new IllegalStateException("Can't find " + FILE);
        }
        final Document document = getFileAsDom(file);
        return from(document);
    }

    private static Document getFileAsDom(Path file) {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
            doc = dBuilder.parse(file.toFile());
            doc.getDocumentElement().normalize();
            return doc;
        } catch (final Exception e) {
            throw new RuntimeException("Error while parsing " + FILE + " : ", e);
        }
    }

    private static List<DotClasspathModel.ClasspathEntry> from(Document document) {
        final NodeList nodeList = document.getElementsByTagName("dependent-module");
        final List<DotClasspathModel.ClasspathEntry> result = new LinkedList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            final Element element = (Element) node;
            final String handle = element.getAttribute("handle");
            final DotClasspathModel.ClasspathEntry entry = handleToFile(handle);
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }

    private static DotClasspathModel.ClasspathEntry handleToFile(String handle) {
        final DotClasspathModel.ClasspathEntry result;
        if (handle.startsWith(VAR_PREFIX)) {
            final String rest = handle.substring(VAR_PREFIX.length());
            return DotClasspathModel.ClasspathEntry.of(DotClasspathModel.ClasspathEntry.Kind.VAR, rest);
        } else if (handle.startsWith(LIB_PREFIX)) {
            final String rest = handle.substring(LIB_PREFIX.length());
            return DotClasspathModel.ClasspathEntry.of(DotClasspathModel.ClasspathEntry.Kind.LIB, rest);
        } else {
            JkLog.warn("Ignoring handle " + handle);
            result = null;
        }
        return result;
    }

}
