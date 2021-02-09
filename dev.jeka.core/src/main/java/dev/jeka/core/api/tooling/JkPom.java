package dev.jeka.core.api.tooling;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.depmanagement.JkQualifiedDependency;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static dev.jeka.core.api.depmanagement.JkQualifiedDependencies.*;

/**
 * Wraps a POM file (Ideally an effective POM file) and provides convenient methods to extract
 * information on.
 *
 * @author Jerome Angibaud
 */
public final class JkPom {

    private static final List<String> KNOWN_SCOPE = JkUtilsIterable.listOf(COMPILE_SCOPE, RUNTIME_SCOPE,
            PROVIDED_SCOPE, TEST_SCOPE);

    private final Document pomDoc;

    private JkPom(Document pomDoc) {
        super();
        this.pomDoc = pomDoc;
    }

    /**
     * Creates a {@link JkPom} jump a POM file, ideally an effective POM file.
     */
    public static JkPom of(Path file) {
        final Document document = JkUtilsXml.documentFrom(file);
        return new JkPom(document);
    }

    private Element propertiesEl() {
        return JkUtilsXml.directChild(projectEl(), "properties");
    }

    private Element dependenciesElement() {
        return JkUtilsXml.directChild(projectEl(), "dependencies");
    }

    private Element dependencyManagementEl() {
        return JkUtilsXml.directChild(projectEl(), "dependencyManagement");
    }

    private Element repositoriesEl() {
        return JkUtilsXml.directChild(projectEl(), "repositories");
    }

    private Element projectEl() {
        return pomDoc.getDocumentElement();
    }

    /**
     * The groupId for this POM.
     */
    public String getGroupId() {
        return JkUtilsXml.directChildText(projectEl(), "groupId");
    }

    /**
     * The artifzctId for this POM.
     */
    public String getArtifactId() {
        return JkUtilsXml.directChildText(projectEl(), "artifactId");
    }

    /**
     * The version for this POM.
     */
    public String getVersion() {
        return JkUtilsXml.directChildText(projectEl(), "version");
    }

    /**
     * The dependencies declared in this POM.
     */
    public JkQualifiedDependencies getDependencies() {
        return dependencies(dependenciesElement(), getProperties());
    }

    /**
     * The map groupId:ArtifactId -> version provided by the <code>dependencyManagement</code>
     * section of this POM.
     */
    public JkVersionProvider getVersionProvider() {
        final List<JkVersionedModule> versionedModules = new LinkedList<>();
        if (dependencyManagementEl() == null) {
            return JkVersionProvider.of();
        }
        final Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl(),
                "dependencies");
        final JkQualifiedDependencies scopedDependencies = dependencies(dependenciesEl, getProperties());
        for (final JkModuleDependency moduleDependency : scopedDependencies.getModuleDependencies()) {
            final JkVersionedModule versionedModule = JkVersionedModule.of(
                    moduleDependency.getModuleId(),
                    JkVersion.of(moduleDependency.getVersion().getValue()));
            versionedModules.add(versionedModule);
        }
        return JkVersionProvider.of(versionedModules);
    }

    /**
     * Returns properties declared in this POM.
     */
    public Map<String, String> getProperties() {
        final Map<String, String> result = new HashMap<>();
        if (propertiesEl() == null) {
            return result;
        }
        final NodeList nodeList = propertiesEl().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (node instanceof Element) {
                final Element element = (Element) node;
                result.put(element.getTagName(), element.getTextContent());
            }
        }
        interpolate(result, 0);
        return result;
    }

    private static void interpolate(Map<String, String> map, int count) {
        boolean found = false;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String value = entry.getValue();
            for (String key : map.keySet()) {
                String token = "${" + key + "}";
                if (value.contains(token)) {
                    found = true;
                    String interpolatedValue = map.get(key);
                    String newValue = value.replace(token, interpolatedValue);
                    map.put(entry.getKey(), newValue);
                }
            }
        }
        if (count < 10 && found) {
            interpolate(map, count+1);
        }
    }

    /**
     * The {@link DependencyExclusions} instance provided by the <code>dependencyManagement</code>
     * section of this POM.
     */
    public DependencyExclusions getDependencyExclusion() {
        DependencyExclusions result = DependencyExclusions.of();
        if (dependencyManagementEl() == null) {
            return result;
        }
        final Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl(), "dependencies");
        final JkQualifiedDependencies scopedDependencies = dependencies(dependenciesEl, getProperties());
        for (final JkModuleDependency moduleDependency : scopedDependencies.getModuleDependencies()) {
            if (!moduleDependency.getExclusioins().isEmpty()) {
                result = result.and(moduleDependency.getModuleId(), moduleDependency.getExclusioins());
            }
        }
        return result;
    }

    /**
     * Repositories declared in this POM.
     */
    public JkRepoSet getRepos() {
        final List<String> urls = new LinkedList<>();
        if (repositoriesEl() == null) {
            return JkRepoSet.of();
        }
        for (final Element repositoryEl : JkUtilsXml.directChildren(repositoriesEl(), "repository")) {
            urls.add(JkUtilsXml.directChildText(repositoryEl, "url"));
        }
        return JkRepoSet.of(JkUtilsIterable.arrayOf(urls, String.class));
    }

    private JkQualifiedDependencies dependencies(Element dependenciesEl, Map<String, String> props) {
        List<JkQualifiedDependency> scopedDependencies = new LinkedList<>();
        for (final Element dependencyEl : JkUtilsXml.directChildren(dependenciesEl, "dependency")) {
            scopedDependencies.add(scopedDep(dependencyEl, props));
        }
        return JkQualifiedDependencies.of(scopedDependencies);
    }

    private JkQualifiedDependency scopedDep(Element mvnDependency, Map<String, String> props) {
        final String groupId = JkUtilsXml.directChildText(mvnDependency, "groupId");
        final String artifactId = JkUtilsXml.directChildText(mvnDependency, "artifactId");
        final String version = resolveProps(JkUtilsXml.directChildText(mvnDependency, "version"), props) ;
        JkModuleDependency moduleDependency = JkModuleDependency.of(groupId, artifactId, version);
        final String type = JkUtilsXml.directChildText(mvnDependency, "type");
        if (type != null) {
            moduleDependency = moduleDependency.withExt(type);
        }
        final String classifier = JkUtilsXml.directChildText(mvnDependency, "classifier");
        if (classifier != null) {
            moduleDependency = moduleDependency.withClassifier(classifier);
        }
        final Element exclusionsEl = JkUtilsXml.directChild(mvnDependency, "exclusions");
        if (exclusionsEl != null) {
            for (final Element exclusionElement : JkUtilsXml.directChildren(exclusionsEl,
                    "exclusion")) {
                moduleDependency = moduleDependency.andExclude(jkDepExclude(exclusionElement));
            }
        }
        String scope = JkUtilsXml.directChildText(mvnDependency, "scope");
        scope = JkUtilsObject.firstNonNull(scope, COMPILE_SCOPE);
        String realScope = toScope(scope);
        return JkQualifiedDependency.of(realScope, moduleDependency);
    }

    private JkDependencyExclusion jkDepExclude(Element exclusionEl) {
        final String groupId = JkUtilsXml.directChildText(exclusionEl, "groupId");
        final String artifactId = JkUtilsXml.directChildText(exclusionEl, "artifactId");
        return JkDependencyExclusion.of(groupId, artifactId);

    }

    private static String resolveProps(String value, Map<String, String> props) {
        if (JkUtilsString.isBlank(value)) {
            return value;
        }
        if (value.startsWith("${") && value.endsWith("}")) {
            String varName = value.substring(2, value.length()-1);
            String varValue = props.get(varName);
            if (varValue != null) {
                return varValue;
            }
        }
        return value;
    }

    static String toScope(String candidate)  {
        if (candidate == null) {
            return null;
        }
        String normalized = candidate.trim().toLowerCase();
        if (KNOWN_SCOPE.contains(normalized)) {
            return candidate;
        }
        return null;
    }

}
