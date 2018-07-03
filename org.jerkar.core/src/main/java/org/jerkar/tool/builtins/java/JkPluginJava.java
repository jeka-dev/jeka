package org.jerkar.tool.builtins.java;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaProjectMaker;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.*;
import org.jerkar.tool.builtins.repos.JkPluginRepos;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin for building Java projects. It comes with a {@link JkJavaProject} pre-configured with {@link JkOptions}.
 * and a decoration for scaffolding.
 */
@JkDoc("Build of a Java project through a JkJavaProject instance.")
@JkDocPluginDeps(JkPluginRepos.class)
public class JkPluginJava extends JkPlugin {

    // ------------------------------ options -------------------------------------------

    @JkDoc("Version for the project to build.")
    public String projectVersion;

    @JkDoc("Publish")
    public final JkPublishOptions publish = new JkPublishOptions();

    @JkDoc("Packing")
    public final JkJavaPackOptions pack = new JkJavaPackOptions();

    @JkDoc("Tests")
    public final JkTestOptions tests = new JkTestOptions();

    @JkDoc("Extra arguments to be passed to the compiler")
    public String compilerExtraArgs;

    // ----------------------------------------------------------------------------------

    private final JkPluginRepos repoPlugin;

    private final JkJavaProject project;

    private final List<JkArtifactId> producedArtifacts = new ArrayList<>();

    protected JkPluginJava(JkBuild build) {
        super(build);
        this.repoPlugin = build.plugins().get(JkPluginRepos.class);
        this.project = new JkJavaProject(this.build.baseDir());
        this.producedArtifacts.add(this.project.maker().mainArtifactId());
    }

    @JkDoc("Adds artifact creation task to 'doDefault' method." +
            "\n  Improves scaffolding by creating a project structure ready to build." +
            "\n  Enriches build information with project build structure.")
    @Override  
    protected void decorateBuild() {
        this.applyOptions();
        this.addDefaultAction(this::doDefault);
        this.setupScaffolder();
    }

    private void doDefault() {
        this.project().maker().clean();
        this.project().maker().pack(producedArtifacts);
    }

    private void applyOptions() {
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
        project.maker().setPublishRepos(repoPlugin.publishRepository());
        if (publish.localOnly) {
            project.maker().setPublishRepos(JkPublishRepos.local());
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
        if (publish.signArtifacts) {
            project.maker().postPack.chain(() -> project.maker().signArtifactFiles(repoPlugin.pgpSigner()));
        }
        if (tests.fork) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(this.tests.jvmOptions);
            project.maker().setJuniter(project.maker().getJuniter().forked(javaProcess));
        }
        project.maker().setSkipTests(tests.skip);
        project.maker().setJuniter(project.maker().getJuniter().withOutputOnConsole(tests.output));
        project.maker().setJuniter(project.maker().getJuniter().withReport(tests.report));
        if (this.compilerExtraArgs != null) {
            project.getCompileSpec().addOptions(JkUtilsString.translateCommandline(this.compilerExtraArgs));
        }
    }

    private void setupScaffolder() {
        String template = JkUtilsIO.read(JkPluginJava.class.getResource("buildclass.snippet"));
        String baseDirName = build.baseDir().getFileName().toString();
        String code = template.replace("${group}", baseDirName).replace("${name}", baseDirName);
        project.getSourceLayout().sources().fileTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().resources().fileTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().tests().fileTrees().stream().forEach(tree -> tree.createIfNotExist());
        project.getSourceLayout().testResources().fileTrees().stream().forEach(tree -> tree.createIfNotExist());
        this.build.scaffolder().setBuildClassCode(code);
    }

    // ------------------------------ Accessors -----------------------------------------

    public JkJavaProject project() {
        return project;
    }

    public List<JkArtifactId> producedArtifacts() {
        return producedArtifacts;
    }

    public JkPathTree ouputTree() {
        return JkPathTree.of(this.project().getOutLayout().outputPath());
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

    @JkDoc("Generates artifacts defined through 'pack' options. " +
            "Does not re-generate artifacts already generated : " +
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
        JkLog.info("Execute 'java#showDependencies' to display details on dependencies.");
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

}
