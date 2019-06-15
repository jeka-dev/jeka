package dev.jeka.core.api.depmanagement;

import java.io.Serializable;

/**
 * Information about problem when resolving dependencies
 */
public class JkModuleDepProblem implements Serializable {

    private static final long serialVersionUID = 1L;

    public static JkModuleDepProblem of(JkModuleId moduleId, String version, String text) {
        return new JkModuleDepProblem(moduleId, JkVersion.of(version), text);
    }

    private final JkModuleId moduleId;

    private final JkVersion version;

    private final String problemText;

    private JkModuleDepProblem(JkModuleId moduleId, JkVersion version, String problemText) {
        this.moduleId = moduleId;
        this.version= version;
        this.problemText = problemText;
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
