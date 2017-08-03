package org.jerkar.api.depmanagement;

import java.io.Serializable;

/**
 * Information about problem when resolving dependencies
 */
public class JkModuleDepProblem implements Serializable {

    private static final long serialVersionUID = 1L;

    static JkModuleDepProblem of(JkModuleId moduleId, String version, String text) {
        return new JkModuleDepProblem(moduleId, JkVersionRange.of(version), text);
    }

    private final JkModuleId moduleId;

    private final JkVersionRange versionRange;

    private final String problemText;

    private JkModuleDepProblem(JkModuleId moduleId, JkVersionRange versionRange, String problemText) {
        this.moduleId = moduleId;
        this.versionRange = versionRange;
        this.problemText = problemText;
    }

    /**
     * Returns the modueId related to this problem.
     */
    public JkModuleId moduleId() {
        return moduleId;
    }

    /**
     * Returns the version range for which the problematic module dependency has been declared.
     */
    public JkVersionRange versionRange() {
        return versionRange;
    }

    /**
     * Returns the text explaining this problem.
     */
    public String problemText() {
        return problemText;
    }

    @Override
    public String toString() {
        return moduleId + ":" + versionRange + " -> " + problemText;
    }
}
