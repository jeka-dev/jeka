/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.api.tooling.maven;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

import static dev.jeka.core.api.depmanagement.JkQualifiedDependencySet.*;

/**
 * Wraps a POM file (Ideally an effective POM file) and provides convenient methods to extract
 * information on.
 *
 * @author Jerome Angibaud
 */
public final class JkPom {

    private static final List<String> KNOWN_SCOPE = List.of(
            COMPILE_SCOPE,
            RUNTIME_SCOPE,
            PROVIDED_SCOPE,
            TEST_SCOPE,
            IMPORT_SCOPE);

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

    static JkPom of(InputStream is) {
        final Document document = JkDomDocument.parse(is).getW3cDocument();
        return new JkPom(document);
    }

    /**
     * Creates an effective POM representation for the specified Maven coordinate using the provided repository set.
     *
     * @param coordinate the Maven coordinate for which to generate the effective POM
     * @param repoSet the set of repositories to use for resolving dependencies and inheritance
     * @return a {@code JkPom} instance representing the effective POM of the specified coordinate
     */
    public static JkPom ofEffectivePom(JkCoordinate coordinate, JkRepoSet repoSet) {
        Path pomFile = JkCoordinateFileProxy.of(repoSet, coordinate).get();
        Document pomDoc = new PomResolver(repoSet).effectivePom(coordinate);
        return new JkPom(pomDoc);
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

    public String getParentGroupId() {
        Element parentEl = JkUtilsXml.directChild(projectEl(), "parent");
        if (parentEl != null) {
            return JkUtilsXml.directChildText(parentEl, "groupId");
        }
        return null;
    }

    /**
     * The artifactId for this POM.
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

    public String getParentVersion() {
        Element parentEl = JkUtilsXml.directChild(projectEl(), "parent");
        if (parentEl != null) {
            return JkUtilsXml.directChildText(parentEl, "version");
        }
        return null;
    }

    /**
     * The dependencies declared in this POM.
     */
    public JkQualifiedDependencySet getDependencies() {
        Element dependenciesEl = dependenciesElement();
        if (dependenciesEl == null) {
            return JkQualifiedDependencySet.of();
        }
        return dependencies(dependenciesEl, getProperties());
    }


    /**
     * Converts the current POM representation into a {@link JkVersionProvider}, which manages
     * version information for dependencies within the POM, including those defined in its
     * dependency management section. If there is no dependency management section, an empty
     * version provider is returned. Dependencies with a type of "pom" are identified as imported
     * BOMs and are not included in the returned provider.
     * <p>
     * Important: Ensure to use this method on an effectivePom (obtained with #toEffectivePom) to
     * get full resolution.
     *
     * @return a {@link JkVersionProvider} containing the versions of dependencies defined in the
     *         dependency management section of the POM, or an empty provider if no such section exists.
     */
    public JkVersionProvider toVersionProvider(JkRepoSet repos) {
        return toVersionProvider(repos, new HashSet<>());
    }

    private JkVersionProvider toVersionProvider(JkRepoSet repos, Set<JkCoordinate> resolvedCoordinates) {
        final List<JkCoordinate> coordinates = new LinkedList<>();
        Element dependencyManagementEl = dependencyManagementEl();
        if (dependencyManagementEl == null) {
            return JkVersionProvider.of();
        }
        final Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl,
                "dependencies");
        if (dependenciesEl == null) {
            return JkVersionProvider.of();
        }
        final JkQualifiedDependencySet scopedDependencies = dependencies(dependenciesEl, getProperties());

        List<JkCoordinate> importedBoms = new LinkedList<>();
        for (final JkCoordinateDependency coordinateDependency : scopedDependencies.getCoordinateDependencies()) {

            JkCoordinate coordinate = coordinateDependency.getCoordinate();
            if (!"pom".equals(coordinate.getArtifactSpecification().getType())) {
                coordinates.add(coordinate);
            } else {
                importedBoms.add(coordinate);
            }
        }

        // Import recursively boms
        importedBoms.forEach(pomCoordinate -> {
            pomCoordinate = pomCoordinate.withType("pom");
            Document resolvedImportedBomDoc = new PomResolver(repos).effectivePom(pomCoordinate);
            if (resolvedCoordinates.contains(pomCoordinate)) {
                return;
            }
            resolvedCoordinates.add(pomCoordinate);
            JkLog.debug("Resolving bom: " + pomCoordinate);
            JkPom resolvedImportedBom = new JkPom(resolvedImportedBomDoc);;
            coordinates.addAll(resolvedImportedBom.toVersionProvider(repos, resolvedCoordinates).toList());
        });

        return JkVersionProvider.of(coordinates);
    }

    /**
     * Returns properties declared in this POM.
     */
    public Map<String, String> getProperties() {
        final Map<String, String> result = new HashMap<>();
        if (propertiesEl() != null) {
            final NodeList nodeList = propertiesEl().getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                final Node node = nodeList.item(i);
                if (node instanceof Element) {
                    final Element element = (Element) node;
                    result.put(element.getTagName(), element.getTextContent());
                }
            }
        }
        return result;
    }



    /**
     * The {@link DependencyExclusions} instance provided by the <code>dependencyManagement</code>
     * section of this POM.
     */
    public DependencyExclusions getDependencyExclusion() {
        DependencyExclusions result = DependencyExclusions.of();
        Element dependencyManagementEl = dependencyManagementEl();
        if (dependencyManagementEl == null) {
            return result;
        }
        final Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl, "dependencies");
        if (dependenciesEl == null) {
            return result;
        }
        final JkQualifiedDependencySet scopedDependencies = dependencies(dependenciesEl, getProperties());
        for (final JkCoordinateDependency coordinateDependency : scopedDependencies.getCoordinateDependencies()) {
            if (!coordinateDependency.getExclusions().isEmpty()) {
                result = result.and(coordinateDependency.getCoordinate().getModuleId(),
                        coordinateDependency.getExclusions());
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

    /**
     * Converts the current POM to its effective form, resolving any inherited or external dependencies
     * using the specified repository set.
     *
     * @param repos the set of repositories to use for resolving dependencies and external properties
     * @return the effective POM representation as a {@code JkPom} instance
     */
    public JkPom toEffectivePom(JkRepoSet repos) {
        JkCoordinate coordinate = getCoordinate();
        Document effectivePom;
        try {
            effectivePom = new PomResolver(repos).effectivePom(coordinate);
        } catch (final IllegalStateException e) {
            JkLog.debug("POM not found for " + coordinate);
            effectivePom = (Document) pomDoc.cloneNode(true);
            new PomResolver(repos).resolve(effectivePom);
        }
        return new JkPom(effectivePom);
    }

    /**
     * Converts the current instance's underlying POM document into a DOM {@code Document}.
     *
     * @return a deep-cloned {@code Document} representation of the POM.
     */
    public Document toDocument() {
        return (Document) pomDoc.cloneNode(true);
    }

    private JkQualifiedDependencySet dependencies(Element dependenciesEl, Map<String, String> props) {
        List<JkQualifiedDependency> scopedDependencies = new LinkedList<>();
        for (final Element dependencyEl : JkUtilsXml.directChildren(dependenciesEl, "dependency")) {
            scopedDependencies.add(scopedDep(dependencyEl, props));
        }
        return JkQualifiedDependencySet.of(scopedDependencies);
    }

    private JkQualifiedDependency scopedDep(Element mvnDependency, Map<String, String> props) {
        final String groupId = JkUtilsXml.directChildText(mvnDependency, "groupId");
        final String artifactId = JkUtilsXml.directChildText(mvnDependency, "artifactId");
        final String version = JkUtilsXml.directChildText(mvnDependency, "version");
        JkCoordinate coordinate = JkCoordinate.of(groupId, artifactId, version);
        final String type = JkUtilsXml.directChildText(mvnDependency, "type");
        final String classifier = JkUtilsXml.directChildText(mvnDependency, "classifier");
        if (type != null || classifier != null) {
            coordinate = coordinate.withClassifierAndType(classifier, type);
        }
        JkCoordinateDependency coordinateDependency = JkCoordinateDependency.of(coordinate);
        final Element exclusionsEl = JkUtilsXml.directChild(mvnDependency, "exclusions");
        if (exclusionsEl != null) {
            for (final Element exclusionElement : JkUtilsXml.directChildren(exclusionsEl,
                    "exclusion")) {
                coordinateDependency = coordinateDependency.andExclusions(jkDepExclude(exclusionElement));
            }
        }
        String scope = JkUtilsXml.directChildText(mvnDependency, "scope");
        scope = JkUtilsObject.firstNonNull(scope, COMPILE_SCOPE);
        String realScope = toScope(scope);
        return JkQualifiedDependency.of(realScope, coordinateDependency);
    }

    private JkDependencyExclusion jkDepExclude(Element exclusionEl) {
        final String groupId = JkUtilsXml.directChildText(exclusionEl, "groupId");
        final String artifactId = JkUtilsXml.directChildText(exclusionEl, "artifactId");
        return JkDependencyExclusion.of(groupId, artifactId);

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

    private JkCoordinate getCoordinate() {
        JkCoordinate parentCoordinate = getParentCoordinate();
        String groupId = Optional.ofNullable(getGroupId())
                .orElseGet(() -> parentCoordinate.getModuleId().getGroup());
        String artifactId = getArtifactId();
        String versionId = Optional.ofNullable(getVersion())
                .orElseGet(() -> parentCoordinate.getVersion().toString());
        return JkCoordinate.of(groupId, artifactId, versionId);
    }

    private JkCoordinate getParentCoordinate() {
        Element parentEl = JkUtilsXml.directChild(projectEl(), "parent");
        if (parentEl == null) {
            return null;
        }
        String groupId = JkUtilsXml.directChildText(parentEl, "groupId");
        String artifactId = JkUtilsXml.directChildText(parentEl, "artifactId");
        String version = JkUtilsXml.directChildText(parentEl, "version");
        return JkCoordinate.of(groupId, artifactId, version);
    }

}
