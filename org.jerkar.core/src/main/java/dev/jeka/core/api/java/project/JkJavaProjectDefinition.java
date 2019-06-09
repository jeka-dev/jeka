package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.JkJavaVersion;

/**
 * Minimal information necessary to generate metadata project file for IDE.
 */
public interface JkJavaProjectDefinition {

    default JkProjectSourceLayout getSourceLayout() {
        return JkProjectSourceLayout.ofMavenStyle();
    }

    default JkDependencySet getDependencies() {
        return JkDependencySet.of();
    }

    default JkJavaVersion getSourceVersion() {
        return JkJavaVersion.V8;
    }

}
