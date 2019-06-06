package org.jerkar.tool.builtins.java;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkJavaProjectMaker;
import org.jerkar.api.java.project.JkJavaProjectTestTasks;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.*;
import org.jerkar.tool.builtins.repos.JkPluginPgp;
import org.jerkar.tool.builtins.repos.JkPluginRepo;
import org.jerkar.tool.builtins.scaffold.JkPluginScaffold;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Plugin for building Java projects. It comes with a {@link JkJavaProject} pre-configured with {@link JkOptions}.
 * and a decoration for scaffolding.
 */
@JkDoc("Build of a Java project through a JkJavaProject instance.")
@JkDocPluginDeps({JkPluginRepo.class, JkPluginScaffold.class})
public class JkPluginJava extends JkPlugin {

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

    protected JkPluginJava(JkRun run) {
        super(run);
        this.scaffoldPlugin = run.getPlugins().get(JkPluginScaffold.class);
        this.repoPlugin = run.getPlugins().get(JkPluginRepo.class);

        // Pre-configure JkJavaProject instance
        this.project = JkJavaProject.ofMavenLayout(this.getRun().getBaseDir());
        this.project.setDependencies(JkDependencySet.ofLocal(run.getBaseDir().resolve(JkConstants.JERKAR_DIR + "/libs")));
        final Path path = run.getBaseDir().resolve(JkConstants.JERKAR_DIR + "/libs/dependencies.txt");
        if (Files.exists(path)) {
            this.project.addDependencies(JkDependencySet.ofTextDescription(path));
        }
        this.project.getMaker().getOutputCleaner().set(() -> run.clean());
    }

    @JkDoc("Improves scaffolding by creating a project structure ready to build.")
    @Override  
    protected void activate() {
        this.applyOptionsToUnderlyingProject();
        this.setupScaffolder();
    }

    private void applyOptionsToUnderlyingProject() {
        final JkJavaProjectMaker maker = project.getMaker();
        if (!pack.sources) {
            maker.removeArtifact(JkJavaProjectMaker.SOURCES_ARTIFACT_ID);
        }
        if (pack.javadoc) {
            maker.addJavadocArtifact();
        }
        if (pack.tests) {
            maker.addTestArtifact();
        }
        if (pack.testSources) {
            maker.addTestSourceArtifact();
        }
        if (maker.getTasksForCompilation().getCompiler().isDefault()) {  // If no compiler specified, try to set the best fitted
            maker.getTasksForCompilation().setCompiler(compiler());
        }
        maker.getTasksForPublishing().setPublishRepos(repoPlugin.publishRepositories());
        final JkRepoSet downloadRepos = repoPlugin.downloadRepositories();
        JkDependencyResolver resolver = project.getMaker().getDependencyResolver();
        resolver = resolver.withRepos(downloadRepos); // always look in local repo
        maker.setDependencyResolver(resolver);
        if (pack.checksums().length > 0) {
            maker.getTasksForPackaging().setChecksumAlgorithms(pack.checksums());
        }
        if (publish.signArtifacts) {
            JkPluginPgp pgpPlugin = this.getRun().getPlugins().get(JkPluginPgp.class);
            JkPgp pgp = pgpPlugin.get();
            maker.getTasksForPublishing().setSigner(pgp.getSigner(pgpPlugin.keyName));
        }
        JkUnit tester = maker.getTasksForTesting().getRunner();
        if (tests.fork) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(this.tests.jvmOptions);
            tester = tester.withForking(javaProcess);
        }
        if (tests.runIT) {
            maker.getTasksForTesting().setTestClassMatcher(maker.getTasksForTesting().getTestClassMatcher()
                    .and(true, JkJavaProjectTestTasks.IT_CLASS_PATTERN));
        }
        tester = tester.withOutputOnConsole(tests.output);
        tester = tester.withReport(tests.report);
        maker.getTasksForTesting().setRunner(tester);
        maker.getTasksForTesting().setSkipTests(tests.skip);
        if (this.compilerExtraArgs != null) {
            project.getCompileSpec().addOptions(JkUtilsString.translateCommandline(this.compilerExtraArgs));
        }
    }

    private void setupScaffolder() {
        String template = JkUtilsIO.read(JkPluginJava.class.getResource("buildclass.snippet"));
        String baseDirName = getRun().getBaseDir().getFileName().toString();
        String code = template.replace("${group}", baseDirName).replace("${name}", baseDirName);
        JkLog.info("Create source directories.");
        project.getSourceLayout().getSources().getPathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().getResources().getPathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().getTests().getPathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().getTestResources().getPathTrees().stream().forEach(tree -> tree.createIfNotExist());
        scaffoldPlugin.setRunClassClode(code);
    }

    //  ----------------------------- Shorthands ---------------------------------------

    /**
     * Cleans the output directory for the project
     */
    public JkPluginJava clean() {
        this.project.getMaker().clean();
        return this;
    }

    // ------------------------------ Accessors -----------------------------------------

    public JkJavaProject getProject() {
        return project;
    }

    public void setProject(JkJavaProject javaProject) {
        this.project = javaProject;
    }

    public JkPathTree ouputTree() {
        return JkPathTree.of(this.getProject().getMaker().getOutLayout().getOutputPath());
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
        project.getMaker().getTasksForCompilation().run();
    }

    @JkDoc("Compiles and run tests defined within the project (typically Junit tests).")
    public void test() {
        project.getMaker().getTasksForTesting().run();
    }

    @JkDoc("Generates from scratch artifacts defined through 'pack' options (Perform compilation and testing if needed).  " +
            "\nDoes not re-generate artifacts already generated : " +
            "execute 'clean java#pack' to re-generate artifacts.")
    public void pack() {
        project.getMaker().makeAllMissingArtifacts();
    }

    /**
     * Displays the resolved dependency tree on the console.
     */
    @JkDoc("Displays resolved dependency tree on console.")
    public final void showDependencies() {
        JkLog.info("Declared dependencies : ");
        project.getDependencies().toResolvedModuleVersions().toList().forEach(dep -> JkLog.info(dep.toString()));
        JkLog.info("Resolved to : ");
        final JkResolveResult resolveResult = this.getProject().getMaker().getDependencyResolver()
                .resolve(this.project.getDependencies().withDefaultScopes(JkJavaDepScopes.COMPILE_AND_RUNTIME));
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
        project.getMaker().getTasksForPublishing().publish();
    }

    @JkDoc("Publishes produced artifacts to local repository.")
    public void publishLocal() {
        project.getMaker().getTasksForPublishing().publishLocal();
    }

    @JkDoc("Fetches project dependencies in cache.")
    public void refreshDeps() {
        project.getMaker().getDependencyResolver().resolve(project.getMaker().getScopeDefaultedDependencies());
    }

    public static class JkPublishOptions {

        @JkDoc("If true, publishing will occur only in the local repository.")
        public boolean localOnly = false;

        @JkDoc("If true, all artifacts to be published will be signed with PGP.")
        public boolean signArtifacts = false;

    }

    private JkJavaCompiler compiler() {
        final Map<String, String> jdkOptions = JkOptions.getAllStartingWith("jdk.");
        final JkProcess process =  JkJavaCompiler.getForkedProcessOnJavaSourceVersion(jdkOptions,
                getProject().getCompileSpec().getSourceVersion().get());
        if (process != null) {
            return getProject().getMaker().getTasksForCompilation().getCompiler().withForking(process);
        }
        return project.getMaker().getTasksForCompilation().getCompiler();
    }

}
