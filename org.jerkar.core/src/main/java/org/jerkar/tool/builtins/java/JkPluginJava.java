package org.jerkar.tool.builtins.java;

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
import org.jerkar.tool.builtins.repos.JkPluginRepo;
import org.jerkar.tool.builtins.scaffold.JkPluginScaffold;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    private final List<JkArtifactId> producedArtifacts = new ArrayList<>();

    protected JkPluginJava(JkRun run) {
        super(run);
        this.repoPlugin = run.plugins().get(JkPluginRepo.class);
        this.project = JkJavaProject.ofMavenLayout(this.owner.baseDir());
        this.project.setDependencies(JkDependencySet.ofLocal(project().getSourceLayout()
                .baseDir().resolve(JkConstants.JERKAR_DIR + "/libs")));
        final Path path = this.project.getSourceLayout().baseDir().resolve(JkConstants.DEF_DIR + "/dependencies.txt");
        if (Files.exists(path)) {
            this.project.setDependencies(this.project.getDependencies().and(JkDependencySet.fromDescription(path)));
        }
        this.producedArtifacts.add(this.project.maker().mainArtifactId());
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
        if (!publish.sources) {
            project.maker().getArtifactFileIdsToNotPublish().addAll(
                    project.maker().artifactIdsWithClassifier("sources"));
        }
        if (!publish.tests) {
            project.maker().getArtifactFileIdsToNotPublish().addAll(
                    project.maker().artifactIdsWithClassifier("test"));
        }
        project.maker().setCompiler(compiler());
        project.maker().setPublishRepos(JkRepoSet.of(repoPlugin.publishRepository()));
        if (publish.localOnly) {
            project.maker().setPublishRepos(JkRepoSet.local());
        }
        final JkRepo downloadRepo = repoPlugin.downloadRepository();
        JkDependencyResolver resolver = project.maker().getDependencyResolver();
        resolver = resolver.withRepos(downloadRepo); // always look in local repo
        project.maker().setDependencyResolver(resolver);
        if (pack.javadoc) {
            producedArtifacts.add(JkJavaProjectMaker.JAVADOC_ARTIFACT_ID);
        }
        if (pack.sources) {
            producedArtifacts.add(JkJavaProjectMaker.SOURCES_ARTIFACT_ID);
        }
        if (pack.tests) {
            producedArtifacts.add(JkJavaProjectMaker.TEST_ARTIFACT_ID);
        }
        if (pack.checksums().length > 0) {
            project.maker().postPack.chain(() -> project.maker().checksum(pack.checksums()));
        }
        /*
        if (publish.signArtifacts) {
            project.maker().postPack.chain(() -> project.maker().signArtifactFiles(repoPlugin.pgpSigner()));
        }
        */
        JkUnit tester = (JkUnit) project.maker().getTestRunner();
        if (tests.fork) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(this.tests.jvmOptions);
            tester = tester.forked(javaProcess);
        }
        tester = tester.withOutputOnConsole(tests.output);
        tester = tester.withReport(tests.report);
        project.maker().setTestRunner(tester);
        project.maker().setSkipTests(tests.skip);
        if (this.compilerExtraArgs != null) {
            project.getCompileSpec().addOptions(JkUtilsString.translateCommandline(this.compilerExtraArgs));
        }
    }

    private void setupScaffolder() {
        String template = JkUtilsIO.read(JkPluginJava.class.getResource("buildclass.snippet"));
        String baseDirName = owner.baseDir().getFileName().toString();
        String code = template.replace("${group}", baseDirName).replace("${name}", baseDirName);
        JkLog.info("Create source directories.");
        project.getSourceLayout().sources().pathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().resources().pathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().tests().pathTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().testResources().pathTrees().stream().forEach(tree -> tree.createIfNotExist());
        scaffoldPlugin.setRunClassClode(code);
    }

    // ------------------------------ Accessors -----------------------------------------

    public JkJavaProject project() {
        return project;
    }

    public void setProject(JkJavaProject javaProject) {
        this.project = javaProject;
    }

    public List<JkArtifactId> producedArtifacts() {
        return producedArtifacts;
    }

    public JkPathTree ouputTree() {
        return JkPathTree.of(this.project().maker().getOutLayout().outputPath());
    }

    public void addArtifactToProduce(JkArtifactId artifactId) {
        this.producedArtifacts.add(artifactId);
    }

    public void removeArtifactToProduce(JkArtifactId artifactId) {
        this.producedArtifacts.remove(artifactId);
    }

    // ------------------------------- command line methods -----------------------------

    @JkDoc("Performs compilation and resource processing.")
    public void compile() {
        project.maker().compile();
    }

    @JkDoc("Compiles and run tests defined within the project (typically Junit tests).")
    public void test() {
        project.maker().test();
    }

    @JkDoc("Generates from scratch artifacts defined through 'pack' options (Perform compilation and testing if needed).  " +
            "\nDoes not re-generate artifacts already generated : " +
            "execute 'clean java#pack' to re-generate artifacts.")
    public void pack() {
        project.maker().pack(this.producedArtifacts);
    }

    /**
     * Displays the resolved dependency tree on the console.
     */
    @JkDoc("Displays resolved dependency tree on console.")
    public final void showDependencies() {
        final JkResolveResult resolveResult = this.project().maker().getDependencyResolver()
                .resolve(this.project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME));
        final JkDependencyNode tree = resolveResult.dependencyTree();
        JkLog.info(String.join("\n", tree.toStrings()));
    }

    @JkDoc("Displays information about the Java project to build.")
    public void info() {
        JkLog.info(this.project.info());
        JkLog.info("Produced Artifacts : " + this.producedArtifacts);
        JkLog.info("\nExecute 'java#showDependencies' to display details on dependencies.");

    }

    @JkDoc("Publishes produced artifacts to configured repository.")
    public void publish() {
        project.maker().publish();
    }

    public static class JkPublishOptions {

        @JkDoc("If true, publishing to repository will include sources jar.")
        public boolean sources = true;

        @JkDoc("If true, publishing to repository will include tests jar.")
        public boolean tests = false;

        @JkDoc("If true, publishing will occur only in the local repository.")
        public boolean localOnly = false;

        @JkDoc("If true, all artifacts to be published will be signed with PGP.")
        public boolean signArtifacts = false;

    }

    private JkJavaCompiler compiler() {
        final Map<String, String> jdkOptions = JkOptions.getAllStartingWith("jdk.");
        final JkProcess process =  JkJavaCompiler.getForkedProcessIfNeeded(jdkOptions,
                project().getCompileSpec().getSourceVersion().name());
        if (process != null) {
            return project().maker().getCompiler().fork(process);
        }
        return project.maker().getCompiler();
    }

}
