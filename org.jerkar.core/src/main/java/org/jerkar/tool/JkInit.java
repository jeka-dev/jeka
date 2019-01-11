package org.jerkar.tool;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkUrlClassLoader;
import org.jerkar.api.system.JkInfo;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 * Class for instantiating builds while displaying meaningful information about environment on console.
 */
public final class JkInit {

    /**
     * Creates an instance of the specified run class and displays information about this class andPrepending environment.
     */
    public static <T extends JkRun> T instanceOf(Class<T> clazz, String... args) {
        JkLog.registerHierarchicalConsoleHandler();
        Environment.initialize(args);
        JkLog.Verbosity verbosity = JkLog.verbosity();
        if (!Environment.standardOptions.logHeaders) {
            JkLog.setVerbosity(JkLog.Verbosity.MUTE);
        }
        displayInfo();
        final T jkRun = JkRun.of(clazz);
        JkLog.info("Jerkar run is ready to start.");
        JkLog.setVerbosity(verbosity);
        return jkRun;
    }

    static void displayInfo() {
        StringBuilder sb = new StringBuilder()
                .append("\nWorking Directory : " + System.getProperty("user.dir"))
                .append("\nJava Home : " + System.getProperty("java.home"))
                .append("\nJava Version : " + System.getProperty("java.version") + ", "
                        + System.getProperty("java.vendor"))
                .append("\nJerkar Version : " + JkInfo.getJerkarVersion());
        if ( embedded(JkLocator.getJerkarHomeDir())) {
            sb.append("\nJerkar Home : " + bootDir() + " ( embedded !!! )");
        } else {
            sb.append("\nJerkar Home : " + JkLocator.getJerkarHomeDir());
        }
        sb.append("\nJerkar User Home : " + JkLocator.getJerkarUserHomeDir().toAbsolutePath().normalize());
        sb.append("\nJerkar Run Repositories : " + Engine.repos().toString());
        sb.append("\nJerkar Repository Cache : " + JkLocator.getJerkarRepositoryCache());
        sb.append("\nJerkar Classpath : " + System.getProperty("java.class.path"));
        sb.append("\nCommand Line : " + JkUtilsString.join(Arrays.asList(Environment.commandLine.rawArgs()), " "));
        sb.append(propsAsString("Specified System Properties", Environment.systemProps));
        sb.append("\nStandard Options : " + Environment.standardOptions);
        sb.append(propsAsString("Options", JkOptions.toDisplayedMap(JkOptions.getAll())));
        JkLog.info(sb.toString());
    }

    private final static String propsAsString(String message, Map<String, String> props) {
        StringBuilder sb = new StringBuilder();
        if (props.isEmpty()) {
            sb.append("\n" + message + " : none.");
        } else if (props.size() <= 3) {
            sb.append("\n" + message + " : " + JkUtilsIterable.toString(props));
        } else {
            sb.append("\n" + message + " : ");
            JkUtilsIterable.toStrings(props).forEach(line -> sb.append("  " + line));
        }
        return sb.toString();
    }

    private static boolean embedded(Path jarFolder) {
        if (!Files.exists(bootDir())) {
            return false;
        }
        return JkUtilsPath.isSameFile(bootDir(), jarFolder);
    }

    private static Path bootDir() {
        return Paths.get(JkConstants.BOOT_DIR);
    }

}
