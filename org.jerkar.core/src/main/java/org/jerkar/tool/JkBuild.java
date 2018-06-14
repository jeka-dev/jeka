package org.jerkar.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsObject;
import org.jerkar.api.utils.JkUtilsReflect;

/**
 * Base build class for defining builds. All build classes must extend this class in order
 * to be run with Jerkar.
 *
 * @author Jerome Angibaud
 */
public class JkBuild {

    private static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> MASTER_BUILD = new ThreadLocal<>();

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

    private JkBuildPlugins plugins;

    private JkDependencyResolver buildDefDependencyResolver;

    private JkDependencies buildDependencies;

    private final JkImportedBuilds importedBuilds;

    private final JkRunnables defaulter = JkRunnables.noOp();

    private JkScaffolder scaffolder;

    private final StringBuilder infoProvider = new StringBuilder();

    // ------------------ options --------------------------------------------------------


    @JkDoc("Help options")
    private final JkHelpOptions help = new JkHelpOptions();

    @JkDoc("Embed Jerkar jar along bin script in the project while scaffolding so the project can be run without Jerkar installed.")
    boolean scaffoldEmbed;


    // ------------------ Instantiation cycle  --------------------------------------

    /**
     * Constructs a {@link JkBuild}. Using constructor alone won't give you an instance populated with runtime options
     * neither decorated with plugins. <br/>
     * Use {@link JkBuild#of(Class)} to get instances populated with options and decorated with plugins.
     */
    protected JkBuild() {
        final Path baseDirContext = BASE_DIR_CONTEXT.get();
        JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir context : " + baseDirContext);
        this.baseDir = JkUtilsObject.firstNonNull(baseDirContext, Paths.get("").toAbsolutePath());
        JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir  : " + this.baseDir);

        // Instantiating imported builds
        this.importedBuilds = JkImportedBuilds.of(this.baseTree().root(), this);
        this.infoProvider.append("base directory : " + this.baseDir + "\n"
                + "imported builds : " + this.importedBuilds.directs() + "\n");
    }

    public static <T extends JkBuild> T of(Class<T> buildClass) {
        JkLog.startTask("Initializing class " + buildClass.getName() + " at " + BASE_DIR_CONTEXT.get());
        final T build = JkUtilsReflect.newInstance(buildClass);
        final JkBuild jkBuild = (JkBuild) build;

        // plugins must be instantiated before setupOptionsDefault is invoked.
        jkBuild.plugins = new JkBuildPlugins(build, CommandLine.instance().getPluginOptions());

        // Allow sub-classes to define defaults prior options are injected
        build.setupOptionDefaults();

        // Inject options
        JkOptions.populateFields(build, JkOptions.readSystemAndUserOptions());
        final Map<String, String> options = CommandLine.instance().getBuildOptions();
        JkOptions.populateFields(build, options);

        // Load plugins declared in command line
        build.configurePlugins();
        jkBuild.plugins.loadCommandLinePlugins();
        jkBuild.plugins.all().forEach(plugin -> {
            List<ProjectDef.BuildOptionDef> defs = ProjectDef.BuildClassDef.of(plugin).optionDefs();
            plugin.decorateBuild();
            JkLog.info("Instance decorated with plugin " + plugin.getClass()
                    + HelpDisplayer.optionValues(defs));
        });

        // Extra build configuration
        build.configure();
        List<ProjectDef.BuildOptionDef> defs = ProjectDef.BuildClassDef.of(build).optionDefs();
        JkLog.info("Build instance initialized with options " + HelpDisplayer.optionValues(defs));
        JkLog.endTask("Done.");
        return build;
    }

    /**
     * Override this method to set sensitive defaults for options on this build or plugins.<br/>
     * This method is invoked before options are injected into build instance, so options specified in
     * command line or configuration files will overwrite the default values you have defined here. <p/>
     * Note you should call <code>super()</code> at the beginning of the method in order to not wipe defaults
     * that superclasses may have defined.
     */
    protected void setupOptionDefaults() {
        // Do nothing by default
    }

    /**
     * This method is invoked right after options has been injected into this instance.
     */
    protected void configurePlugins() {
        // Do nothing by default
    }

    /**
     * This method is called once all plugin has decorated this build.
     */
    protected void configure() {
        // Do nothing by default
    }


    // -------------------------------- accessors ---------------------------------------


    /**
     * Returns the base directory tree for this project. All file/directory path are
     * resolved to this directory.
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
        return baseDir.resolve(JkConstants.BUILD_OUTPUT_PATH);
    }

    /**
     * Returns the scaffolder object in charge of doing the scaffolding for this build.
     * Override this method if you write a template class that need to do custom action for scaffolding.
     */
    public final JkScaffolder scaffolder() {
        if (this.scaffolder == null) {
            this.scaffolder = createScaffolder();
        }
        return this.scaffolder;
    }

    public final StringBuilder infoProvider() {
        return infoProvider;
    }

    public JkBuildPlugins plugins() {
        return this.plugins;
    }

    protected JkScaffolder createScaffolder() {
        JkScaffolder scaffolder = new JkScaffolder(this.baseDir, this.scaffoldEmbed);
        scaffolder.setBuildClassCode(JkUtilsIO.read(JkBuild.class.getResource("buildclass.snippet")));
        return scaffolder;
    }

    protected void addDefaultOperation(Runnable runnable) {
        this.defaulter.chain(runnable);
    }


    // ------------------------------ build dependencies --------------------------------

    void setBuildDefDependencyResolver(JkDependencies buildDependencies, JkDependencyResolver scriptDependencyResolver) {
        this.buildDependencies = buildDependencies;
        this.buildDefDependencyResolver = scriptDependencyResolver;
    }

    /**
     * Returns the dependency resolver used to compile/run scripts of this
     * project.
     */
    public final JkDependencyResolver buildDependencyResolver() {
        return this.buildDefDependencyResolver;
    }

    /**
     * Dependencies necessary to compile the this build class. It is not the dependencies for building the project.
     */
    public final JkDependencies buildDependencies() {
        return buildDependencies;
    }

    /**
     * Returns imported builds with plugins applied on.
     */
    public final JkImportedBuilds importedBuilds() {
        return importedBuilds;
    }

    // ------------------------------ Command line methods ------------------------------

    /**
     * Creates the project structure (mainly project folder layout, build class code and IDE metadata) at the asScopedDependency
     * of the current project.
     */
    @JkDoc("Creates the project structure")
    public final void scaffold() {
        scaffolder().run();
    }

    /**
     * Clean the output directory.
     */
    @JkDoc("Cleans the output directory.")
    public void clean() {
        JkLog.execute("Cleaning output directory " + outputDir(), () ->
            {
                if (Files.exists(outputDir())) {
                    JkPathTree.of(outputDir()).refuse(JkConstants.BUILD_DEF_BIN_DIR_NAME + "/**").deleteContent();
                }
            });
    }

    /**
     * Conventional method standing for the default operations to perform.
     *
     * @throws Exception
     */
    @JkDoc("Conventional method standing for the default operations to perform.")
    public void doDefault() {
        defaulter.run();
    }

    /**
     * Displays all available methods defined in this build.
     */
    @JkDoc("Displays all available methods defined in this build.")
    public void help() {
        if (help.xml || help.xmlFile != null) {
            HelpDisplayer.help(this, help.xmlFile);
        } else {
            HelpDisplayer.help(this);
            ;
        }
    }

    /**
     * Displays meaningful information about this build.
     */
    @JkDoc("Displays meaningful information about this build.")
    public final void info() {
        JkLog.info(infoProvider.toString());
    }

    // ----------------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.getClass().getName() + " at " + this.baseDir.toString();
    }

}
