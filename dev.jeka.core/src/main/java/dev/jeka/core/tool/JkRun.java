package dev.jeka.core.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;


/**
 * Base class for defining runs. All run classes must extend this class in order to be run with Jeka.
 *
 * @author Jerome Angibaud
 */
public class JkRun {

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

    private JkRunPlugins plugins;

    private JkDependencyResolver runDefDependencyResolver;

    private JkDependencySet runDependencies;

    private final JkImportedRuns importedRuns;

    // ------------------ options --------------------------------------------------------

    @JkDoc("Help options")
    private final JkHelpOptions help = new JkHelpOptions();

    // ------------------ Instantiation cycle  --------------------------------------

    /**
     * Constructs a {@link JkRun}. Using constructor alone won't give you an instance populated with runtime options
     * neither decorated with plugins. <br/>
     * Use {@link JkRun#of(Class)} to get instances populated with options and decorated with plugins.
     */
    protected JkRun() {
        final Path baseDirContext = BASE_DIR_CONTEXT.get();
        JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir context : " + baseDirContext);
        this.baseDir = JkUtilsObject.firstNonNull(baseDirContext, Paths.get("").toAbsolutePath());
        JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir  : " + this.baseDir);

        // Instantiating imported runs
        this.importedRuns = JkImportedRuns.of(this.getBaseTree().getRoot(), this);

        this.plugins = new JkRunPlugins(this, Environment.commandLine.getPluginOptions());
    }

    /**
     * Creates a instance of the specified run class (extending JkRun), including option injection, plugin loading
     * and plugin activation.
     */
    public static <T extends JkRun> T of(Class<T> runClass) {
        if (BASE_DIR_CONTEXT.get() == null) {
            baseDirContext(Paths.get("").toAbsolutePath());
        }
        JkLog.startTask("Initializing class " + runClass.getName() + " at " + BASE_DIR_CONTEXT.get());
        final T run = JkUtilsReflect.newInstance(runClass);
        final JkRun jkRun = run;

        // Inject options & environment variables
        JkOptions.populateFields(run, JkOptions.readSystemAndUserOptions());
        FieldInjector.injectEnv(run);
        JkOptions.populateFields(run,  Environment.commandLine.getOptions());

        // Load plugins declared in command line and inject options
        jkRun.plugins.loadCommandLinePlugins();
        List<JkPlugin> plugins = jkRun.getPlugins().getAll();
        for (JkPlugin plugin : plugins) {
           jkRun.plugins.injectOptions(plugin);
        }
        run.setup();
        for (JkPlugin plugin : new LinkedList<>(plugins)) {
            List<ProjectDef.RunOptionDef> defs = ProjectDef.RunClassDef.of(plugin).optionDefs();
            try {
                plugin.activate();
            } catch (RuntimeException e) {
                JkLog.setVerbosity(JkLog.Verbosity.NORMAL);
                JkLog.error("Plugin " + plugin.name() + " has caused build instantiation failure.");
                throw e;
            }
            String pluginInfo = "Instance decorated with plugin " + plugin.getClass()
                    + HelpDisplayer.optionValues(defs);
            JkLog.info(pluginInfo);
        }

        // Extra run configuration
        run.setupAfterPluginActivations();
        List<ProjectDef.RunOptionDef> defs = ProjectDef.RunClassDef.of(run).optionDefs();
        JkLog.info("Run instance initialized with options " + HelpDisplayer.optionValues(defs));
        JkLog.endTask();
        baseDirContext(null);

        return run;
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
    public JkRunPlugins getPlugins() {
        return this.plugins;
    }

    /**
     * Shorthand to <code>getPlugins().get(<Pluginclass>)</code>.
     * Returns the plugin instance of the specified class loaded in the holding JkRun instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkPlugin> T getPlugin(Class<T> pluginClass) {
        return getPlugins().get(pluginClass);
    }


    // ------------------------------ run dependencies --------------------------------

    void setRunDependencyResolver(JkDependencySet runDependencies, JkDependencyResolver scriptDependencyResolver) {
        this.runDependencies = runDependencies;
        this.runDefDependencyResolver = scriptDependencyResolver;
    }

    /**
     * Returns the dependency resolver used to compile/run scripts of this project.
     */
    public final JkDependencyResolver getRunDependencyResolver() {
        return this.runDefDependencyResolver;
    }

    /**
     * Dependencies necessary to compile the this run class. It is not the dependencies for building the project.
     */
    public final JkDependencySet getRunDependencies() {
        return runDependencies;
    }

    /**
     * Returns imported runs with plugins applied on.
     */
    public final JkImportedRuns getImportedRuns() {
        return importedRuns;
    }

    // ------------------------------ Command line methods ------------------------------

    /**
     * Cleans the output directory.
     */
    @JkDoc("Cleans the output directory except the compiled run classes.")
    public void clean() {
        JkLog.info("Clean output directory " + getOutputDir());
        if (Files.exists(getOutputDir())) {
            JkPathTree.of(getOutputDir()).andMatching(false, JkConstants.DEF_BIN_DIR_NAME + "/**")
                    .deleteContent();
        }
    }

    /**
     * Displays all available methods defined in this run.
     */
    @JkDoc("Displays all available methods and options defined for this run class.")
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
