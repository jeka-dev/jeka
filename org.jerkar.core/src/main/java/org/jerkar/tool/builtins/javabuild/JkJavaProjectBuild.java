package org.jerkar.tool.builtins.javabuild;

import java.io.File;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.tooling.JkCodeWriterForBuildClass;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkCommonOptions;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkScaffolder;

/**
 * Build for {@link JkJavaProject}
 */
public class JkJavaProjectBuild extends JkBuild {

    // ------------------------------ options ------------------------------------

    @JkDoc("Override version defined in JkJavaProject. No effect if null or blank.")
    private String version = null;

    @JkDoc("Publication")
    private final JkCommonOptions.JkRepoOptions repos = new JkCommonOptions.JkRepoOptions();

    // -------------------------------------------------------------------------------

    private JkJavaProject project;

    public final JkJavaProject project() {
        if (project == null) {
            project = createProject(this.baseTree().root());
            if (project.getVersionedModule() != null && !JkUtilsString.isBlank(version)) {
                project.setVersionedModule(project().getVersionedModule().withVersion(version));
                if (!repos.publishSources) {
                    project.maker().getArtifactFileIdsToNotPublish().addAll(project.artifactsFileIdsWithClassifier("sources"));
                }
                if (!repos.publishTests) {
                    project.maker().getArtifactFileIdsToNotPublish().addAll(project.artifactsFileIdsWithClassifier("test"));
                }
                JkPublishRepos optionPublishRepos = repos.publishRepositories();
                if (optionPublishRepos != null) {
                    project.maker().setPublishRepos(optionPublishRepos);
                }
                if (repos.signPublishedArtifacts) {
                    project.maker().setPublishRepos(project().maker().getPublishRepos().withSigner(repos.pgpSigner()));
                }
                JkRepos optionDownloadRepos = repos.downloadRepositories();
                if (!optionDownloadRepos.isEmpty()) {
                    JkDependencyResolver resolver = project.maker().getDependencyResolver();
                    resolver = resolver.withRepos(optionDownloadRepos.and(JkPublishRepo.local().repo())); // always look in local repo
                    project.maker().setDependencyResolver(resolver);
                }
            }
        }
        return project;
    }

    protected JkJavaProject createProject(File baseDir) {
        return new JkJavaProject(this.baseTree().root());
    }

    @Override
    public void doDefault() {
        this.project().makeMainJar();
    }

    @Override
    public JkFileTree ouputTree() {
        return JkFileTree.of(this.project().getOutLayout().outputDir());
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
