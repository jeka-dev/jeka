package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkVersionProvider;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.system.JkInfo;

/**
 * Holds constants about project structures
 */
public final class JkConstants {

    // ------------ Standard file names and locations --------------------------

    /**
     * Relative path to the project base directory where output files are generated.
     */
    public static final String OUTPUT_PATH = "jeka-output";

    /**
     * Relative path to the project base directory where jeka work files are generated.
     */
    public static final String JEKA_WORK_PATH = ".jeka-work";

    /**
     * Relative path to put jars that will be automatically prepended to jeka classpath.
     */
    static final String JEKA_BOOT_DIR = "jeka-boot";

    /**
     * Relative path to the project where the jeka-src classes will be compiled.
     */
    public static final String JEKA_SRC_CLASSES_DIR = JEKA_WORK_PATH + "/jeka-src-classes";

    /**
     * Relative path of jeka-src dir to the base dir.
     */
    public static final String JEKA_SRC_DIR = "jeka-src";

    /**
     * Relative path to the jeka.properties.
     */
    public static final String PROPERTIES_FILE = "jeka.properties";

    // ------------ Jeka standard properties --------------------------

    static final String CMD_PREFIX_PROP = "jeka.cmd.";

    static final String CMD_APPEND_SUFFIX_PROP =  "_append";

    public static final String CLASSPATH_INJECT_PROP = "jeka.inject.classpath";

    public static final String DEFAULT_KBEAN_PROP = "jeka.default.kbean";

    static final String CMD_APPEND_PROP = CMD_PREFIX_PROP + CMD_APPEND_SUFFIX_PROP;

    static final String CMD_SUBSTITUTE_SYMBOL = ":";

    // --------------------  Misc ----------------------------------------------

    public static final JkPathMatcher PRIVATE_IN_DEF_MATCHER = JkPathMatcher.of("_*", "_*/**");

    /*
     * If version is not specified for dependencies of 'dev.jeka' group, then use the running Jeka version
     */
    static final JkVersionProvider JEKA_VERSION_PROVIDER = JkVersionProvider.of("dev.jeka:*",
            JkInfo.getJekaVersion());
    static final String KBEAN_CMD_SUFFIX = ":";
}
