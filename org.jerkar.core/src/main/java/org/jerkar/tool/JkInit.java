package org.jerkar.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jerkar.api.system.JkInfo;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;

/**
 * Initializer for build instances. It instantiates build class and inject
 * values according command line and option files.
 */
public final class JkInit {

    private final LoadResult loadResult;

    private final String[] args; // command line arguments

    private JkInit(LoadResult loadResult, String[] args) {
        super();
        this.loadResult = loadResult;
        this.args = args;
    }

    static JkInit of(String[] args) {
        final LoadResult loadResult = loadOptionsAndSystemProps(args);
        return new JkInit(loadResult, args);
    }

    /**
     * Creates an instance of the build class for the specified project. It is
     * slower than {@link #instanceOf(Class, String...)} cause it needs
     * compilation prior instantiating the object.
     */
    @SuppressWarnings("unchecked")
    public static <T extends JkBuild> T instanceOf(Path base, String... args) {
        final JkInit init = JkInit.of(args);
        init.displayInfo();
        final Engine engine = new Engine(base.toAbsolutePath().normalize());
        final T result = (T) engine.instantiate(init);
        if (result == null) {
            throw new JkException("No build class found for engine located at : " + base);
        }
        JkLog.info("Build class : " + result.getClass().getName());
        return result;
    }

    /**
     * Creates an instance of the specified build class. the build instance is
     * configured according specified command line arguments and option files
     * found in running environment.
     */
    public static <T extends JkBuild> T instanceOf(Class<T> clazz, String... args) {
        return instanceOf(clazz, Paths.get("").toAbsolutePath(), args);
    }

    /**
     * Creates an instance of the specified build class. the build instance is
     * configured according specified command line arguments and option files
     * found in running environment. The base directory is the specified one.
     */
    public static <T extends JkBuild> T instanceOf(Class<T> clazz, Path baseDir, String... args) {
        final JkInit init = JkInit.of(args);
        init.displayInfo();
        JkBuild.baseDirContext(baseDir);
        final T build;
        try {
            build = JkUtilsReflect.newInstance(clazz);
        } finally {
            JkBuild.baseDirContext(null);
        }
        final Map<String, String> displayedOptions = JkOptions.toDisplayedMap(OptionInjector.injectedFields(build));
        if (JkLog.verbose()) {
            JkInit.logProps("Field values", displayedOptions);
        }
        return build;
    }

    /**
     * As {@link #instanceOf(Class, String...)} but you can specified the
     * command line using two distinct arrays that will be concatenated.
     */
    public static <T extends JkBuild> T instanceOf(Class<T> clazz, String[] args, String... extraArgs) {
        return instanceOf(clazz, JkUtilsIterable.concat(args, extraArgs));
    }

    void displayInfo() {
        JkLog.info("Jerkar Version : " + JkInfo.jerkarVersion());
        JkLog.info("Working Directory : " + System.getProperty("user.dir"));
        JkLog.info("Java Home : " + System.getProperty("java.home"));
        JkLog.info("Java Version : " + System.getProperty("java.projectVersion") + ", " + System.getProperty("java.vendor"));
        if ( embedded(JkLocator.jerkarHomeDir())) {
            JkLog.info("Jerkar Home : " + bootDir() + " ( embedded !!! )");
        } else {
            JkLog.info("Jerkar Home : " + JkLocator.jerkarHomeDir());
        }
        JkLog.info("Jerkar User Home : " + JkLocator.jerkarUserHomeDir().toAbsolutePath().normalize());
        JkLog.info("Jerkar Repository Cache : " + JkLocator.jerkarRepositoryCache());
        JkLog.info("Jerkar Classpath : " + System.getProperty("java.class.path"));
        JkLog.info("Command Line : " + JkUtilsString.join(Arrays.asList(args), " "));
        logProps("Specified System Properties", loadResult.sysprops);
        JkLog.info("Standard Options : " + loadResult.standardOptions);
        logProps("Options", JkOptions.toDisplayedMap(JkOptions.getAll()));
    }

    CommandLine commandLine() {
        return this.loadResult.commandLine;
    }

    String buildClassHint() {
        return loadResult.standardOptions.buildClass;
    }

    private static LoadResult loadOptionsAndSystemProps(String[] args) {
        final Map<String, String> sysProps = getSpecifiedSystemProps(args);
        setSystemProperties(sysProps);
        final Map<String, String> optionMap = new HashMap<>();
        optionMap.putAll(JkOptions.readSystemAndUserOptions());
        CommandLine.init(args);
        final CommandLine commandLine = CommandLine.instance();
        optionMap.putAll(commandLine.getBuildOptions());
        if (!JkOptions.isPopulated()) {
            JkOptions.init(optionMap);
        }
        final JkInit.StandardOptions standardOptions = new JkInit.StandardOptions();
        JkOptions.populateFields(standardOptions, optionMap);
        JkLog.silent(standardOptions.silent);
        JkLog.verbose(standardOptions.verbose);

        JkOptions.populateFields(standardOptions);
        final JkInit.LoadResult loadResult = new JkInit.LoadResult();
        loadResult.sysprops = sysProps;
        loadResult.commandLine = commandLine;
        loadResult.standardOptions = standardOptions;
        return loadResult;
    }

    static void logProps(String message, Map<String, String> props) {
        if (props.isEmpty()) {
            JkLog.info(message + " : none.");
        } else if (props.size() <= 3) {
            JkLog.info(message + " : " + JkUtilsIterable.toString(props));
        } else {
            JkLog.info(message + " : ");
            JkLog.delta(1);
            JkLog.info(JkUtilsIterable.toStrings(props));
            JkLog.delta(-1);
        }
    }

    private static Map<String, String> getSpecifiedSystemProps(String[] args) {
        final Map<String, String> result = new TreeMap<>();
        final Path propFile = JkLocator.jerkarHomeDir().resolve("system.properties");
        if (Files.exists(propFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
        }
        result.putAll(userSystemProperties());
        for (final String arg : args) {
            if (arg.startsWith("-D")) {
                final int equalIndex = arg.indexOf("=");
                if (equalIndex <= -1) {
                    result.put(arg.substring(2), "");
                } else {
                    final String name = arg.substring(2, equalIndex);
                    final String value = arg.substring(equalIndex + 1);
                    result.put(name, value);
                }
            }
        }
        return result;
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

    private static Map<String, String> userSystemProperties() {
        final Map<String, String> result = new HashMap<>();
        final Path userPropFile = JkLocator.jerkarUserHomeDir().resolve("system.properties");
        if (Files.exists(userPropFile)) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
        }
        return result;
    }

    private static void setSystemProperties(Map<String, String> props) {
        for (final Map.Entry<String, String> entry : props.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private static class LoadResult {

        Map<String, String> sysprops;

        CommandLine commandLine;

        private JkInit.StandardOptions standardOptions;

    }

    private static class StandardOptions {

        boolean verbose;

        boolean silent;

        String buildClass;

        @Override
        public String toString() {
            return "buildClass=" + JkUtilsObject.toString(buildClass) + ", verbose=" + verbose + ", silent=" + silent;
        }

    }

}
