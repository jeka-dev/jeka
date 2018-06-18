package org.jerkar.tool;

import org.jerkar.api.system.JkInfo;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;

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
     * Creates an instance of the specified build class and displays information about build class and environment.
     */
    public static <T extends JkBuild> T instanceOf(Class<T> clazz, String... args) {
        JkLog.register(new LogHandler());
        OptionsAndCommandLine optionsAndCommandLine = OptionsAndCommandLine.loadOptionsAndSystemProps(args);
        if (!optionsAndCommandLine.standardOptions.hideHeaders) {
            displayInfo(OptionsAndCommandLine.loadOptionsAndSystemProps(args));
        }
        final T build = JkBuild.of(clazz);
        if (!optionsAndCommandLine.standardOptions.hideHeaders) {
            JkLog.info("Build is ready to start.");
        }
        return build;
    }

    static void displayInfo(OptionsAndCommandLine optionsAndCommandLine) {
        StringBuilder sb = new StringBuilder()
                .append("\nWorking Directory : " + System.getProperty("user.dir"))
                .append("\nJava Home : " + System.getProperty("java.home"))
                .append("\nJava Version : " + System.getProperty("java.version") + ", "
                        + System.getProperty("java.vendor"))
                .append("\nJerkar Version : " + JkInfo.jerkarVersion());
        if ( embedded(JkLocator.jerkarHomeDir())) {
            sb.append("\nJerkar Home : " + bootDir() + " ( embedded !!! )");
        } else {
            sb.append("\nJerkar Home : " + JkLocator.jerkarHomeDir());
        }
        sb.append("\nJerkar User Home : " + JkLocator.jerkarUserHomeDir().toAbsolutePath().normalize());
        sb.append("\nJerkar Repository Cache : " + JkLocator.jerkarRepositoryCache());
        sb.append("\nJerkar Classpath : " + System.getProperty("java.class.path"));
        sb.append("\nCommand Line : " + JkUtilsString.join(Arrays.asList(optionsAndCommandLine.commandLine.rawArgs()), " "));
        sb.append(propsAsString("Specified System Properties", optionsAndCommandLine.sysprops));
        sb.append("\nStandard Options : " + optionsAndCommandLine.standardOptions);
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
        return Paths.get("./build/boot");
    }

}
