package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.depmanagement.JkVersionedModule;

/**
 * Information about problem when resolving dependencies
 */
public class JkModuleDepProblem {

    private final JkModuleId moduleId;

    private final JkVersion version;

    private final String problemText;

    private JkModuleDepProblem(JkModuleId moduleId, JkVersion version, String problemText) {
        this.moduleId = moduleId;
        this.version= version;
        this.problemText = problemText;
    }

    public static JkModuleDepProblem of(JkModuleId moduleId, String version, String text) {
        return new JkModuleDepProblem(moduleId, JkVersion.of(version), text);
    }

    public static JkModuleDepProblem of(JkVersionedModule versionedModule, String text) {
        return of(versionedModule.getModuleId(), versionedModule.getVersion().getValue(), text);
    }

    /**
     * Returns the getModuleId related to this problem.
     */
    public JkModuleId getModuleId() {
        return moduleId;
    }

    /**
     * Returns the version range for which the problematic module dependency has been declared.
     */
    public JkVersion getVersion() {
        return version;
    }

    /**
     * Returns the text explaining this problem.
     */
    public String getProblemText() {
        return problemText;
    }

    @Override
    public String toString() {
        return moduleId + ":" + version + " -> " + problemText;
    }
}
