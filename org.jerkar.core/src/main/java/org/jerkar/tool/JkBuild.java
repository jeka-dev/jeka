package org.jerkar.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.function.JkRunnables;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsObject;

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

    public final JkBuildPlugins plugins = new JkBuildPlugins(this);

    private JkDependencyResolver buildDefDependencyResolver;

    private JkDependencies buildDependencies;

    private final JkImportedBuilds importedBuilds;

    private final JkRunnables defaulter = JkRunnables.noOp();

    private JkScaffolder scaffolder;

    private final StringBuilder infoProvider = new StringBuilder();


    // ------------------ Instantiation cycle cycle --------------------------------------

    /**
     * Constructs a {@link JkBuild}
     */
    public JkBuild() {
        final Path baseDirContext = BASE_DIR_CONTEXT.get();
        final boolean isMaster;
        if (MASTER_BUILD.get() == null) {
            isMaster = true;
            MASTER_BUILD.set(true);
        } else {
            isMaster = false;
        }
        try {
            JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir context : " + baseDirContext);
            this.baseDir = JkUtilsObject.firstNonNull(baseDirContext, Paths.get("").toAbsolutePath());
            JkLog.trace("Initializing " + this.getClass().getName() + " instance with base dir  : " + this.baseDir);

            // Instantiating imported builds
            MASTER_BUILD.set(false);
            this.importedBuilds = JkImportedBuilds.of(this.baseTree().root(), this);
            MASTER_BUILD.set(isMaster);

            // Allow sub-classes to define defaults prior options are injected
            preConfigure();

            // Inject options
            JkOptions.populateFields(this, JkOptions.loadSystemAndUserOptions());
            Map<String, String> options = isMaster ? CommandLine.INSTANCE.getMasterBuildOptions() :
                    CommandLine.INSTANCE.getSubProjectBuildOptions();
            JkOptions.populateFields(this, options);

            // Load plugins declared in command line
            plugins.loadCommandLinePlugins(isMaster);


            this.infoProvider.append("base directory : " + this.baseDir + "\n"
                    + "imported builds : " + this.importedBuilds.directs() + "\n");
        } catch (RuntimeException e) {
            BASE_DIR_CONTEXT.remove();
            throw e;
        } finally {
            if (isMaster == true) {
                BASE_DIR_CONTEXT.remove();
            }
        }
    }



    // ------------------ options --------------------------------------------------------


    @JkDoc("Help options")
    private final JkHelpOptions help = new JkHelpOptions();

    @JkDoc("Embed Jerkar jar along bin script in the project while scaffolding so the project can be run without Jerkar installed.")
    boolean scaffoldEmbed;

    // --------------------------- constructs ----------------------------------



    /**
     * This method is invoked before options are injected into this instance.
     * The values set here are likely to be overrode by command line arguments or external configuration.
     */
    protected void preConfigure() {
        // Do nothing by default
    }

    /**
     * This method is invoked right after options has been injected into this instance.
     * 
     */
    protected void postConfigure() {
        // Do nothing by default
    }

    // -------------------------------- accessor functions ---------------------------------------


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

    protected JkBuildPlugins plugins() {
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


    // ------------------------------ build dependencies ---------------------------------------------

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

    // ------------------------------ Command line methods ---------------------------------------------------

    /**
     * Creates the project structure (mainly project folder layout, build class code and IDE metadata) at the asScopedDependency
     * of the current project.
     */
    @JkDoc("Creates the project structure")
    public final void scaffold() {
        scaffolder().run();
    }

    /** Clean the output directory. */
    @JkDoc("Cleans the output directory.")
    public void clean() {
        JkLog.start("Cleaning output directory " + outputDir());
        if (Files.exists(outputDir())) {
            JkPathTree.of(outputDir()).refuse(JkConstants.BUILD_DEF_BIN_DIR_NAME + "/**").deleteContent();
        }
        JkLog.done();
    }

    /** Conventional method standing for the default operations to perform.
     * @throws Exception */
    @JkDoc("Conventional method standing for the default operations to perform.")
    public void doDefault() {
        defaulter.run();
    }

    /** Displays all available methods defined in this build. */
    @JkDoc("Displays all available methods defined in this build.")
    public void help() {
        if (help.xml || help.xmlFile != null) {
            HelpDisplayer.help(this, help.xmlFile);
        } else {
            HelpDisplayer.help(this);
            HelpDisplayer.helpPlugins();
        }
    }

    /** Displays meaningful information about this build. */
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
