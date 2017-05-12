package org.jerkar.tool;

import org.jerkar.api.java.JkClassLoader;

/**
 * Holds constants about project structures
 */
public final class JkConstants {

    public static final String BUILD_OUTPUT_PATH = "build/output";

    static final String BUILD_DEF_BIN_DIR_NAME = "build-classes";

    static final String BUILD_BOOT = "build/boot";

    static final String DEFAULT_JAVA_SOURCE = "src/main/java";

    static final Class<?> DEFAULT_BUILD_CLASS = JkClassLoader.current().load(
            "org.jerkar.tool.builtins.javabuild.JkJavaBuild");

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

}
