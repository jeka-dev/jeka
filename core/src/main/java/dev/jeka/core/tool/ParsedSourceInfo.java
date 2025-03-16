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

package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkFileDependency;
import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

final class ParsedSourceInfo {

    final LinkedHashSet<Path> importedBaseDirs;

    final List<String> compileOptions;

    private JkDependencySet dependencies;

    private JkDependencySet exportedDependencies;

    ParsedSourceInfo(
            JkDependencySet dependencies,
            LinkedHashSet<Path> importedBaseDirs,
            List<String> compileOptions,
            JkDependencySet exportedDependencies) {

        this.dependencies = dependencies;
        this.importedBaseDirs = importedBaseDirs;
        this.compileOptions = compileOptions;
        this.exportedDependencies = exportedDependencies;
    }

    ParsedSourceInfo() {
        this(JkDependencySet.of(), new LinkedHashSet<>(), new LinkedList<>(), JkDependencySet.of());
    }

    ParsedSourceInfo merge(ParsedSourceInfo other) {
        if (this.isEmpty()) {
            return other;
        } else if (other.isEmpty()) {
            return this;
        }
        LinkedHashSet<Path> mergedDependencyProjects = new LinkedHashSet<>(this.importedBaseDirs);
        mergedDependencyProjects.addAll(other.importedBaseDirs);
        return new ParsedSourceInfo(
                this.dependencies.and(other.dependencies),
                mergedDependencyProjects,
                JkUtilsIterable.concatLists(this.compileOptions, other.compileOptions),
                this.exportedDependencies.and(other.exportedDependencies));
    }

    void addDep(boolean exported, JkDependency dependency) {
        this.dependencies = dependencies.and(dependency);
        if (exported) {
            this.exportedDependencies = exportedDependencies.and(dependency);
        }
    }

    JkDependencySet getDependencies() {
        return dependencies;
    }

    JkDependencySet getExportedDependencies() {
        return exportedDependencies;
    }

    boolean hasPrivateDependencies() {
        return dependencies.getEntries().size() > exportedDependencies.getEntries().size();
    }

    private boolean isEmpty() {
        return this.compileOptions.isEmpty() &&
                this.exportedDependencies.getEntries().isEmpty() &&
                this.importedBaseDirs.isEmpty() &&
                this.dependencies.getEntries().isEmpty();
    }

    List<Path> getDepBaseDirs() {
        return dependencies.getEntries().stream()
                .filter(JkFileSystemDependency.class::isInstance)
                .map(JkFileSystemDependency.class::cast)
                .map(fsDep -> fsDep.getFiles().get(0))
                .filter(JkRunbase::isJekaProject)
                .collect(Collectors.toList());
    }

    JkDependencySet getSanitizedDeps() {
        List<JkDependency> sanitized = dependencies.getEntries().stream()
                .map(ParsedSourceInfo::sanitize)
                .collect(Collectors.toList());
        return JkDependencySet.of(sanitized);
    }

    private static JkDependency sanitize(JkDependency dependency) {
        if (dependency instanceof JkFileDependency) {
            JkFileSystemDependency fsDep = (JkFileSystemDependency) dependency;
            Path depPath = fsDep.getFiles().get(0);
            if (JkRunbase.isJekaProject(depPath)) {
                return JkFileSystemDependency.of(depPath.resolve(JkConstants.JEKA_SRC_CLASSES_DIR));
            }
        }
        return dependency;
    }

}
