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

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin for building Java projects. It comes with a {@link JkJavaProject} pre-configured with {@link JkOptions}.
 * and a decoration for scaffolding.
 */
@SuppressWarnings("javadoc")
public class JkPluginJava extends JkPlugin {

    // ------------------------------ options -------------------------------------------

    @JkDoc("Version for the project to build")
    public String projectVersion;

    @JkDoc("Publication")
    public final JkRepoOptions repos = new JkRepoOptions();

    @JkDoc("Packing")
    public final JkJavaPackOptions pack = new JkJavaPackOptions();

    @JkDoc("Tests")
    public final JkTestOptions tests = new JkTestOptions();

    // ----------------------------------------------------------------------------------

    private final JkJavaProject project;

    private final List<JkArtifactId> producedArtifacts = new ArrayList<>();

    protected JkPluginJava(JkBuild build) {
        super(build);
        this.project = new JkJavaProject(this.build.baseDir());
        this.producedArtifacts.add(project.maker().mainArtifactId());
    }

    @Override  
    protected void decorateBuild() {
        this.applyOptions();
        this.addDefaultAction(this::doDefault);
        this.setupScaffolder();
        this.setupInfo();
    }

    private void doDefault() {
        this.project().maker().clean();
        this.project().maker().pack(producedArtifacts);
    }

    private void applyOptions() {
        if (project.getVersionedModule() != null && !JkUtilsString.isBlank(projectVersion)) {
            project.setVersionedModule(project.getVersionedModule().withVersion(projectVersion));
        }
        if (!repos.publishSources) {
            project.maker().getArtifactFileIdsToNotPublish().addAll(project.maker().artifactIdsWithClassifier("sources"));
        }
        if (!repos.publishTests) {
            project.maker().getArtifactFileIdsToNotPublish().addAll(project.maker().artifactIdsWithClassifier("test"));
        }
        final JkPublishRepos optionPublishRepos = repos.publishRepositories();
        if (optionPublishRepos != null) {
            project.maker().setPublishRepos(optionPublishRepos);
        }
        if (repos.signPublishedArtifacts) {
            project.maker().setPublishRepos(project.maker().getPublishRepos().withSigner(repos.pgpSigner()));
        }
        final JkRepos optionDownloadRepos = repos.downloadRepositories();
        if (!optionDownloadRepos.isEmpty()) {
            JkDependencyResolver resolver = project.maker().getDependencyResolver();
            resolver = resolver.withRepos(optionDownloadRepos.and(JkPublishRepo.local().repo())); // always look in local repo
            project.maker().setDependencyResolver(resolver);
        }
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
            project.maker().afterPack.chain(() -> project.maker().checksum(pack.checksums()));
        }
        if (pack.signWithPgp) {
            project.maker().afterPack.chain(() -> project.maker().signArtifactFiles(repos.pgpSigner()));
        }
        if (tests.fork) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(this.tests.jvmOptions);
            project.maker().setJuniter(project.maker().getJuniter().forked(javaProcess));
        }
        project.maker().setSkipTests(tests.skip);
        project.maker().setJuniter(project.maker().getJuniter().withOutputOnConsole(tests.output));
        project.maker().setJuniter(project.maker().getJuniter().withReport(tests.report));
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

    private void setupInfo() {
        build.infoProvider()
                .append(project.toString()).append('\n')
                .append(project.getVersionedModule());
    }

    // ------------------------------ Accessors -----------------------------------------

    public JkJavaProject project() {
        return project;
    }

    public JkPathTree ouputTree() {
        return JkPathTree.of(this.project().getOutLayout().outputPath());
    }

    // ------------------------------- command line methods -----------------------------

    /**
     * Displays the resolved dependency tree on the console.
     */
    @JkDoc("Displays the resolved dependency tree on the console.")
    public final void showDependencies() {
        JkLog.infoHeaded("Resolved dependencies ");
        final JkResolveResult resolveResult = this.project().maker().getDependencyResolver()
                .resolve(this.project.getDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME));
        final JkDependencyNode tree = resolveResult.dependencyTree();
        JkLog.info(tree.toStrings());
    }

}
