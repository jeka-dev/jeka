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
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dev.jeka.core.api.depmanagement.JkQualifiedDependencySet.*;

/**
 * Translates the dependencies of a Maven POM to JeKa.
 */
class PomMigration {

    private static final String POM_TYPE = "pom";

    // Maven types standing for a JAR with a classifier
    private static final Map<String, String> TYPE_TO_CLASSIFIER = Map.of(
            "test-jar", "tests",
            "java-source", "sources",
            "javadoc", "javadoc",
            "ejb-client", "client");

    // Other Maven types standing for a regular JAR
    private static final List<String> JAR_TYPES = List.of("jar", "ejb", "maven-plugin", "bundle");

    private final Path baseDir;

    private final JkPom effectivePom;

    private final List<JkCoordinate> boms = new LinkedList<>();

    private final Map<JkModuleId, JkVersion> versions = new LinkedHashMap<>();

    private PomMigration(Path baseDir, JkPom effectivePom) {
        this.baseDir = baseDir;
        this.effectivePom = effectivePom;
    }

    /**
     * @param baseDir The directory containing the pom.xml file.
     * @param effectivePom The effective POM of this pom.xml file.
     */
    static PomMigration of(Path baseDir, JkPom effectivePom) {
        PomMigration result = new PomMigration(baseDir, effectivePom);
        result.readManagedVersions(baseDir.resolve("pom.xml"));
        return result;
    }

    String toTxt() {
        StringBuilder sb = new StringBuilder();
        if (!boms.isEmpty() || !versions.isEmpty()) {
            List<JkDependency> managed = new LinkedList<>();
            boms.forEach(bom -> managed.add(JkCoordinateDependency.of(bom)));
            versions.forEach((moduleId, version) -> managed.add(JkCoordinateDependency.of(
                    JkCoordinate.of(moduleId, version))));
            sb.append("[version]\n").append(JkDependencySet.toTxt(managed, false)).append("\n\n");
        }
        sb.append("[compile]\n").append(txt(COMPILE_SCOPE));
        sb.append("\n\n[compile-only]\n").append(txt(PROVIDED_SCOPE));
        systemPaths().forEach(path -> sb.append(path.toString().replace('\\', '/')).append("\n"));
        sb.append("\n\n[runtime]\n").append(txt(RUNTIME_SCOPE));
        sb.append("\n\n[test]\n").append(txt(TEST_SCOPE));
        return sb.toString();
    }

    String toJavaCode(int indentCount) {
        String indent = JkUtilsString.repeat(" ", indentCount);
        StringBuilder sb = new StringBuilder("Compile:\n");
        if (!boms.isEmpty() || !versions.isEmpty()) {
            sb.append(indent).append(".addVersionProvider(JkVersionProvider.of()");
            boms.forEach(bom -> sb.append("\n").append(indent).append(indent)
                    .append(String.format(".andBom(\"%s:%s\")", bom.getModuleId().toColonNotation(), bom.getVersion())));
            versions.forEach((moduleId, version) -> sb.append("\n").append(indent).append(indent)
                    .append(String.format(".and(\"%s\", \"%s\")", moduleId.toColonNotation(), version)));
            sb.append(")\n");
        }
        sb.append(javaCode(indentCount, COMPILE_SCOPE, PROVIDED_SCOPE));
        systemPaths().forEach(path -> sb.append(indent).append(".add(").append(javaPath(path)).append(")\n"));

        sb.append("Runtime:\n").append(javaCode(indentCount, RUNTIME_SCOPE));
        sb.append(JkDependencySet.toJavaCode(indentCount, dependencies(dep -> true, PROVIDED_SCOPE), false));
        systemPaths().forEach(path -> sb.append(indent).append(".remove(").append(javaPath(path)).append(")\n"));

        sb.append("Test:\n").append(javaCode(indentCount, TEST_SCOPE));
        return sb.toString();
    }

    private String txt(String scope) {
        return JkDependencySet.toTxt(dependencies(dep -> !dep.isBom(), scope), false)
                + pomTypeComments("# ", scope);
    }

    private String javaCode(int indentCount, String ...scopes) {
        return JkDependencySet.toJavaCode(indentCount, dependencies(dep -> !dep.isBom(), scopes), true)
                + pomTypeComments(JkUtilsString.repeat(" ", indentCount) + "// ", scopes);
    }

    // JeKa considers dependencies of type 'pom' as BOMs, while Maven adds their transitive dependencies.
    private String pomTypeComments(String prefix, String ...scopes) {
        return dependencies(JkCoordinate::isBom, scopes).stream()
                .map(dep -> ((JkCoordinateDependency) dep).getCoordinate())
                .map(coordinate -> prefix + coordinate.getModuleId().toColonNotation() + ":" + coordinate.getVersion()
                        + " has type 'pom': declare the dependencies it brings instead\n")
                .collect(Collectors.joining());
    }

    private List<JkDependency> dependencies(Predicate<JkCoordinate> predicate, String ...scopes) {
        return effectivePom.getDependencies().getDependenciesHavingQualifier(scopes).stream()
                .map(JkCoordinateDependency.class::cast)
                .map(PomMigration::translate)
                .filter(dependency -> predicate.test(dependency.getCoordinate()))
                .map(JkDependency.class::cast)
                .toList();
    }

    private static JkCoordinateDependency translate(JkCoordinateDependency dependency) {
        JkCoordinate coordinate = dependency.getCoordinate();

        // Version ranges may contain spaces, which are not accepted in coordinates
        coordinate = coordinate.withVersion(JkVersion.of(coordinate.getVersion().getValue().replace(" ", "")));
        String type = coordinate.getArtifactSpecification().getType();
        String classifier = coordinate.getArtifactSpecification().getClassifier();
        if (type != null && classifier == null) {
            classifier = TYPE_TO_CLASSIFIER.get(type);
        }
        if (type != null && (TYPE_TO_CLASSIFIER.containsKey(type) || JAR_TYPES.contains(type))) {
            type = null;
        }

        // In JeKa, '*:*' would also exclude the dependency itself. Mentioning a type makes it non-transitive.
        boolean excludeAll = dependency.getExclusions().stream().anyMatch(PomMigration::excludesAll);
        if (excludeAll && classifier == null && type == null) {
            type = "jar";
        }
        coordinate = coordinate.withClassifierAndType(classifier, type);
        JkCoordinateDependency result = JkCoordinateDependency.of(coordinate);
        if (excludeAll) {
            return result;
        }

        // Contrary to Maven, JeKa does not include the transitive dependencies of secondary artifacts by default
        if (!coordinate.isTransitiveByDefault()) {
            result = result.withTransitivity(JkTransitivity.RUNTIME);
        }
        return result.andExclusions(dependency.getExclusions());
    }

    private static boolean excludesAll(JkDependencyExclusion exclusion) {
        return "*:*".equals(exclusion.getModuleId().toColonNotation());
    }

    private List<Path> systemPaths() {
        Path root = baseDir.toAbsolutePath().normalize();
        return effectivePom.getSystemPaths().stream()
                .map(path -> Paths.get(path).normalize())
                .map(path -> path.startsWith(root) ? root.relativize(path) : path)
                .toList();
    }

    private static String javaPath(Path path) {
        String pathString = path.toString().replace('\\', '/');
        return path.isAbsolute() ? "Paths.get(\"" + pathString + "\")" : "getBaseDir().resolve(\"" + pathString + "\")";
    }

    /*
     * Reads the dependencyManagement of the specified pom file and of its parents. The effective POM is not suitable
     * for this, as it contains the whole content of the BOMs instead of mentioning them.
     */
    private void readManagedVersions(Path pomFile) {
        JkPom pom = JkPom.of(pomFile);
        for (JkCoordinateDependency dependency : pom.getManagedDependencies()) {
            JkCoordinate managed = dependency.getCoordinate();
            JkCoordinate coordinate = JkCoordinate.of(interpolate(managed.getModuleId().getGroup()),
                    interpolate(managed.getModuleId().getName()), interpolate(managed.getVersion().getValue()));
            if (managed.isBom()) {
                addBom(coordinate);
            } else if (!coordinate.hasUnspecifiedVersion()) {
                versions.putIfAbsent(coordinate.getModuleId(), coordinate.getVersion());
            }
        }
        if (pom.getParentArtifactId() == null) {
            return;
        }
        Path parentFile = localParent(pomFile, pom);
        if (parentFile != null) {
            readManagedVersions(parentFile);
        } else if (inheritsManagedVersions()) {
            addBom(JkCoordinate.of(pom.getParentGroupId(), pom.getParentArtifactId(),
                    interpolate(pom.getParentVersion())));
        }
    }

    private void addBom(JkCoordinate coordinate) {
        JkCoordinate bom = coordinate.withType(POM_TYPE);
        if (!boms.contains(bom)) {
            boms.add(bom);
        }
    }

    // If we only found versions, we can tell if some others are inherited.
    private boolean inheritsManagedVersions() {
        return effectivePom.getManagedDependencies().stream()
                .anyMatch(dependency -> !versions.containsKey(dependency.getCoordinate().getModuleId()));
    }

    private static Path localParent(Path pomFile, JkPom pom) {
        String relativePath = pom.getParentRelativePath();
        if (relativePath == null) {
            return null;
        }
        Path result = pomFile.toAbsolutePath().getParent().resolve(relativePath).normalize();
        if (Files.isDirectory(result)) {
            result = result.resolve("pom.xml");
        }
        if (!Files.exists(result)) {
            return null;
        }

        // As Maven, ignore the file if it is not the one of the parent.
        JkPom parent = JkPom.of(result);
        String parentGroupId = JkUtilsString.nullToEmpty(parent.getGroupId() == null ?
                parent.getParentGroupId() : parent.getGroupId());
        boolean matches = Objects.equals(parent.getArtifactId(), pom.getParentArtifactId())
                && Objects.equals(parentGroupId, pom.getParentGroupId());
        return matches ? result : null;
    }

    // In parent POMs, project properties stand for the ones of the child.
    private String interpolate(String value) {
        Map<String, String> props = new HashMap<>(effectivePom.getProperties());
        props.put("project.groupId", effectivePom.getGroupId());
        props.put("project.artifactId", effectivePom.getArtifactId());
        props.put("project.version", effectivePom.getVersion());
        props.put("project.parent.groupId", effectivePom.getParentGroupId());
        props.put("project.parent.version", effectivePom.getParentVersion());
        String result = value.trim();
        for (Map.Entry<String, String> entry : props.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }

}
