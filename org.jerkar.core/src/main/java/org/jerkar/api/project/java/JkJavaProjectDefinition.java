package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.JkProjectSourceLayout;

/**
 * Minimal information necessary to generate metadata project file for IDE.
 */
public interface JkJavaProjectDefinition {

    default JkProjectSourceLayout getSourceLayout() {
        return JkProjectSourceLayout.mavenJava();
    }

    default JkDependencies getDependencies() {
        return JkDependencies.of();
    }

    default JkJavaCompileVersion getCompileVersion() {
        return JkJavaCompileVersion.V8;
    }

}
