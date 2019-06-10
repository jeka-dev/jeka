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

    static final String DEF_BIN_DIR_NAME = "def-classes";

    static final String BOOT_DIR = JEKA_DIR + "/boot";

    static final Class<? extends JkCommands> DEFAULT_RUN_CLASS = JkCommands.class;

    /**
     * Relative path to the project where the def classes will be
     * compiled.
     */
    public static final String DEF_BIN_DIR = OUTPUT_PATH + "/" + DEF_BIN_DIR_NAME;



    /**
     * Relative path to the project where the def definition sources lie.
     */
    public static final String DEF_DIR = JEKA_DIR + "/def";



    /**
     * The default method to be invoked when none is specified.
     */
    public static final String DEFAULT_METHOD = "help";

}
