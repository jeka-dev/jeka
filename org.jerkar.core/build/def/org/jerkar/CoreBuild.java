package org.jerkar;

import static org.jerkar.api.project.java.JkJavaProjectMaker.JAVADOC_FILE_ID;
import static org.jerkar.api.project.java.JkJavaProjectMaker.SOURCES_FILE_ID;

import java.nio.file.Path;
import java.util.List;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaProjectMaker;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkOptions;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * Build script for Jerkar 0.7 using new features
 */
public class CoreBuild extends JkJavaProjectBuild {

    public static final JkArtifactFileId DISTRIB_FILE_ID = JkArtifactFileId.of("distrib", "zip");

    private static final String VERSION = "0.7-SNAPSHOT";

    public Path distribFolder;

    @Override
    protected JkJavaProject createProject() {
        final JkJavaProject project = defaultProject();
        applyCommons(project, "core");
        project.maker().addArtifactFile(DISTRIB_FILE_ID, this::doDistrib);
        this.distribFolder = project.getOutLayout().outputPath().resolve("distrib");
        return project;
    }

    @Override
    public void doDefault() {
        project().maker().clean();
        project().maker().makeAllArtifactFiles();
    }

    private void doDistrib() {
        final JkJavaProjectMaker maker = this.project().maker();
        maker.makeArtifactFilesIfNecessary(SOURCES_FILE_ID, JAVADOC_FILE_ID, maker.mainArtifactFileId());
        final JkPathTree distrib = JkPathTree.of(distribFolder);
        distrib.copyIn(baseDir().getParent().resolve("LICENSE"));
        distrib.merge(baseDir().resolve("src/main/dist"));
        distrib.merge(baseDir().resolve("src/main/java/META-INF/bin"));
        distrib.copyIn(maker.artifactPath(maker.mainArtifactFileId()));
        final List<Path> ivySourceLibs = baseTree().goTo("build/libs-sources").accept("apache-ivy*.jar").files();
        distrib.goTo("libs-sources")
            .copyIn(ivySourceLibs)
            .copyIn(maker.artifactPath(SOURCES_FILE_ID));
        distrib.goTo("libs-javadoc").copyIn(maker.artifactPath(JAVADOC_FILE_ID));
        final Path distripZipFile = maker.artifactPath(DISTRIB_FILE_ID);
        distrib.zipTo(distripZipFile);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class).doDefault();
    }

    // build methods shared with other modules from org.jerkar
    public static void applyCommons(JkJavaProject project, String moduleName) {

        // Fork to avoid compile failure bug on github/travis
        project.maker().setBaseCompiler(JkJavaCompiler.base().fork(true));
        project.maker().setTestBaseCompiler(JkJavaCompiler.base().fork(true));

        project.setVersionedModule(JkModuleId.of("org.jerkar", moduleName).version(VERSION));
        project.maker().setArtifactFileNameSupplier(() -> project.getVersionedModule().moduleId().fullName());
        project.setSourceVersion(JkJavaVersion.V8);
        project.setMavenPublicationInfo(mavenPublication());
        project.maker().setPublishRepos(publishRepos());
    }

    private static JkMavenPublicationInfo mavenPublication() {
        return JkMavenPublicationInfo
                .of("Jerkar", "Build simpler, stronger, faster", "http://jerkar.github.io")
                .withScm("https://github.com/jerkar/jerkar.git").andApache2License()
                .andGitHubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    private static JkPublishRepos publishRepos() {
        return JkPublishRepos.ossrh(JkOptions.get("repo.ossrh.username"),
                JkOptions.get("repo.ossrh.password"), JkPgp.of(JkOptions.getAll())).withUniqueSnapshot(true);
    }

}
