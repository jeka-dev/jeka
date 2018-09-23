package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.JkProjectSourceLayout;

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
