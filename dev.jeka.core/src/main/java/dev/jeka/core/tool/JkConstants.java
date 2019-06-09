package dev.jeka.core.tool;

/**
 * Holds constants about project structures
 */
public final class JkConstants {

    /**
     * Relative path to the project where Jerkar elements lie.
     */
    public static final String JERKAR_DIR = "jeka";

    /**
     * Relative path to the project base directory where output files are generated.
     */
    public static final String OUTPUT_PATH = JERKAR_DIR + "/output";

    static final String DEF_BIN_DIR_NAME = "def-classes";

    static final String BOOT_DIR = JERKAR_DIR + "/boot";

    static final Class<? extends JkRun> DEFAULT_RUN_CLASS = JkRun.class;

    /**
     * Relative path to the project where the def classes will be
     * compiled.
     */
    public static final String DEF_BIN_DIR = OUTPUT_PATH + "/" + DEF_BIN_DIR_NAME;



    /**
     * Relative path to the project where the def definition sources lie.
     */
    public static final String DEF_DIR = JERKAR_DIR + "/def";



    /**
     * The default method to be invoked when none is specified.
     */
    public static final String DEFAULT_METHOD = "help";

}
