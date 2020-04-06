package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.java.project.*;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.repos.JkPluginPgp;
import dev.jeka.core.tool.builtins.repos.JkPluginRepo;
import dev.jeka.core.tool.builtins.scaffold.JkPluginScaffold;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Plugin for building Java projects. It comes with a {@link JkJavaProject} pre-configured with {@link JkOptions}.
 * and a decoration for scaffolding.
 */
@JkDoc("Build of a Java project through a JkJavaProject instance.")
@JkDocPluginDeps({JkPluginRepo.class, JkPluginScaffold.class})
public class JkPluginJava extends JkPlugin implements JkJavaIdeSupport.JkSupplier {

    // ------------  Options injectable by command line --------------------------------

    /**
     * Options for the publish tasks. These options are injectable from command line.
     */
    public final JkPublishOptions publish = new JkPublishOptions();

    /**
     * Options for the packaging tasks (jar creation). These options are injectable from command line.
     */
    public final JkJavaPackOptions pack = new JkJavaPackOptions();

    /**
     * Options for the testing tasks. These options are injectable from command line.
     */
    public final JkTestOptions tests = new JkTestOptions();

    @JkDoc("Extra arguments to be passed to the compiler (e.g. -Xlint:unchecked).")
    public String compilerExtraArgs;

    // ----------------------------------------------------------------------------------

    private final JkPluginRepo repoPlugin;

    private final JkPluginScaffold scaffoldPlugin;

    private JkJavaProject project;

    protected JkPluginJava(JkCommandSet run) {
        super(run);
        this.scaffoldPlugin = run.getPlugins().get(JkPluginScaffold.class);
        this.repoPlugin = run.getPlugins().get(JkPluginRepo.class);

        // Pre-configure JkJavaProject instance
        this.project = JkJavaProject.of().setBaseDir(this.getCommandSet().getBaseDir());
        this.project.getDependencyManagement().addDependencies(JkDependencySet.ofLocal(run.getBaseDir().resolve(JkConstants.JEKA_DIR + "/libs")));
        final Path path = run.getBaseDir().resolve(JkConstants.JEKA_DIR + "/libs/dependencies.txt");
        if (Files.exists(path)) {
            this.project.getDependencyManagement().addDependencies(JkDependencySet.ofTextDescription(path));
        }
    }

    @JkDoc("Improves scaffolding by creating a project structure ready to build.")
    @Override  
    protected void activate() {
        this.applyOptionsToUnderlyingProject();
        this.setupScaffolder();
    }

    private void applyOptionsToUnderlyingProject() {
        JkVersionedModule versionedModule = project.getPublication().getVersionedModule();
        if (versionedModule != null) {
            project.getPackaging().getManifest()
                    .addMainAttribute(JkManifest.IMPLEMENTATION_TITLE, versionedModule.getModuleId().getName())
                    .addMainAttribute(JkManifest.IMPLEMENTATION_VENDOR, versionedModule.getModuleId().getGroup())
                    .addMainAttribute(JkManifest.IMPLEMENTATION_VERSION, versionedModule.getVersion().getValue());
        }
        final JkArtifactBasicProducer artifactProducer = project.getArtifactProducer();
        if (!pack.sources) {
            artifactProducer.removeArtifact(JkJavaProject.SOURCES_ARTIFACT_ID);
        }
        if (!pack.javadoc) {
            artifactProducer.removeArtifact(JkJavaProject.JAVADOC_ARTIFACT_ID);
        }
        if (project.getCompilation().getCompiler().isDefault()) {  // If no compiler specified, try to set the best fitted
            project.getCompilation().getCompiler().setForkingProcess(compilerProcess());
        }
        if (project.getPublication().getPublishRepos() == null
                || project.getPublication().getPublishRepos().getRepoList().isEmpty()) {
            project.getPublication().addPublishRepo(repoPlugin.publishRepository());
        }
        final JkRepo downloadRepo = repoPlugin.downloadRepository();
        project.getDependencyManagement().getResolver().addRepos(downloadRepo);
        if (pack.checksums().length > 0) {
            project.getPackaging().setChecksumAlgorithms(pack.checksums());
        }
        JkPluginPgp pgpPlugin = this.getCommandSet().getPlugins().get(JkPluginPgp.class);
        JkGpg pgp = pgpPlugin.get();
        project.getPublication().setSigner(pgp.getSigner(pgpPlugin.keyName));

        JkTestProcessor testProcessor = project.getTesting().getTestProcessor();
        if (tests.fork) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(this.tests.jvmOptions);
            testProcessor.setForkingProcess(javaProcess);
        }
        project.getTesting().setSkipped(tests.skip);
        if (this.compilerExtraArgs != null) {
            project.getCompilation().addOptions(JkUtilsString.translateCommandline(this.compilerExtraArgs));
        }
    }

    private void setupScaffolder() {
        String template = JkUtilsIO.read(JkPluginJava.class.getResource("buildclass.snippet"));
        String baseDirName = getCommandSet().getBaseDir().getFileName().toString();
        String code = template.replace("${group}", baseDirName).replace("${name}", baseDirName);
        JkLog.info("Create source directories.");
        JkCompileLayout prodLayout = project.getCompilation().getLayout();
        prodLayout.resolveSources().toList().stream().forEach(tree -> tree.createIfNotExist());
        prodLayout.resolveResources().toList().stream().forEach(tree -> tree.createIfNotExist());
        JkCompileLayout testLayout = project.getTesting().getCompilation().getLayout();
        testLayout.resolveSources().toList().stream().forEach(tree -> tree.createIfNotExist());
        testLayout.resolveResources().toList().stream().forEach(tree -> tree.createIfNotExist());
        scaffoldPlugin.getScaffolder().setCommandClassCode(code);
        scaffoldPlugin.getScaffolder().setClassFilename("Build.java");
    }

    // ------------------------------ Accessors -----------------------------------------

    public JkJavaProject getProject() {
        return project;
    }

    public void setProject(JkJavaProject javaProject) {
        this.project = javaProject;
    }

    public JkPluginRepo getRepoPlugin() {
        return repoPlugin;
    }

    public  JkPluginScaffold getScaffoldPlugin() {
        return scaffoldPlugin;
    }

    // ------------------------------- command line methods -----------------------------

    @JkDoc("Performs compilation and resource processing.")
    public void compile() {
        project.getCompilation().run();
    }

    @JkDoc("Compiles and run tests defined within the project (typically Junit tests).")
    public void test() {
        project.getTesting().run();
    }

    @JkDoc("Generates from scratch artifacts defined through 'pack' options (Perform compilation and testing if needed).  " +
            "\nDoes not re-generate artifacts already generated : " +
            "execute 'clean java#pack' to re-generate artifacts.")
    public void pack() {
        project.getArtifactProducer().makeAllMissingArtifacts();
    }

    /**
     * Displays the resolved dependency tree on the console.
     */
    @JkDoc("Displays resolved dependency tree on console.")
    public final void showDependencies() {
        JkLog.info("Declared dependencies : ");
        project.getDependencyManagement().getDependencies().toResolvedModuleVersions().toList()
                .forEach(dep -> JkLog.info(dep.toString()));
        JkLog.info("Resolved to : ");
        final JkResolveResult resolveResult = this.getProject().getDependencyManagement().fetchDependencies();
        final JkDependencyNode tree = resolveResult.getDependencyTree();
        JkLog.info(String.join("\n", tree.toStrings()));
    }

    @JkDoc("Displays information about the Java project to build.")
    public void info() {
        JkLog.info(this.project.getInfo());
        JkLog.info("\nExecute 'java#showDependencies' to display details on dependencies.");

    }

    @JkDoc("Publishes produced artifacts to configured repository.")
    public void publish() {
        project.getPublication().publish();
    }

    @JkDoc("Publishes produced artifacts to local repository.")
    public void publishLocal() {
        project.getPublication().publishLocal();
    }

    @JkDoc("Fetches project dependencies in cache.")
    public void refreshDeps() {
        project.getDependencyManagement().fetchDependencies();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return project.getJavaIdeSupport();
    }

    public static class JkPublishOptions {

        @JkDoc("If true, publishing will occur only in the local repository.")
        public boolean localOnly = false;

    }

    private JkProcess compilerProcess() {
        final Map<String, String> jdkOptions = JkOptions.getAllStartingWith("jdk.");
        JkJavaProjectCompilation compilation = project.getCompilation();
        return JkJavaCompiler.getForkedProcessOnJavaSourceVersion(jdkOptions,
                compilation.getJavaVersion().get());
    }

}
