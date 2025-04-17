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

package dev.jeka.core.api.project;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JkDependenciesTxt {

    public static final String COMPILE = "compile";

    static final String COMPILE_ONLY = "compile-only";

    public static final String RUNTIME = "runtime";

    public static final String TEST = "test";

    private static final String VERSION = "version";

    private static final String FILE_NAME = "dependencies.txt";

    private static final String MINUS_SYMBOL = "-";

    private static final String LOCAL_EXCLUDE_SYMBOL = "@";

    private static final String GLOBAL_EXCLUDE_SYMBOL = "@@";

    private final Map<String, JkDependencySet> dependencySets;

    private final JkVersionProvider versionProvider;

    private JkDependenciesTxt(Map<String, JkDependencySet> dependencySets, JkVersionProvider versionProvider) {
        this.dependencySets = dependencySets;
        this.versionProvider = versionProvider;
    }

    public static List<Path> getModuleDependencies(Path baseDir) {
        Path dependenciesTxtFile = baseDir.resolve(FILE_NAME);
        if (!Files.exists(dependenciesTxtFile)) {
            return Collections.emptyList();
        }
        return JkUtilsPath.readAllLines(dependenciesTxtFile).stream()
                .map(String::trim)
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !line.startsWith("-"))
                .filter(line -> !line.isEmpty())
                .filter(line -> !JkDependency.isCoordinate(line))
                .map(baseDir::resolve)
                .filter(Files::exists)
                .filter(JkLocator::isJekaProject)
                .map(Path::normalize)
                .distinct()
                .collect(Collectors.toList());
    }

    static JkDependenciesTxt parse(Path dependenciesXmlPath) {
        Map<String, JkDependencySet> raw = parseFile(dependenciesXmlPath);

        // Manage Version provider
        JkVersionProvider versionProvider = JkVersionProvider.of();
        for (JkDependency dependency : raw.getOrDefault(VERSION, JkDependencySet.of()).getEntries()) {
            if (! (dependency instanceof JkCoordinateDependency)) {
                String msg = String.format("%s file : %s mentioned in [version] section is not a valid coordinate" +
                                " as `groupId:artifactId:version`",
                        dependenciesXmlPath, dependency);
                throw new IllegalStateException(msg);
            }
            JkCoordinateDependency coordinateDependency = (JkCoordinateDependency) dependency;
            JkCoordinate coordinate = coordinateDependency.getCoordinate();
            if (coordinate.getVersion() == null) {
                String msg = String.format("%s file : %s instruction in [version] section should mention a version as " +
                                "`groupId:artifactId:version`",
                        dependenciesXmlPath, dependency);
                throw new IllegalStateException(msg);
            }
            if (coordinate.isBom()) {
                versionProvider = versionProvider.andBom(coordinate);
            } else {
                versionProvider = versionProvider.and(coordinate.getModuleId(), coordinate.getVersion());
            }
        }

        Path parent = dependenciesXmlPath.getParent().getParent().resolve("dependencies.txt");
        if (Files.exists(parent)) {
            JkDependenciesTxt parsedDependenciesTxt = JkDependenciesTxt.parse(parent);
            versionProvider = parsedDependenciesTxt.versionProvider.and(versionProvider);
        }

        return new JkDependenciesTxt(raw, versionProvider);
    }

    JkDependencySet getDependencies(String section) {
        return dependencySets.get(section);
    }

    JkVersionProvider getVersionProvider() {
        return versionProvider;
    }

    public JkDependencySet computeCompileDeps() {
        return dependencySets.get(COMPILE).and(dependencySets.get(COMPILE_ONLY)).andVersionProvider(versionProvider);
    }

    public JkDependencySet computeRuntimeDeps() {
        return dependencySets.get(RUNTIME).and(dependencySets.get(COMPILE)).andVersionProvider(versionProvider);
    }

    public JkDependencySet computeTestDeps() {
        return dependencySets.get(TEST).and(computeRuntimeDeps());
    }

    private static Map<String, JkDependencySet> parseFile(Path dependenciesXmlPath) {
        Map<String, JkDependencySet> result = new LinkedHashMap<>();
        Map<String, List<String>> sectionLines = parseToStrings(dependenciesXmlPath);
        sectionLines.forEach((key, value) -> {
            JkDependencySet dependencySet = JkDependencySet.of();
            for (String line : value) {
                dependencySet = apply(line, dependencySet);
            }
            result.putIfAbsent(key, dependencySet);
        });
        return result;
    }

    private static Map<String, List<String>> parseToStrings(Path path) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        List<String> currentList = null;
        String currentSection;

        for (String line : JkUtilsPath.readAllLines(path)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.contains("#")) {
                line = JkUtilsString.substringBeforeFirst(line, "#").trim();
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1);
                currentList = new ArrayList<>();
                sections.put(currentSection, currentList);
            } else if (currentList != null)  {
                currentList.add(line);
            }
        }
        return sections;
    }

    private static JkDependencySet apply(String line, JkDependencySet current) {
        if (line.startsWith(LOCAL_EXCLUDE_SYMBOL)) {
            String coordinate = line.substring(LOCAL_EXCLUDE_SYMBOL.length()).trim();
            return current.withLocalExclusions(coordinate);
        }
        if (line.startsWith(GLOBAL_EXCLUDE_SYMBOL)) {
            String coordinate = line.substring(GLOBAL_EXCLUDE_SYMBOL.length()).trim();
            return current.withGlobalExclusions(coordinate);
        } else {
            return current.and(translate(line));
        }
    }

    private static JkDependency translate(String line) {
        return JkDependency.of(line);
    }
}
