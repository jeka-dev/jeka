package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsObject;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Base class for defining methods executable from command line.
 *
 * @author Jerome Angibaud
 */
public class JkClass {

    private static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    static void baseDirContext(Path baseDir) {
        if (baseDir == null) {
            BASE_DIR_CONTEXT.set(null);
        } else {
            JkUtilsAssert.argument(baseDir.isAbsolute(), baseDir + " is not absolute");
            JkUtilsAssert.argument(Files.isDirectory(baseDir), baseDir + " is not a directory.");
            BASE_DIR_CONTEXT.set(baseDir.toAbsolutePath().normalize());
        }
    }

    private final Path baseDir;

    private JkClassPlugins plugins;

    private JkDependencyResolver defDependencyResolver;

    private JkDependencySet defDependencies;

    private final JkImportedJkClasses importedJkClasses;

    // ------------------ options --------------------------------------------------------

    @JkDoc("Help options")
    private final JkHelpOptions help = new JkHelpOptions();

    // ------------------ Instantiation cycle  --------------------------------------

    /**
     * Constructs a {@link JkClass}. Using constructor alone won't give you an instance populated with runtime options
     * neither decorated with plugins. <br/>
     * Use {@link JkClass#of(Class)} to get instances populated with options and decorated with plugins.
     */
    protected JkClass() {
        final Path baseDirContext = BASE_DIR_CONTEXT.get();
        JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir context : " + baseDirContext);
        this.baseDir = JkUtilsObject.firstNonNull(baseDirContext, Paths.get("").toAbsolutePath());
        JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir  : " + this.baseDir);

        // Instantiating imported runs
        this.importedJkClasses = JkImportedJkClasses.of(this);

        // Instantiate plugins
        this.plugins = new JkClassPlugins(this, Environment.commandLine.getPluginOptions());
    }

    /**
     * Creates a instance of the specified Jeka class (extending JkClass), including option injection, plugin loading
     * and plugin activation.
     */
    public static <T extends JkClass> T of(Class<T> jkClass) {
        JkLog.startTask("Instantiating Jeka class " + jkClass.getName() + " at " + BASE_DIR_CONTEXT.get());
        final T jkClassInstance = ofUninitialized(jkClass);
        try {
            jkClassInstance.initialise();
        } catch (Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
        JkLog.endTask();
        return jkClassInstance;
    }

    void initialise() throws Exception {
        setup();

        // initialise imported projects after setup to let a chance master Jeka class
        // to modify imported Jeka classes in the setup method.
        for (JkClass jkClass : importedJkClasses.getDirects()) {
            jkClass.initialise();
        }

        for (JkPlugin plugin : new LinkedList<>(plugins.getLoadedPlugins())) {
            List<ProjectDef.JkClassOptionDef> defs = ProjectDef.RunClassDef.of(plugin).optionDefs();
            JkLog.startTask("Activating Plugin " + plugin.name() + " with options "
                    + HelpDisplayer.optionValues(defs));
            try {
                plugin.afterSetup();
            } catch (RuntimeException e) {
                JkLog.error("Plugin " + plugin.name() + " has caused build instantiation failure.");
                throw e;
            }
            JkLog.endTask();
        }

        // Extra run configuration
        postSetup();
        baseDirContext(null);
    }

    static <T extends JkClass> T ofUninitialized(Class<T> jkClass) {
        if (BASE_DIR_CONTEXT.get() == null) {
            baseDirContext(Paths.get("").toAbsolutePath());
        }
        final T jkCkass = JkUtilsReflect.newInstance(jkClass);
        final JkClass jkClassInstance = jkCkass;

        // Inject options & environment variables
        JkOptions.populateFields(jkCkass, JkOptions.readSystemAndUserOptions());
        JkOptions.populateFields(jkCkass, JkOptions.readFromProjectOptionsProperties(
                Optional.ofNullable(BASE_DIR_CONTEXT.get()).orElse(Paths.get(""))));
        FieldInjector.injectEnv(jkCkass);
        Set<String> unusedCmdOptions = JkOptions.populateFields(jkCkass, Environment.commandLine.getCommandOptions());
        unusedCmdOptions.forEach(key -> JkLog.warn("Option '" + key
                + "' from command line does not match with any field of class " + jkCkass.getClass().getName()));

        // Load plugins declared in command line and inject options
        jkClassInstance.plugins.loadCommandLinePlugins();
        List<JkPlugin> plugins = jkClassInstance.getPlugins().getLoadedPlugins();
        for (JkPlugin plugin : plugins) {
            if (!jkClassInstance.plugins.getLoadedPlugins().contains(plugin)) {
                jkClassInstance.plugins.injectOptions(plugin);
            }
        }
        return jkCkass;
    }

    /**
     * Configure your build and plugins here.
     * At this point, option values has been injected and {@link JkPlugin#beforeSetup()} method invoked on all plugins.
     */
    protected void setup() throws Exception {
        // Do nothing by default
    }

    /**
     * This method is called once {@link JkPlugin#afterSetup()} method has been invoked on all plugins.
     * It gives a chance to the Jeka class to override some plugin settings.
     */
    protected void postSetup() throws Exception {
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
    public JkClassPlugins getPlugins() {
        return this.plugins;
    }

    /**
     * Shorthand to <code>getPlugins().get(<Pluginclass>)</code>.
     * Returns the plugin instance of the specified class loaded in the holding JkClass instance. If it does not hold
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
     * Dependencies necessary to compile the this Jeka class. It is not the dependencies for building the project.
     */
    public final JkDependencySet getDefDependencies() {
        return defDependencies;
    }

    /**
     * Returns imported runs with plugins applied on.
     */
    public final JkImportedJkClasses getImportedJkClasses() {
        return importedJkClasses;
    }

    // ------------------------------ Command line methods ------------------------------

    /**
     * Cleans the output directory.
     */
    @JkDoc("Cleans the output directory.")
    public void clean() {
        Path path = getOutputDir();
        Path relPath = path.isAbsolute() ? Paths.get("").toAbsolutePath().relativize(path) : path;
        JkLog.info("Clean output directory " + relPath);
        if (Files.exists(path)) {
            JkPathTree.of(path).deleteContent();
        }
    }

    /**
     * Displays all available methods defined in this run.
     */
    @JkDoc("Displays all available methods and options defined for this Jeka class.")
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
