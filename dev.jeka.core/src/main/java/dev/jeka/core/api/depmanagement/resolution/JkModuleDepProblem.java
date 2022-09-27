package dev.jeka.core.api.depmanagement.resolution;

import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkCoordinate.GroupAndName;
import dev.jeka.core.api.depmanagement.JkVersion;

/**
 * Information about problem when resolving dependencies
 */
public class JkModuleDepProblem {

    private final GroupAndName groupAndName;

    private final JkVersion version;

    private final String problemText;

    private JkModuleDepProblem(GroupAndName groupAndName, JkVersion version, String problemText) {
        this.groupAndName = groupAndName;
        this.version= version;
        this.problemText = problemText;
    }

    public static JkModuleDepProblem of(GroupAndName groupAndName, String version, String text) {
        return new JkModuleDepProblem(groupAndName, JkVersion.of(version), text);
    }

    public static JkModuleDepProblem of(JkCoordinate coordinate, String text) {
        return of(coordinate.getGroupAndName(), coordinate.getVersion().getValue(), text);
    }

    /**
     * Returns the getModuleId related to this problem.
     */
    public GroupAndName getGroupAndName() {
        return groupAndName;
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
        return groupAndName + ":" + version + " -> " + problemText;
    }
}
