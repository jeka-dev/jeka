package org.jerkar.tool;

import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * Holds constants about project structures
 */
public final class JkConstants {

    /**
     * Relative path to the project base directory where output files are generated.
     */
    public static final String BUILD_OUTPUT_PATH = "build/output";

    static final String BUILD_DEF_BIN_DIR_NAME = "build-classes";

    static final String BUILD_BOOT = "build/boot";

    static final String DEFAULT_JAVA_SOURCE = "src/main/java";

    static final Class<?> DEFAULT_BUILD_CLASS = DefaultBuildClass.class;

    /**
     * Relative path to the project where the build definition classes will be
     * compiled.
     */
    public static final String BUILD_DEF_BIN_DIR = BUILD_OUTPUT_PATH + "/" + BUILD_DEF_BIN_DIR_NAME;

    /**
     * Relative path to the project where the build definition sources are.
     */
    public static final String BUILD_DEF_DIR = "build/def";

    /**
     * The default method to be invoked when none is specified.
     */
    public static final String DEFAULT_METHOD = "doDefault";

    private static class DefaultBuildClass extends JkJavaProjectBuild {

        @Override
        protected JkJavaProject createProject() {
            return defaultProject();
        }

    }

}
