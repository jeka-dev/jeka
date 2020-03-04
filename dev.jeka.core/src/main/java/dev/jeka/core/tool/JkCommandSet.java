package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Base class for defining commands executable from command line.
 *
 * @author Jerome Angibaud
 */
public class JkCommandSet {

    private static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    static void baseDirContext(Path baseDir) {
        if (baseDir == null) {
            BASE_DIR_CONTEXT.set(null);
        } else {
            JkUtilsAssert.isTrue(baseDir.isAbsolute(), baseDir + " is not absolute");
            JkUtilsAssert.isTrue(Files.isDirectory(baseDir), baseDir + " is not a directory.");
            BASE_DIR_CONTEXT.set(baseDir.toAbsolutePath().normalize());
        }
    }

    private final Path baseDir;

    private JkCommandSetPlugins plugins;

    private JkDependencyResolver defDependencyResolver;

    private JkDependencySet defDependencies;

    private final JkImportedCommandSets importedCommandSets;

    // ------------------ options --------------------------------------------------------

    @JkDoc("Help options")
    private final JkHelpOptions help = new JkHelpOptions();

    // ------------------ Instantiation cycle  --------------------------------------

    /**
     * Constructs a {@link JkCommandSet}. Using constructor alone won't give you an instance populated with runtime options
     * neither decorated with plugins. <br/>
     * Use {@link JkCommandSet#of(Class)} to get instances populated with options and decorated with plugins.
     */
    protected JkCommandSet() {
        final Path baseDirContext = BASE_DIR_CONTEXT.get();
        JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir context : " + baseDirContext);
        this.baseDir = JkUtilsObject.firstNonNull(baseDirContext, Paths.get("").toAbsolutePath());
        JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir  : " + this.baseDir);

        // Instantiating imported runs
        this.importedCommandSets = JkImportedCommandSets.of(this);

        // Instantiate plugins
        this.plugins = new JkCommandSetPlugins(this, Environment.commandLine.getPluginOptions());
    }

    /**
     * Creates a instance of the specified command class (extending JkCommandSet), including option injection, plugin loading
     * and plugin activation.
     */
    public static <T extends JkCommandSet> T of(Class<T> commandClass) {
        JkLog.startTask("Instantiating commandSet class " + commandClass.getName() + " at " + BASE_DIR_CONTEXT.get());
        final T commands = ofUninitialised(commandClass);
        commands.initialise();
        JkLog.endTask();
        return commands;
    }

    void initialise() {
        setup();

        // initialise imported project after setup to let a chance master commands to modify imported commands
        // in the setup method.
        importedCommandSets.getDirects().forEach(JkCommandSet::initialise);

        for (JkPlugin plugin : new LinkedList<>(plugins.getLoadedPlugins())) {
            List<ProjectDef.CommandOptionDef> defs = ProjectDef.RunClassDef.of(plugin).optionDefs();
            JkLog.startTask("Activating plugin " + plugin.name() + " with options " + HelpDisplayer.optionValues(defs));
            try {
                plugin.activate();
            } catch (RuntimeException e) {
                JkLog.error("Plugin " + plugin.name() + " has caused build instantiation failure.");
                throw e;
            }
            JkLog.endTask();
        }

        // Extra run configuration
        setupAfterPluginActivations();
        List<ProjectDef.CommandOptionDef> defs = ProjectDef.RunClassDef.of(this).optionDefs();
        JkLog.info(this.getClass().getSimpleName() + " instance initialized with options " + HelpDisplayer.optionValues(defs));
        baseDirContext(null);
    }

    static <T extends JkCommandSet> T ofUninitialised(Class<T> commandClass) {
        if (BASE_DIR_CONTEXT.get() == null) {
            baseDirContext(Paths.get("").toAbsolutePath());
        }
        final T commands = JkUtilsReflect.newInstance(commandClass);
        final JkCommandSet jkCommandSet = commands;

        // Inject options & environment variables
        JkOptions.populateFields(commands, JkOptions.readSystemAndUserOptions());
        FieldInjector.injectEnv(commands);
        Set<String> unusedCmdOptions = JkOptions.populateFields(commands, Environment.commandLine.getCommandOptions());
        unusedCmdOptions.forEach(key -> JkLog.warn("Option '" + key
                + "' from command line does not match with any field of class " + commands.getClass().getName()));

        // Load plugins declared in command line and inject options
        jkCommandSet.plugins.loadCommandLinePlugins();
        List<JkPlugin> plugins = jkCommandSet.getPlugins().getLoadedPlugins();
        for (JkPlugin plugin : plugins) {
            if (!jkCommandSet.plugins.getLoadedPlugins().contains(plugin)) {
                jkCommandSet.plugins.injectOptions(plugin);
            }
        }
        return commands;
    }


    /**
     * This method is invoked right after options has been injected into this instance. Here, You will typically
     * configure plugins before they are activated.
     */
    protected void setup() {
        // Do nothing by default
    }

    /**
     * This method is called once all plugin has been activated. This method is intended to overwrite what plugin
     * activation has setup.
     */
    protected void setupAfterPluginActivations() {
        // Do nothing by default
    }


    // -------------------------------- accessors ---------------------------------------


    /**
     * Returns the base directory tree for this project. All file/directory path are
     * resolved to this directory. Short-hand for <code>JkPathTree.of(baseDir)</code>.
     */
    public final JkPathTree getBaseTree() {
        return JkPathTree.of(baseDir);
    }

    /**
     * Returns the base directory for this project.
     */
    public final Path getBaseDir() {
        return baseDir;
    }

    /**
     * Returns the output directory where all the final and intermediate artifacts are generated.
     */
    public Path getOutputDir() {
        return baseDir.resolve(JkConstants.OUTPUT_PATH);
    }

    /**
     * Returns the container of loaded plugins for this instance.
     */
    public JkCommandSetPlugins getPlugins() {
        return this.plugins;
    }

    /**
     * Shorthand to <code>getPlugins().get(<Pluginclass>)</code>.
     * Returns the plugin instance of the specified class loaded in the holding JkCommandSet instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkPlugin> T getPlugin(Class<T> pluginClass) {
        return getPlugins().get(pluginClass);
    }


    // ------------------------------ run dependencies --------------------------------

    void setDefDependencyResolver(JkDependencySet defDependencies, JkDependencyResolver scriptDependencyResolver) {
        this.defDependencies = defDependencies;
        this.defDependencyResolver = scriptDependencyResolver;
    }

    /**
     * Returns the dependency resolver used to compile/run scripts of this project.
     */
    public final JkDependencyResolver getDefDependencyResolver() {
        return this.defDependencyResolver;
    }

    /**
     * Dependencies necessary to compile the this command class. It is not the dependencies for building the project.
     */
    public final JkDependencySet getDefDependencies() {
        return defDependencies;
    }

    /**
     * Returns imported runs with plugins applied on.
     */
    public final JkImportedCommandSets getImportedCommandSets() {
        return importedCommandSets;
    }

    // ------------------------------ Command line methods ------------------------------

    /**
     * Cleans the output directory.
     */
    @JkDoc("Cleans the output directory.")
    public void clean() {
        JkLog.info("Clean output directory " + getOutputDir());
        if (Files.exists(getOutputDir())) {
            JkPathTree.of(getOutputDir()).deleteContent();
        }
    }

    /**
     * Displays all available methods defined in this run.
     */
    @JkDoc("Displays all available methods and options defined for this commandSet class.")
    public void help() {
        if (help.xml || help.xmlFile != null) {
            HelpDisplayer.help(this, help.xmlFile);
        } else {
            HelpDisplayer.help(this);
        }
    }

    // ----------------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.getClass().getName() + " at " + this.baseDir.toString();
    }

}
