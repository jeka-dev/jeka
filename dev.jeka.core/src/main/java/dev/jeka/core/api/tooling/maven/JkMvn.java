package dev.jeka.core.api.tooling.maven;

import dev.jeka.core.api.marshalling.xml.JkDomDocument;
import dev.jeka.core.api.system.JkAbstractProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Convenient class wrapping maven process.
 *
 * @author Jerome Angibaud
 */
public final class JkMvn extends JkAbstractProcess<JkMvn> {

    public static final String VERBOSE_ARG = "-X";

    public static final String FORCE_UPDATE_ARG = "-U";

    private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

    private static String cached_mvn_cmd;

    /**
     * Path to the local Maven repository dir containing user configuration and local repo.
     */
    public static final Path USER_M2_DIR = USER_HOME.resolve(".m2");

    private JkMvn(Path workingDir) {
        super();
    }

    private JkMvn(JkMvn other) {
        super(other);
    }

    /**
     * Creates a Maven command wrapper.
     */
    public static JkMvn of(Path workingDir) {
        return new JkMvn(workingDir).setWorkingDir(workingDir).setCommand(mvnCmd(workingDir));
    }

    /**
     * Returns the path to the local Maven repository.
     */
    public static Path getM2LocalRepo() {
        return USER_M2_DIR.resolve("repository");  // TODO naive implementation : handle settings.xml
    }

    /**
     * Returns the XML document representing the Maven settings file, or null if the file does not exist.
     */
    public static JkDomDocument settingsXmlDoc() {
        Path file = USER_M2_DIR.resolve("settings");
        if (!Files.exists(file)) {
            return null;
        }
        return JkDomDocument.parse(file);
    }

    @Override
    protected JkMvn copy() {
        return new JkMvn(this);
    }

    private JkProcess process() {
        return JkProcess.of(mvnCmd(this.getWorkingDir()))
                .setLogCommand(true)
                .setLogWithJekaDecorator(true);
    }

    // TODO handle mvn wrapper
    private static String mvnCmd(Path workingDir) {
        if (JkUtilsSystem.IS_WINDOWS) {
            if (exist("mvn.bat")) {
                return "mvn.bat";
            } else if (exist("mvn.cmd")) {
                return "mvn.cmd";
            } else {
                return null;
            }
        }
        if (exist("mvn")) {
            return "mvn";
        }
        return null;
    }

    private static boolean exist(String cmd) {
        String command = cmd + " -version";
        try {
            final int result = Runtime.getRuntime().exec(command).waitFor();
            return result == 0;
        } catch (final Exception e) {  //NOSONAR
            JkLog.verbose("Error while executing command '%s' : %s", command, e.getMessage());
            return false;
        }
    }



}
