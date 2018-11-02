package org.jerkar.api.tooling;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.depmanagement.JkRepoSet;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wraps a POM file (Ideally an effective POM file) and provides convenient methods to extract
 * information jump.
 *
 * @author Jerome Angibaud
 */
public final class JkPom {

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
    public JkDependencySet getDependencies() {
        return dependencies(dependenciesElement(), getProperties());
    }

    /**
     * The map groupId:ArtifactId -> version provideded by the <code>dependencyManagement</code>
     * section of this POM.
     */
    public JkVersionProvider getVersionProvider() {
        final List<JkVersionedModule> versionedModules = new LinkedList<>();
        if (dependencyManagementEl() == null) {
            return JkVersionProvider.empty();
        }
        final Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl(),
                "dependencies");
        final JkDependencySet dependencies = dependencies(dependenciesEl, getProperties());
        for (final JkScopedDependency scopedDependency : dependencies) {
            final JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency
                    .withDependency();
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
        return result;
    }

    /**
     * The {@link JkDependencyExclusions} instance provided by the <code>dependencyManagement</code>
     * section of this POM.
     */
    public JkDependencyExclusions getDependencyExclusion() {
        JkDependencyExclusions result = JkDependencyExclusions.of();
        if (dependencyManagementEl() == null) {
            return result;
        }
        final Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl(),
                "dependencies");
        final JkDependencySet dependencies = dependencies(dependenciesEl, getProperties());
        for (final JkScopedDependency scopedDependency : dependencies) {
            final JkModuleDependency moduleDependency = (JkModuleDependency) scopedDependency
                    .withDependency();
            if (!moduleDependency.getExcludes().isEmpty()) {
                result = result.and(moduleDependency.getModuleId(), moduleDependency.getExcludes());
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
            return JkRepoSet.ofEmpty();
        }
        for (final Element repositoryEl : JkUtilsXml.directChildren(repositoriesEl(), "repository")) {
            urls.add(JkUtilsXml.directChildText(repositoryEl, "url"));
        }
        return JkRepoSet.of(JkUtilsIterable.arrayOf(urls, String.class));
    }

    private JkDependencySet dependencies(Element dependenciesEl, Map<String, String> props) {
        List<JkScopedDependency> scopedDependencies = new LinkedList<>();
        for (final Element dependencyEl : JkUtilsXml.directChildren(dependenciesEl, "dependency")) {
            scopedDependencies.add(jkDependency(dependencyEl, props));
        }
        return JkDependencySet.of(scopedDependencies);
    }

    private JkScopedDependency jkDependency(Element mvnDependency, Map<String, String> props) {

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
        final String scope = JkUtilsXml.directChildText(mvnDependency, "scope");
        final JkScope[] jkScopes;
        if (scope == null || scope.equalsIgnoreCase("compile")) {
            jkScopes = new JkScope[0];
        } else {
            jkScopes =  new JkScope[] {JkScope.of(scope)};
        }
        return JkScopedDependency.of(moduleDependency, jkScopes);
    }

    private JkDepExclude jkDepExclude(Element exclusionEl) {
        final String groupId = JkUtilsXml.directChildText(exclusionEl, "groupId");
        final String artifactId = JkUtilsXml.directChildText(exclusionEl, "artifactId");
        return JkDepExclude.of(groupId, artifactId);

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

}
