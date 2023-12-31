package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.utils.JkUtilsIterable;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

class ParsedSourceInfo {

    JkDependencySet dependencies;

    final LinkedHashSet<Path> dependencyProjects;

    final List<String> compileOptions;

    ParsedSourceInfo(JkDependencySet dependencies, LinkedHashSet<Path> dependencyProjects, List<String> compileOptions) {
        this.dependencies = dependencies;
        this.dependencyProjects = dependencyProjects;
        this.compileOptions = compileOptions;
    }

    ParsedSourceInfo() {
        this(JkDependencySet.of(), new LinkedHashSet<>(), new LinkedList<>());
    }

    ParsedSourceInfo merge(ParsedSourceInfo other) {
        LinkedHashSet<Path> allDependencyProjects = new LinkedHashSet<>(this.dependencyProjects);
        allDependencyProjects.addAll(other.dependencyProjects);
        return new ParsedSourceInfo(this.dependencies.and(other.dependencies),
                allDependencyProjects,
                JkUtilsIterable.concatLists(this.compileOptions, other.compileOptions));
    }

    void addDep(JkDependency dependency) {
        this.dependencies = dependencies.and(dependency);
    }

}
