package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

class ParsedSourceInfo {

    private JkDependencySet dependencies;

    final LinkedHashSet<Path> dependencyProjects;

    final List<String> compileOptions;

    private JkDependencySet exportedDependencies;

    ParsedSourceInfo(
            JkDependencySet dependencies,
            LinkedHashSet<Path> dependencyProjects,
            List<String> compileOptions,
            JkDependencySet exportedDependencies) {

        this.dependencies = dependencies;
        this.dependencyProjects = dependencyProjects;
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
        LinkedHashSet<Path> mergedDependencyProjects = new LinkedHashSet<>(this.dependencyProjects);
        mergedDependencyProjects.addAll(other.dependencyProjects);
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
                this.dependencyProjects.isEmpty() &&
                this.dependencies.getEntries().isEmpty();
    }

}
