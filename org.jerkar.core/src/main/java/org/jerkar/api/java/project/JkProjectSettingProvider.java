package org.jerkar.api.java.project;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.java.JkJavaVersion;

import java.util.Map;

/**
 * Minimal information necessary to generate metadata project file for IDE.
 */
public interface JkProjectSettingProvider {

    default JkProjectSourceLayout getSourceLayout() {
        return JkProjectSourceLayout.mavenJava();
    }

    default JkDependencies getDependencies(Map<String, String> options) {
        return JkDependencies.of();
    }

    default JkJavaVersion getSourceVersion() {
        return null;
    }

    default JkJavaVersion getTargetVersion() {
        return null;
    }
}
