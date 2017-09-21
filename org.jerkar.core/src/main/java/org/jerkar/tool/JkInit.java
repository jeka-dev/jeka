package org.jerkar.tool;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.system.JkInfo;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.CommandLine.JkPluginSetup;

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
    public static <T extends JkBuild> T instanceOf(File base, String... args) {
        final JkInit init = JkInit.of(args);
        init.displayInfo();
        final Project project = new Project(base);
        final T result = (T) project.instantiate(init);
        if (result == null) {
            throw new JkException("No build class found for project located at : " + base.getPath());
        }
        JkLog.info("Build class " + result.getClass().getName());
        JkLog.info("Activated plugins : " + result.plugins.getActives());
        return result;
    }

    /**
     * Creates an instance of the specified build class. the build instance is
     * configured according specified command line arguments and option files
     * found in running environment.
     */
    public static <T extends JkBuild> T instanceOf(Class<T> clazz, String... args) {
        return instanceOf(clazz, JkUtilsFile.workingDir(), args);
    }

    /**
     * Creates an instance of the specified build class. the build instance is
     * configured according specified command line arguments and option files
     * found in running environment. The base directory is the specified one.
     */
    public static <T extends JkBuild> T instanceOf(Class<T> clazz, File baseDir, String... args) {
        final JkInit init = JkInit.of(args);
        init.displayInfo();
        JkBuild.baseDirContext(baseDir);
        final T build;
        try {
            build = JkUtilsReflect.newInstance(clazz);
        } finally {
            JkBuild.baseDirContext(null);
        }
        init.initProject(build);
        JkLog.info("Build class " + build.getClass().getName());
        JkLog.info("Activated plugins : " + build.plugins.getActives());
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
        JkLog.info("Java Version : " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        if ( embedded(JkLocator.jerkarHome())) {
            JkLog.info("Jerkar Home : " + bootDir() + " ( embedded !!! )");
        } else {
            JkLog.info("Jerkar Home : " + JkLocator.jerkarHome());
        }
        JkLog.info("Jerkar User Home : " + JkLocator.jerkarUserHome().getAbsolutePath());
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
        JkUtilsTool.setSystemProperties(sysProps);
        final Map<String, String> optionMap = new HashMap<String, String>();
        optionMap.putAll(loadOptionsProperties());
        final CommandLine commandLine = CommandLine.of(args);
        optionMap.putAll(commandLine.getSubProjectBuildOptions());
        optionMap.putAll(commandLine.getMasterBuildOptions());
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
        final Map<String, String> result = new TreeMap<String, String>();
        final File propFile = new File(JkLocator.jerkarHome(), "system.properties");
        if (propFile.exists()) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
        }
        result.putAll(JkUtilsTool.userSystemProperties());
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

    PluginDictionnary<JkBuildPlugin> initProject(JkBuild build) {
        final CommandLine commandLine = this.loadResult.commandLine;
        JkOptions.populateFields(build, commandLine.getMasterBuildOptions());
        build.init();

        // setup plugins activated in command line
        final Class<JkBuildPlugin> baseClass = JkClassLoader.of(build.getClass()).load(JkBuildPlugin.class.getName());
        final PluginDictionnary<JkBuildPlugin> dictionnary = PluginDictionnary.of(baseClass);
        final List<JkBuild> importedBuilds = build.importedBuilds().all();
        if (!importedBuilds.isEmpty()) {
            JkLog.startHeaded("Configure imported builds");
            for (final JkBuild subBuild : importedBuilds) {
                JkLog.startln("Configure build " + build.baseDir().relativePath(subBuild.baseDir().root()));
                configureBuild(subBuild, commandLine.getSubProjectPluginSetups(),
                        commandLine.getSubProjectBuildOptions(), dictionnary);
                JkLog.done();
            }
            JkLog.done();
        }
        configureBuild(build, commandLine.getMasterPluginSetups(), commandLine.getMasterBuildOptions(), dictionnary);
        return dictionnary;

    }

    private static Map<String, String> loadOptionsProperties() {
        final File propFile = new File(JkLocator.jerkarHome(), "options.properties");
        final Map<String, String> result = new HashMap<String, String>();
        if (propFile.exists()) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(propFile));
        }
        final File userPropFile = new File(JkLocator.jerkarUserHome(), "options.properties");
        if (userPropFile.exists()) {
            result.putAll(JkUtilsFile.readPropertyFileAsMap(userPropFile));
        }
        return result;
    }

    private static void configureBuild(JkBuild build, Collection<JkPluginSetup> pluginSetups,
                                       Map<String, String> commandlineOptions, PluginDictionnary<JkBuildPlugin> dictionnary) {
        JkOptions.populateFields(build);
        final File localProps = build.file(JkConstants.BUILD_DEF_DIR + "/build.properties");
        if (localProps.exists()) {
            JkOptions.populateFields(build, JkUtilsFile.readPropertyFileAsMap(localProps));
        }
        JkOptions.populateFields(build, commandlineOptions);
        configureAndActivatePlugins(build, pluginSetups, dictionnary);
    }

    private static void configureAndActivatePlugins(JkBuild build, Collection<JkPluginSetup> pluginSetups,
            PluginDictionnary<JkBuildPlugin> dictionnary) {
        for (final JkPluginSetup pluginSetup : pluginSetups) {
            final Class<? extends JkBuildPlugin> pluginClass = dictionnary.loadByNameOrFail(pluginSetup.pluginName)
                    .pluginClass();
            if (pluginSetup.activated) {
                JkLog.startln("Activating plugin " + pluginClass.getName());
                final Object plugin = build.plugins.addActivated(pluginClass, pluginSetup.options);
                JkLog.done("Activating plugin " + pluginClass.getName() + " with options "
                        + JkOptions.fieldOptionsToString(plugin));
            } else {
                JkLog.startln("Configuring plugin " + pluginClass.getName());
                final Object plugin = build.plugins.addConfigured(pluginClass, pluginSetup.options);
                JkLog.done("Configuring plugin " + pluginClass.getName() + " with options "
                        + JkOptions.fieldOptionsToString(plugin));
            }
        }
    }

    private static boolean embedded(File jarFolder) {
        return JkUtilsFile.isSame(bootDir(), jarFolder);
    }

    private static File bootDir() {
        return new File("./build/boot");
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
