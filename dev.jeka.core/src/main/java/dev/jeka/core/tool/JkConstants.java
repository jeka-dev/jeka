package dev.jeka.core.tool;

import dev.jeka.core.api.file.JkPathMatcher;

/**
 * Holds constants about project structures
 */
public final class JkConstants {

    /**
     * Relative path to the project where Jeka elements lie.
     */
    public static final String JEKA_DIR = "jeka";

    /**
     * Relative path to the project base directory where output files are generated.
     */
    public static final String OUTPUT_PATH = "jeka-output";

    /**
     * Relative path to the project base directory where jeka work files are generated.
     */
    public static final String WORK_PATH = ".jeka-work";

    static final String DEF_BIN_DIR_NAME = "def-classes";

    static final String BOOT_DIR = "jeka-boot";

    /**
     * Relative path to the project where the def classes will be compiled.
     */
    public static final String DEF_BIN_DIR = WORK_PATH + "/" + DEF_BIN_DIR_NAME;

    public static final String KBEAN_CLASSES_CACHE_FILE_NAME = "kbean-classes.txt";

    /**
     * Relative path to the project where the def definition sources lie.
     */
    public static final String DEF_DIR = "jeka-src";

    public static final String CMD_PROP_PREFIX = "jeka.cmd.";

    public static final String CMD_APPEND_SUFFIX_PROP =  "_append";

    public static final String CLASSPATH_INJECT_PROP = "jeka.classpath.inject";

    public static final String DEFAULT_KBEAN_PROP = "jeka.default.kbean";

    public static final String JAVA_VERSION_PROP = "jeka.java.version";

    public static final String CMD_APPEND_PROP = CMD_PROP_PREFIX + CMD_APPEND_SUFFIX_PROP;

    public static final String CMD_SUBSTITUTE_SYMBOL = ":";

    public static final String PROPERTIES_FILE = "local.properties";

    public static final JkPathMatcher PRIVATE_IN_DEF_MATCHER = JkPathMatcher.of("_*", "_*/**");

}
