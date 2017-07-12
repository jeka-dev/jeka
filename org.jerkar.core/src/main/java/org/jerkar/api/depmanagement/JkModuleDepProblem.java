package org.jerkar.api.depmanagement;

import java.io.Serializable;

/**
 * Information about problem when resolving dependencies
 */
public class JkModuleDepProblem implements Serializable {

    static JkModuleDepProblem of(JkModuleId moduleId, String version, String text) {
        return new JkModuleDepProblem(moduleId, JkVersionRange.of(version), text);
    }

    private JkModuleId moduleId;

    private JkVersionRange versionRange;

    private String problemText;

    private JkModuleDepProblem(JkModuleId moduleId, JkVersionRange versionRange, String problemText) {
        this.moduleId = moduleId;
        this.versionRange = versionRange;
        this.problemText = problemText;
    }

    public JkModuleId getModuleId() {
        return moduleId;
    }

    public JkVersionRange getVersionRange() {
        return versionRange;
    }

    public String getProblemText() {
        return problemText;
    }

    @Override
    public String toString() {
        return moduleId + ":" + versionRange + " -> " + problemText;
    }
}
