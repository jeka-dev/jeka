package org.jerkar.tool.builtins.java;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaProjectMaker;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.tooling.JkCodeWriterForBuildClass;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.*;

/**
 * Build for {@link JkJavaProject}
 */
@SuppressWarnings("javadoc")
public class JkPluginJava extends JkPlugin2 {

    // ------------------------------ options -------------------------------------------

    @JkDoc("Override version defined in JkJavaProject. No effect if null or blank.")
    protected final String version = null;

    @JkDoc("Publication")
    public final JkRepoOptions repos = new JkRepoOptions();

    @JkDoc("Packing")
    public final JkJavaPackOptions pack = new JkJavaPackOptions();

    @JkDoc("Tests")
    public final JkTestOptions tests = new JkTestOptions();

    // ----------------------------------------------------------------------------------

    private JkJavaProject project;

    protected JkPluginJava(JkBuild build) {
        super(build);
    }

    @Override
    protected void setupBuild() {
        project = new JkJavaProject(this.build.baseDir());
        applyOptions(project);
    }

    @Override
    public void doDefault() {
        this.project().maker().clean();
        this.project().maker().makeAllArtifactFiles();
    }


    private void applyOptions(JkJavaProject project) {
        if (project.getVersionedModule() != null && !JkUtilsString.isBlank(version)) {
            project.setVersionedModule(project.getVersionedModule().withVersion(version));
        }
        if (!repos.publishSources) {
            project.maker().getArtifactFileIdsToNotPublish().addAll(project.maker().artifactsFileIdsWithClassifier("sources"));
        }
        if (!repos.publishTests) {
            project.maker().getArtifactFileIdsToNotPublish().addAll(project.maker().artifactsFileIdsWithClassifier("test"));
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
        if (pack.javadoc != null && pack.javadoc) {
            project.removeArtifactFile(JkJavaProjectMaker.JAVADOC_FILE_ID);
        } else if (pack.javadoc != null && !pack.javadoc) {
            project.maker().addArtifactFile(JkJavaProjectMaker.JAVADOC_FILE_ID, project.maker()::makeJavadocJar);
        }
        if (pack.tests != null && pack.tests) {
            project.removeArtifactFile(JkJavaProjectMaker.TEST_FILE_ID, JkJavaProjectMaker.TEST_SOURCE_FILE_ID);
        } else if (pack.tests != null && !pack.tests) {
            project.maker().addArtifactFile(JkJavaProjectMaker.TEST_FILE_ID, project.maker()::makeTestJar);
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

    private JkScaffolder scaffolder() {
        final JkCodeWriterForBuildClass codeWriter = new JkCodeWriterForBuildClass();
        codeWriter.extendedClass = JkPluginJava.class.getName();
        codeWriter.imports.clear();
        codeWriter.imports.addAll(JkCodeWriterForBuildClass.importsForJkJavaProjectBuild());
        return this.build.scaffolder().buildClassWriter(codeWriter);
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
