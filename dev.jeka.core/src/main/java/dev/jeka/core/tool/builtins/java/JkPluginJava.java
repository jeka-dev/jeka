package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.project.*;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.repos.JkPluginGpg;
import dev.jeka.core.tool.builtins.repos.JkPluginRepo;
import dev.jeka.core.tool.builtins.scaffold.JkPluginScaffold;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Plugin for building Java projects. It comes with a {@link JkJavaProject} pre-configured with {@link JkOptions}.
 * and a decoration for scaffolding.
 */
@JkDoc("Build of a Java project through a JkJavaProject instance.")
@JkDocPluginDeps({JkPluginRepo.class, JkPluginScaffold.class})
public class JkPluginJava extends JkPlugin implements JkJavaIdeSupport.JkSupplier {

    /**
     * Options for the packaging tasks (jar creation). These options are injectable from command line.
     */
    public final JkJavaPackOptions pack = new JkJavaPackOptions();

    /**
     * Options for the testing tasks. These options are injectable from command line.
     */
    public final JkTestOptions test = new JkTestOptions();

    @JkDoc("Extra arguments to be passed to the compiler (e.g. -Xlint:unchecked).")
    public String compilerExtraArgs;

    @JkDoc("Scaffolded code won't use the simple facade over JkJavaProject")
    public boolean noFacade;

    // ----------------------------------------------------------------------------------

    private final JkPluginRepo repoPlugin;

    private final JkPluginScaffold scaffoldPlugin;

    private JkJavaProject project;

    protected JkPluginJava(JkClass jkClass) {
        super(jkClass);
        this.scaffoldPlugin = jkClass.getPlugins().get(JkPluginScaffold.class);
        this.repoPlugin = jkClass.getPlugins().get(JkPluginRepo.class);

        // Pre-configure JkJavaProject instance
        this.project = JkJavaProject.of().setBaseDir(this.getJkClass().getBaseDir());
        this.project.getConstruction().getDependencyResolver().addDependencies(
                JkDependencySet.ofLocal(jkClass.getBaseDir().resolve(JkConstants.JEKA_DIR + "/libs")));
        final Path path = jkClass.getBaseDir().resolve(JkConstants.JEKA_DIR + "/libs/dependencies.txt");
        if (Files.exists(path)) {
            this.project.getConstruction().getDependencyResolver();
        }
    }

    @Override
    protected void beforeSetup() {
        setupDefaultProject();
    }

    @JkDoc("Improves scaffolding by creating a project structure ready to build.")
    @Override  
    protected void afterSetup() {
        this.applyPostSetupOptions();
        this.setupScaffolder();
    }

    private void setupDefaultProject() {
        JkJavaProjectConstruction construction = project.getConstruction();
        JkJavaCompiler compiler = construction.getCompilation().getCompiler();
        if (compiler.isDefault()) {  // If no compiler specified, try to set the best fitted
            compiler.setForkingProcess(compilerProcess());
        }
        if (project.getPublication().getPublishRepos() == null
                || project.getPublication().getPublishRepos().getRepoList().isEmpty()) {
            project.getPublication().addRepos(repoPlugin.publishRepository());
        }
        final JkRepo downloadRepo = repoPlugin.downloadRepository();
        JkDependencyResolver resolver = construction.getDependencyResolver().getResolver();
        if (!resolver.getRepos().contains(downloadRepo.getUrl())) {
            resolver.addRepos(downloadRepo);
        }
        JkPluginGpg pgpPlugin = this.getJkClass().getPlugins().get(JkPluginGpg.class);

        // Use signer from GPG plugin as default
        JkGpg gpg = pgpPlugin.get();
        UnaryOperator<Path> signer  = gpg.getSigner(pgpPlugin.keyName);
        project.getPublication().setSigner(signer);
    }

    private void applyPostSetupOptions() {
        final JkStandardFileArtifactProducer artifactProducer = project.getPublication().getArtifactProducer();
        JkArtifactId sources = JkJavaProjectPublication.SOURCES_ARTIFACT_ID;
        if (pack.sources != null && !pack.sources) {
            artifactProducer.removeArtifact(sources);
        } else if (pack.sources != null && pack.sources && !artifactProducer.getArtifactIds().contains(sources)) {
            Consumer<Path> sourceJar = project.getDocumentation()::createSourceJar;
            artifactProducer.putArtifact(sources, sourceJar);
        }
        JkArtifactId javadoc = JkJavaProjectPublication.JAVADOC_ARTIFACT_ID;
        if (pack.javadoc != null && !pack.javadoc) {
            artifactProducer.removeArtifact(javadoc);
        } else if (pack.javadoc != null && pack.javadoc && !artifactProducer.getArtifactIds().contains(javadoc)) {
            Consumer<Path> javadocJar = project.getDocumentation()::createJavadocJar;
            artifactProducer.putArtifact(javadoc, javadocJar);
        }
        JkTestProcessor testProcessor = project.getConstruction().getTesting().getTestProcessor();
        if (test.fork != null && test.fork && testProcessor.getForkingProcess() == null) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(this.test.jvmOptions);
            testProcessor.setForkingProcess(javaProcess);
        } else if (test.fork != null && !test.fork && testProcessor.getForkingProcess() != null) {
            testProcessor.setForkingProcess(false);
        }
        if (test.skip != null) {
            project.getConstruction().getTesting().setSkipped(test.skip);
        }
        if (this.compilerExtraArgs != null) {
            project.getConstruction().getCompilation().addOptions(JkUtilsString.translateCommandline(this.compilerExtraArgs));
        }
    }

    private void setupScaffolder() {
        String snippet = noFacade ? "buildclass.snippet" : "buildclassfacade.snippet";
        String template = JkUtilsIO.read(JkPluginJava.class.getResource(snippet));
        String baseDirName = getJkClass().getBaseDir().getFileName().toString();
        String code = template.replace("${group}", baseDirName).replace("${name}", baseDirName);
        JkLog.info("Create source directories.");
        JkCompileLayout prodLayout = project.getConstruction().getCompilation().getLayout();
        prodLayout.resolveSources().toList().stream().forEach(tree -> tree.createIfNotExist());
        prodLayout.resolveResources().toList().stream().forEach(tree -> tree.createIfNotExist());
        JkCompileLayout testLayout = project.getConstruction().getTesting().getCompilation().getLayout();
        testLayout.resolveSources().toList().stream().forEach(tree -> tree.createIfNotExist());
        testLayout.resolveResources().toList().stream().forEach(tree -> tree.createIfNotExist());
        scaffoldPlugin.getScaffolder().setJekaClassCode(code);
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
        project.getConstruction().getCompilation().run();
    }

    @JkDoc("Compiles and run tests defined within the project (typically Junit tests).")
    public void test() {
        project.getConstruction().getTesting().run();
    }

    @JkDoc("Generates from scratch artifacts defined through 'pack' options (Perform compilation and testing if needed).  " +
            "\nDoes not re-generate artifacts already generated : " +
            "execute 'clean java#pack' to re-generate artifacts.")
    public void pack() {
        project.getPublication().getArtifactProducer().makeAllMissingArtifacts();
    }

    /**
     * Displays the resolved dependency tree on the console.
     */
    @JkDoc("Displays resolved dependency tree on console.")
    public final void showDependencies() {
        JkLog.info("Declared dependencies : ");
        project.getConstruction().getDependencyResolver().getDependencies().toResolvedModuleVersions().toList()
                .forEach(dep -> JkLog.info(dep.toString()));
        JkLog.info("Resolved to : ");
        final JkResolveResult resolveResult = this.getProject().getConstruction().getDependencyResolver().resolveDependencies();
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
        project.getConstruction().getDependencyResolver().resolveDependencies();
    }

    @Override
    public JkJavaIdeSupport getJavaIdeSupport() {
        return project.getJavaIdeSupport();
    }

    private JkProcess compilerProcess() {
        final Map<String, String> jdkOptions = JkOptions.getAllStartingWith("jdk.");
        JkJavaProjectCompilation compilation = project.getConstruction().getCompilation();
        return JkJavaCompiler.getForkedProcessOnJavaSourceVersion(jdkOptions,
                compilation.getJavaVersion().get());
    }

    /**
     * Standard options for packaging java projects.
     */
    public static class JkJavaPackOptions {

        /** When true, javadoc is created and packed in a jar file.*/
        @JkDoc("If true, javadoc jar is added in the list of artifact to produce/publish.")
        public Boolean javadoc;

        /** When true, sources are packed in a jar file.*/
        @JkDoc("If true, sources jar is added in the list of artifact to produce/publish.")
        public Boolean sources;

    }

    /**
     * Options about tests
     */
    public static final class JkTestOptions {

        /** Turn it on to skip tests. */
        @JkDoc("If true, tests are not run.")
        public Boolean skip;

        /** Turn it on to run tests in a withForking process. */
        @JkDoc("If true, tests will be executed in a withForking process.")
        public Boolean fork;

        /** Argument passed to the JVM if tests are withForking. Example : -Xms2G -Xmx2G */
        @JkDoc("Argument passed to the JVM if tests are withForking. E.g. -Xms2G -Xmx2G.")
        public String jvmOptions;

    }
}
