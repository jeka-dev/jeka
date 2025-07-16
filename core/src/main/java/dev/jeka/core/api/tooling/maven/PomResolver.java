/*
 * Copyright 2014-2025  the original author or authors.
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
import dev.jeka.core.api.utils.JkUtilsXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code PomResolver} class is responsible for resolving and merging Maven POM files
 * to produce effective POMs. This includes handling inheritance from parent POMs and
 * applying dependency management, properties, dependencies, and repositories as specified
 * across multiple levels.
 *
 * The class uses a provided repository set to fetch and cache POM files, optimizing
 * resolution for repeated accesses. It maintains an internal cache of already resolved
 * POMs to avoid redundant computations.
 *
 * Key functionalities:
 * - Resolves parent-child relationships in POM files.
 * - Merges parent POM elements (e.g., properties, dependencies, dependency management, repositories)
 *   into the current POM.
 * - Interpolates properties into POM elements.
 * - Ensures properties, dependencies, and repositories inheriting from parents do not override
 *   explicitly specified elements in child POMs.
 *
 * This class relies on utility components for parsing and DOM manipulation as well as
 * repository management.
 */
class PomResolver {

    private static final Map<JkCoordinate, Document> RESOLVED_POMS = new HashMap<>();

    private final JkRepoSet repos;

    public PomResolver(JkRepoSet repoSet) {
        this.repos = repoSet;
    }

    Document effectivePom(JkCoordinate coordinate) {
        Document resolvedPom = RESOLVED_POMS.get(coordinate);
        if (resolvedPom != null) {
            return resolvedPom;
        }
        Path pomFile = JkCoordinateFileProxy.of(repos, coordinate.withClassifierAndType("", "pom")).get();
        Document document = JkDomDocument.parse(pomFile).getW3cDocument();
        resolve(document);
        RESOLVED_POMS.put(coordinate, document);
        return document;
    }

    // resolve to effective pom
    void resolve(Document pom) {
        Document parentPom = getParentPom(pom);
        if (parentPom != null) {
            mergeProperties(pom, parentPom);
            mergeDependencies(pom, parentPom);
            mergeDependencyManagement(pom, parentPom);
            mergeRepositories(pom, parentPom);
        }
        Map<String, String> properties = getPropertiesFromDom(pom);
        interpolateElement(pom.getDocumentElement(), properties);
    }

    private Document getParentPom(Document pom) {
        JkCoordinate parentCoordinate = getParentCoordinate(pom);
        if (parentCoordinate == null) {
            return null;
        }
        Document parentDoc = RESOLVED_POMS.get(parentCoordinate);
        if (parentDoc != null) {
            return parentDoc;
        }

        // Resolve parent pom
        JkCoordinateFileProxy coordinateFileProxy = JkCoordinateFileProxy.of(repos, parentCoordinate);
        try {
            return JkDomDocument.parse(coordinateFileProxy.get()).getW3cDocument();
        } catch (IllegalStateException e) {
            coordinateFileProxy = JkCoordinateFileProxy.of(repos, parentCoordinate.withType("pom"));
            try {
                return JkDomDocument.parse(coordinateFileProxy.get()).getW3cDocument();
            } catch (IllegalStateException e2) {
                throw new IllegalStateException("Cannot find parent " + parentCoordinate + " of pom\n"
                        + JkDomDocument.of(pom).toXml(), e);
            }
        }
    }

    private static JkCoordinate getParentCoordinate(Document pom) {
        Element parentEl = JkUtilsXml.directChild(projectEl(pom), "parent");
        if (parentEl == null) {
            return null;
        }
        String groupId = JkUtilsXml.directChildText(parentEl, "groupId");
        String artifactId = JkUtilsXml.directChildText(parentEl, "artifactId");
        String version = JkUtilsXml.directChildText(parentEl, "version");
        return JkCoordinate.of(groupId, artifactId, version);
    }

    private static Element projectEl(Document pom) {
        return pom.getDocumentElement();
    }

    private static void mergeProperties(Document pom, Document parentPom) {
        Element root = pom.getDocumentElement();
        Element propertiesEl = JkUtilsXml.directChild(root, "properties");
        if (propertiesEl == null) {
            propertiesEl = pom.createElement("properties");
            root.appendChild(propertiesEl);
        }

        Element parentPropertiesEl = JkUtilsXml.directChild(parentPom.getDocumentElement(), "properties");
        if (parentPropertiesEl != null) {
            NodeList parentPropertyNodes = parentPropertiesEl.getChildNodes();
            for (int i = 0; i < parentPropertyNodes.getLength(); i++) {
                Node node = parentPropertyNodes.item(i);
                if (node instanceof Element) {
                    Element parentPropertyEl = (Element) node;
                    String tagName = parentPropertyEl.getTagName();

                    // Only add if not already present (child overrides parent)
                    Element existingProperty = JkUtilsXml.directChild(propertiesEl, tagName);
                    if (existingProperty == null) {
                        Element importedProperty = (Element) pom.importNode(parentPropertyEl, true);
                        propertiesEl.appendChild(importedProperty);
                    }
                }
            }
        }
    }

    private void mergeDependencies(Document pom, Document parentPom) {
        Element dependenciesEl = JkUtilsXml.directChild(pom.getDocumentElement(), "dependencies");
        Element parentDependenciesEl = JkUtilsXml.directChild(parentPom.getDocumentElement(), "dependencies");

        if (parentDependenciesEl != null) {
            if (dependenciesEl == null) {
                dependenciesEl = pom.createElement("dependencies");
                pom.getDocumentElement().appendChild(dependenciesEl);
            }

            NodeList parentDependencyNodes = parentDependenciesEl.getChildNodes();
            for (int i = 0; i < parentDependencyNodes.getLength(); i++) {
                Node node = parentDependencyNodes.item(i);
                if (node instanceof Element) {
                    Element parentDependencyEl = (Element) node;
                    if ("dependency".equals(parentDependencyEl.getTagName())) {
                        Element importedDependency = (Element) pom.importNode(parentDependencyEl, true);
                        dependenciesEl.appendChild(importedDependency);
                    }
                }
            }
        }
    }

    private static void mergeDependencyManagement(Document pom, Document parentPom) {
        Element dependencyManagementEl = JkUtilsXml.directChild(pom.getDocumentElement(), "dependencyManagement");
        Element parentDependencyManagementEl = JkUtilsXml.directChild(parentPom.getDocumentElement(),
                "dependencyManagement");

        if (parentDependencyManagementEl != null) {
            if (dependencyManagementEl == null) {
                dependencyManagementEl = pom.createElement("dependencyManagement");
                pom.getDocumentElement().appendChild(dependencyManagementEl);
            }

            Element dependenciesEl = JkUtilsXml.directChild(dependencyManagementEl, "dependencies");
            if (dependenciesEl == null) {
                dependenciesEl = pom.createElement("dependencies");
                dependencyManagementEl.appendChild(dependenciesEl);
            }

            Element parentDependenciesEl = JkUtilsXml.directChild(parentDependencyManagementEl, "dependencies");
            if (parentDependenciesEl != null) {
                NodeList parentDependencyNodes = parentDependenciesEl.getChildNodes();
                for (int i = 0; i < parentDependencyNodes.getLength(); i++) {
                    Node node = parentDependencyNodes.item(i);
                    if (node instanceof Element) {
                        Element parentDependencyEl = (Element) node;
                        if ("dependency".equals(parentDependencyEl.getTagName())) {
                            String groupId = JkUtilsXml.directChildText(parentDependencyEl, "groupId");
                            String artifactId = JkUtilsXml.directChildText(parentDependencyEl, "artifactId");

                            // Check if dependency management already exists (child overrides parent)
                            if (!dependencyManagementExists(dependenciesEl, groupId, artifactId)) {
                                Element importedDependency = (Element) pom.importNode(parentDependencyEl, true);
                                dependenciesEl.appendChild(importedDependency);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void mergeRepositories(Document pom, Document parentPom) {
        Element repositoriesEl = JkUtilsXml.directChild(pom.getDocumentElement(), "repositories");
        Element parentRepositoriesEl = JkUtilsXml.directChild(parentPom.getDocumentElement(), "repositories");

        if (parentRepositoriesEl != null) {
            if (repositoriesEl == null) {
                repositoriesEl = pom.createElement("repositories");
                pom.getDocumentElement().appendChild(repositoriesEl);
            }

            NodeList parentRepositoryNodes = parentRepositoriesEl.getChildNodes();
            for (int i = 0; i < parentRepositoryNodes.getLength(); i++) {
                Node node = parentRepositoryNodes.item(i);
                if (node instanceof Element) {
                    Element parentRepositoryEl = (Element) node;
                    if ("repository".equals(parentRepositoryEl.getTagName())) {
                        String id = JkUtilsXml.directChildText(parentRepositoryEl, "id");

                        // Check if repository already exists (child overrides parent)
                        if (!repositoryExists(repositoriesEl, id)) {
                            Element importedRepository = (Element) pom.importNode(parentRepositoryEl, true);
                            repositoriesEl.appendChild(importedRepository);
                        }
                    }
                }
            }
        }
    }

    private static boolean repositoryExists(Element repositoriesEl, String id) {
        NodeList repositoryNodes = repositoriesEl.getChildNodes();
        for (int i = 0; i < repositoryNodes.getLength(); i++) {
            Node node = repositoryNodes.item(i);
            if (node instanceof Element) {
                Element repositoryEl = (Element) node;
                if ("repository".equals(repositoryEl.getTagName())) {
                    String existingId = JkUtilsXml.directChildText(repositoryEl, "id");
                    if (Objects.equals(id, existingId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean dependencyManagementExists(Element dependenciesEl, String groupId, String artifactId) {
        NodeList dependencyNodes = dependenciesEl.getChildNodes();
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Node node = dependencyNodes.item(i);
            if (node instanceof Element) {
                Element dependencyEl = (Element) node;
                if ("dependency".equals(dependencyEl.getTagName())) {
                    String existingGroupId = JkUtilsXml.directChildText(dependencyEl, "groupId");
                    String existingArtifactId = JkUtilsXml.directChildText(dependencyEl, "artifactId");
                    if (Objects.equals(groupId, existingGroupId) && Objects.equals(artifactId, existingArtifactId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private Map<String, String> getPropertiesFromDom(Document document) {
        final Map<String, String> result = new HashMap<>();

        Element rootElement = document.getDocumentElement();
        Element propertiesEl = JkUtilsXml.directChild(rootElement, "properties");

        if (propertiesEl != null) {
            final NodeList nodeList = propertiesEl.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                final Node node = nodeList.item(i);
                if (node instanceof Element) {
                    final Element element = (Element) node;
                    result.put(element.getTagName(), element.getTextContent());
                }
            }
        }

        // Add built-in Maven properties
        String projectVersion = getVersionFromDom(document);
        if (projectVersion != null) {
            result.put("project.version", projectVersion);
        }

        String groupId = getGroupIdFromDom(document);
        if (groupId != null) {
            result.put("project.groupId", groupId);
        }

        String artifactId = getArtifactIdFromDom(document);
        if (artifactId != null) {
            result.put("project.artifactId", artifactId);
        }
        return result;
    }

    private static void interpolateElement(Element element, Map<String, String> properties) {

        // Interpolate text content
        if (hasOnlyTextContent(element)) {
            String textContent = element.getTextContent();
            if (textContent != null && !textContent.trim().isEmpty()) {
                String interpolatedText = interpolateText(textContent, properties);
                if (!textContent.equals(interpolatedText)) {
                    element.setTextContent(interpolatedText);
                }
            }
        }

        // Interpolate attributes
        if (element.hasAttributes()) {
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                Node attribute = element.getAttributes().item(i);
                String attributeValue = attribute.getNodeValue();
                if (attributeValue != null) {
                    String interpolatedValue = interpolateText(attributeValue, properties);
                    if (!attributeValue.equals(interpolatedValue)) {
                       attribute.setTextContent(interpolatedValue);
                    }
                }
            }
        }

        // Recursively interpolate child elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                interpolateElement((Element) node, properties);
            }
        }
    }

    private static String interpolateText(String text, Map<String, String> properties) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        int maxIterations = 10; // Prevent infinite loops
        boolean changed = true;

        for (int iteration = 0; iteration < maxIterations && changed; iteration++) {
            changed = false;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                if (result.contains(placeholder)) {
                    String value = entry.getValue();
                    if (value != null) {
                        result = result.replace(placeholder, value);
                        changed = true;
                    }
                }
            }
        }

        return result;
    }

    private static String getVersionFromDom(Document document) {
        Element rootElement = document.getDocumentElement();
        String version = JkUtilsXml.directChildText(rootElement, "version");
        if (version != null) {
            return version;
        }

        // Check parent version
        Element parentEl = JkUtilsXml.directChild(rootElement, "parent");
        if (parentEl != null) {
            return JkUtilsXml.directChildText(parentEl, "version");
        }

        return null;
    }

    private static String getGroupIdFromDom(Document document) {
        Element rootElement = document.getDocumentElement();
        String groupId = JkUtilsXml.directChildText(rootElement, "groupId");
        if (groupId != null) {
            return groupId;
        }

        // Check parent groupId
        Element parentEl = JkUtilsXml.directChild(rootElement, "parent");
        if (parentEl != null) {
            return JkUtilsXml.directChildText(parentEl, "groupId");
        }

        return null;
    }

    private static String getArtifactIdFromDom(Document document) {
        Element rootElement = document.getDocumentElement();
        return JkUtilsXml.directChildText(rootElement, "artifactId");
    }

    private static boolean hasChildElements(Element element) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOnlyTextContent(Element element) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                return false; // Found a child element
            }
        }
        return hasTextContent(element);
    }

    private static boolean hasTextContent(Element element) {
        String textContent = element.getTextContent();
        return textContent != null && !textContent.trim().isEmpty();
    }




}
