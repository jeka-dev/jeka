package org.jerkar.tool.builtins.java;

import org.jerkar.api.depmanagement.JkDependencyNode;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkPublishRepo;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolveResult;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.tooling.JkCodeWriterForBuildClass;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkRepoOptions;
import org.jerkar.tool.JkScaffolder;

/**
 * Build for {@link JkJavaProject}
 */
@SuppressWarnings("javadoc")
public abstract class JkJavaProjectBuild extends JkBuild {

    // ------------------------------ options ------------------------------------

    @JkDoc("Override version defined in JkJavaProject. No effect if null or blank.")
    protected final String version = null;

    @JkDoc("Publication")
    protected final JkRepoOptions repos = new JkRepoOptions();

    @JkDoc("Packing")
    protected final JkJavaPackOptions pack = new JkJavaPackOptions();

    @JkDoc("Tests")
    protected final JkTestOptions tests = new JkTestOptions();

    // -------------------------------------------------------------------------------

    private JkJavaProject project;

    public final JkJavaProject project() {
        if (project == null) {
            project = createProject();
            applyOptions(project);
            setupPlugins();
        }
        return project;
    }

    protected abstract JkJavaProject createProject();

    /**
     * Provides a default project usable in {@link #createProject()} method
     */
    protected JkJavaProject defaultProject() {
        return new JkJavaProject(this.baseDir());
    }

    protected void setupPlugins() {
        // Do nothing by default
    }

    private void applyOptions(JkJavaProject project) {
        if (project.getVersionedModule() != null && !JkUtilsString.isBlank(version)) {
            project.setVersionedModule(project().getVersionedModule().withVersion(version));
        }
        if (!repos.publishSources) {
            project.maker().getArtifactFileIdsToNotPublish().addAll(project.artifactsFileIdsWithClassifier("sources"));
        }
        if (!repos.publishTests) {
            project.maker().getArtifactFileIdsToNotPublish().addAll(project.artifactsFileIdsWithClassifier("test"));
        }
        final JkPublishRepos optionPublishRepos = repos.publishRepositories();
        if (optionPublishRepos != null) {
            project.maker().setPublishRepos(optionPublishRepos);
        }
        if (repos.signPublishedArtifacts) {
            project.maker().setPublishRepos(project().maker().getPublishRepos().withSigner(repos.pgpSigner()));
        }
        final JkRepos optionDownloadRepos = repos.downloadRepositories();
        if (!optionDownloadRepos.isEmpty()) {
            JkDependencyResolver resolver = project.maker().getDependencyResolver();
            resolver = resolver.withRepos(optionDownloadRepos.and(JkPublishRepo.local().repo())); // always look in local repo
            project.maker().setDependencyResolver(resolver);
        }
        if (pack.javadoc != null && pack.javadoc) {
            project.removeArtifactFile(JkJavaProject.JAVADOC_FILE_ID);
        } else if (pack.javadoc != null && !pack.javadoc) {
            project.addArtifactFile(JkJavaProject.JAVADOC_FILE_ID, project.maker()::makeJavadocJar);
        }
        if (pack.tests != null && pack.tests) {
            project.removeArtifactFile(JkJavaProject.TEST_FILE_ID, JkJavaProject.TEST_SOURCE_FILE_ID);
        } else if (pack.tests != null && !pack.tests) {
            project.addArtifactFile(JkJavaProject.TEST_FILE_ID, project.maker()::makeTestJar);
        }
        if (pack.checksums().length > 0) {
            project.maker().afterPackage.chain(() -> project.maker().checksum(pack.checksums()));
        }
        if (pack.signWithPgp) {
            project.maker().afterPackage.chain(() -> project.maker().signArtifactFiles(repos.pgpSigner()));
        }
        if (tests.fork != null && tests.fork) {
            final JkJavaProcess javaProcess = JkJavaProcess.of().andCommandLine(this.tests.jvmOptions);
            project.maker().setJuniter(project.maker().getJuniter().forked(javaProcess));
        } else if (tests.fork != null && !tests.fork){
            project.maker().setJuniter(project.maker().getJuniter().forked(false));
        }
        if (tests.skip != null) {
            project.maker().setSkipTests(tests.skip);
        }
        if (tests.output != null) {
            project.maker().setJuniter(project.maker().getJuniter().withOutputOnConsole(tests.output));
        }
        if (tests.report != null) {
            project.maker().setJuniter(project.maker().getJuniter().withReport(tests.report));
        }
    }

    @Override
    public void doDefault() {
        this.project().maker().clean();
        this.project().makeAllArtifactFiles();
    }

    @Override
    public JkPathTree ouputTree() {
        return JkPathTree.of(this.project().getOutLayout().outputPath());
    }

    @Override
    protected JkScaffolder createScaffolder() {
        final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
        codeWriter.extendedClass = JkJavaProjectBuild.class.getName();
        codeWriter.imports.clear();
        codeWriter.imports.addAll(JkCodeWriterForBuildClass.importsForJkJavaProjectBuild());
        return super.scaffolder().buildClassWriter(codeWriter);
    }

    // ------------------------------- command line methods

    /**
     * Displays the resolved dependency tree on the console.
     */
    @JkDoc("Displays the resolved dependency tree on the console.")
    public final void showDependencies() {
        JkLog.infoHeaded("Resolved dependencies ");
        final JkResolveResult resolveResult = this.project.maker().getDependencyResolver()
                .resolve(this.buildDependencies().withDefaultScope(JkJavaDepScopes.COMPILE_AND_RUNTIME));
        final JkDependencyNode tree = resolveResult.dependencyTree();
        JkLog.info(tree.toStrings());
    }
}
