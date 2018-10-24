package org.jerkar.tool.builtins.java;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkJavaProjectMaker;
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

    // ------------------------------ options -------------------------------------------

    @JkDoc("Version for the project to build.")
    public String projectVersion;

    public final JkPublishOptions publish = new JkPublishOptions();

    public final JkJavaPackOptions pack = new JkJavaPackOptions();

    public final JkTestOptions tests = new JkTestOptions();

    @JkDoc("Extra arguments to be passed to the compiler (e.g. -Xlint:unchecked).")
    public String compilerExtraArgs;

    // ----------------------------------------------------------------------------------

    private final JkPluginRepo repoPlugin;

    private final JkPluginScaffold scaffoldPlugin;

    private JkJavaProject project;

    protected JkPluginJava(JkRun run) {
        super(run);
        this.repoPlugin = run.plugins().get(JkPluginRepo.class);
        this.project = JkJavaProject.ofMavenLayout(this.getOwner().baseDir());
        this.project.setDependencies(JkDependencySet.ofLocal(project().getSourceLayout()
                .getBaseDir().resolve(JkConstants.JERKAR_DIR + "/libs")));
        final Path path = this.project.getSourceLayout().getBaseDir().resolve(JkConstants.DEF_DIR + "/dependencies.txt");
        if (Files.exists(path)) {
            this.project.setDependencies(this.project.getDependencies().and(JkDependencySet.ofTextDescription(path)));
        }
        this.scaffoldPlugin = run.plugins().get(JkPluginScaffold.class);
    }

    @JkDoc("Improves scaffolding by creating a project structure ready to build.")
    @Override  
    protected void activate() {
        this.applyOptionsToUnderlyingProject();
        this.setupScaffolder();
    }

    private void applyOptionsToUnderlyingProject() {
        if (project.getVersionedModule() != null && !JkUtilsString.isBlank(projectVersion)) {
            project.setVersionedModule(project.getVersionedModule().withVersion(projectVersion));
        }
        final JkJavaProjectMaker maker = project.getMaker();
        if (!pack.sources) {
            project.getMaker().undefineArtifact(JkJavaProjectMaker.SOURCES_ARTIFACT_ID);
        }
        if (pack.javadoc) {
            project.getMaker().defineJavadocArtifact();
        }
        if (pack.tests) {
            project.getMaker().defineTestArtifact();
        }
        if (pack.testSources) {
            project.getMaker().defineTestSourceArtifact();
        }
        if (maker.getCompileTasks().getCompiler().isDefault()) {  // If no compiler specified, try to set the best fitted
            maker.getCompileTasks().setCompiler(compiler());
        }
        maker.getPublishTasks().setPublishRepos(JkRepoSet.of(repoPlugin.publishRepository()));
        final JkRepo downloadRepo = repoPlugin.downloadRepository();
        JkDependencyResolver resolver = project.getMaker().getDependencyResolver();
        resolver = resolver.withRepos(downloadRepo); // always look in local repo
        project.getMaker().setDependencyResolver(resolver);
        if (pack.checksums().length > 0) {
            project.getMaker().getPackTasks().setChecksumAlgorithms(pack.checksums());
        }
        if (publish.signArtifacts) {
            JkPluginPgp pgpPlugin = this.getOwner().plugins().get(JkPluginPgp.class);
            JkPgp pgp = pgpPlugin.get();
            project.getMaker().getPublishTasks().setSigner(pgp::sign);
        }
        JkUnit tester = project.getMaker().getTestTasks().getRunner();
        if (tests.fork) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(this.tests.jvmOptions);
            tester = tester.withForking(javaProcess);
        }
        tester = tester.withOutputOnConsole(tests.output);
        tester = tester.withReport(tests.report);
        maker.getTestTasks().setRunner(tester);
        maker.setSkipTests(tests.skip);
        if (this.compilerExtraArgs != null) {
            project.getCompileSpec().addOptions(JkUtilsString.translateCommandline(this.compilerExtraArgs));
        }
    }

    private void setupScaffolder() {
        String template = JkUtilsIO.read(JkPluginJava.class.getResource("buildclass.snippet"));
        String baseDirName = getOwner().baseDir().getFileName().toString();
        String code = template.replace("${group}", baseDirName).replace("${name}", baseDirName);
        JkLog.info("Create source directories.");
        project.getSourceLayout().getSources().getPathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().getResources().getPathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().getTests().getPathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().getTestResources().getPathTrees().stream().forEach(tree -> tree.createIfNotExist());
        scaffoldPlugin.setRunClassClode(code);
    }

    // ------------------------------ Accessors -----------------------------------------

    public JkJavaProject project() {
        return project;
    }

    public void setProject(JkJavaProject javaProject) {
        this.project = javaProject;
    }

    public JkPathTree ouputTree() {
        return JkPathTree.of(this.project().getMaker().getOutLayout().getOutputPath());
    }

    // ------------------------------- command line methods -----------------------------

    @JkDoc("Performs compilation and resource processing.")
    public void compile() {
        project.getMaker().compile();
    }

    @JkDoc("Compiles and run tests defined within the project (typically Junit tests).")
    public void test() {
        project.getMaker().test();
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
        final JkResolveResult resolveResult = this.project().getMaker().getDependencyResolver()
                .resolve(this.project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME));
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
        project.getMaker().getPublishTasks().publish();
    }

    @JkDoc("Publishes produced artifacts to local repository.")
    public void publishLocal() {
        project.getMaker().getPublishTasks().publishLocal();
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
                project().getCompileSpec().getSourceVersion().get());
        if (process != null) {
            return project().getMaker().getCompileTasks().getCompiler().withForking(process);
        }
        return project.getMaker().getCompileTasks().getCompiler();
    }

}
