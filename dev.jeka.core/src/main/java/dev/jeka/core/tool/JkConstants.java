package dev.jeka.core.tool;

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
    public static final String OUTPUT_PATH = JEKA_DIR + "/output";

    /**
     * Relative path to the project base directory where jeka work files are generated.
     */
    public static final String WORK_PATH = JEKA_DIR + "/.work";

    public static final String GLOBAL_PROPERTIES = "global.properties";

    static final String DEF_BIN_DIR_NAME = "def-classes";

    static final String BOOT_DIR = JEKA_DIR + "/boot";

    /**
     * Relative path to the project where the def classes will be
     * compiled.
     */
    public static final String DEF_BIN_DIR = WORK_PATH + "/" + DEF_BIN_DIR_NAME;

    public static final String KBEAN_CLASSES_CACHE_FILE_NAME = "kbean-classes.txt";



    /**
     * Relative path to the project where the def definition sources lie.
     */
    public static final String DEF_DIR = JEKA_DIR + "/def";

    public static final String CMD_PROPERTIES = "cmd.properties";

    public static final String PROJECT_PROPERTIES = "project.properties";

}
