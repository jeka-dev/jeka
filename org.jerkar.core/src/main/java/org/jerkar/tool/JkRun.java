package org.jerkar.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.*;

/**
 * Base class for defining runs. All run classes must extend this class in order
 * to be run with Jerkar.
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

        // Instantiating imported builds
        this.importedRuns = JkImportedRuns.of(this.baseTree().root(), this);

        this.plugins = new JkRunPlugins(this, Environment.commandLine.getPluginOptions());
    }

    /**
     * Creates a instance of the specified build class (extending JkRun), including option injection, plugin loading
     * and plugin activation.
     */
    public static <T extends JkRun> T of(Class<T> runClass) {
        if (BASE_DIR_CONTEXT.get() == null) {
            baseDirContext(Paths.get("").toAbsolutePath());
        }
        JkLog.startTask("Initializing class " + runClass.getName() + " at " + BASE_DIR_CONTEXT.get());
        final T build = JkUtilsReflect.newInstance(runClass);
        final JkRun jkRun = build;

        // Inject options
        JkOptions.populateFields(build, JkOptions.readSystemAndUserOptions());
        final Map<String, String> options = Environment.commandLine.getOptions();
        JkOptions.populateFields(build, options);

        // Load plugins declared in command line and inject options
        jkRun.plugins.loadCommandLinePlugins();
        for (JkPlugin plugin : jkRun.plugins().all()) {
           jkRun.plugins.injectOptions(plugin);
        }

        build.afterOptionsInjected();

        jkRun.plugins.loadCommandLinePlugins();
        for (JkPlugin plugin : jkRun.plugins().all()) {
            List<ProjectDef.BuildOptionDef> defs = ProjectDef.BuildClassDef.of(plugin).optionDefs();
            try {
                plugin.activate();
            } catch (RuntimeException e) {
                throw new RuntimeException("Plugin " + plugin.name() + " has caused build instantiation failure.", e);
            }
            String pluginInfo = "Instance decorated with plugin " + plugin.getClass()
                    + HelpDisplayer.optionValues(defs);
            JkLog.info(pluginInfo);
        }

        // Extra build configuration
        build.afterPluginsActivated();
        List<ProjectDef.BuildOptionDef> defs = ProjectDef.BuildClassDef.of(build).optionDefs();
        JkLog.info("Build instance initialized with options " + HelpDisplayer.optionValues(defs));
        JkLog.endTask();
        baseDirContext(null);


        return build;
    }

    /**
     * This method is invoked right after options has been injected into this instance. You will typically
     * setup plugins here before they decorate this build.
     */
    protected void afterOptionsInjected() {
        // Do nothing by default
    }

    /**
     * This method is called once all plugin has decorated this build.
     */
    protected void afterPluginsActivated() {
        // Do nothing by default
    }


    // -------------------------------- accessors ---------------------------------------


    /**
     * Returns the base directory tree for this project. All file/directory path are
     * resolved to this directory. Short-hand for <code>JkPathTree.of(baseDir)</code>.
     */
    public final JkPathTree baseTree() {
        return JkPathTree.of(baseDir);
    }

    /**
     * Returns the base directory for this project.
     */
    public final Path baseDir() {
        return baseDir;
    }

    /**
     * The output directory where all the final and intermediate artifacts are generated.
     */
    public Path outputDir() {
        return baseDir.resolve(JkConstants.OUTPUT_PATH);
    }

    /**
     * Returns the container of loaded plugins for this instance.
     */
    public JkRunPlugins plugins() {
        return this.plugins;
    }


    // ------------------------------ build dependencies --------------------------------

    void setRunDependencyResolver(JkDependencySet buildDependencies, JkDependencyResolver scriptDependencyResolver) {
        this.runDependencies = buildDependencies;
        this.runDefDependencyResolver = scriptDependencyResolver;
    }

    /**
     * Returns the dependency resolver used to compile/run scripts of this
     * project.
     */
    public final JkDependencyResolver runDependencyResolver() {
        return this.runDefDependencyResolver;
    }

    /**
     * Dependencies necessary to compile the this build class. It is not the dependencies for building the project.
     */
    public final JkDependencySet runDependencies() {
        return runDependencies;
    }

    /**
     * Returns imported builds with plugins applied on.
     */
    public final JkImportedRuns importedRuns() {
        return importedRuns;
    }

    // ------------------------------ Command line methods ------------------------------

    /**
     * Clean the output directory.
     */
    @JkDoc("Cleans the output directory except the compiled run classes.")
    public void clean() {
        JkLog.info("Clean output directory " + outputDir());
        if (Files.exists(outputDir())) {
            JkPathTree.of(outputDir()).andRefuse(JkConstants.DEF_BIN_DIR_NAME + "/**").deleteContent();
        }
    }

    /**
     * Displays all available methods defined in this build.
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
