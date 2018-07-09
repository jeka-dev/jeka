package org.jerkar;

import static org.jerkar.api.project.java.JkJavaProjectMaker.JAVADOC_ARTIFACT_ID;
import static org.jerkar.api.project.java.JkJavaProjectMaker.SOURCES_ARTIFACT_ID;

import java.nio.file.Path;
import java.util.List;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaProjectMaker;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * Build script for Jerkar 0.7 using new features
 */
public class CoreBuild extends JkJavaProjectBuild {

    public static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final String VERSION = "0.7-SNAPSHOT";

    public Path distribFolder;

    @Override
    protected void setupOptionDefaults() {
        java().tests.fork = false;;
    }

    protected void configurePlugins() {
        applyCommonSettings(project(), "core");
        maker().defineArtifact(DISTRIB_FILE_ID, this::doDistrib);
        this.distribFolder = project().getOutLayout().outputPath().resolve("distrib");
    }

    private void doDistrib() {
        final JkJavaProjectMaker maker = this.java().project().maker();
        maker.makeArtifactsIfAbsent(SOURCES_ARTIFACT_ID, JAVADOC_ARTIFACT_ID, maker.mainArtifactId());
        final JkPathTree distrib = JkPathTree.of(distribFolder);
        distrib.copyIn(baseDir().getParent().resolve("LICENSE"));
        distrib.merge(baseDir().resolve("src/main/dist"));
        distrib.merge(baseDir().resolve("src/main/java/META-INF/bin"));
        distrib.copyIn(maker.artifactPath(maker.mainArtifactId()));
        final List<Path> ivySourceLibs = baseTree().goTo("build/libs-sources").accept("apache-ivy*.jar").files();
        distrib.goTo("libs-sources")
            .copyIn(ivySourceLibs)
            .copyIn(maker.artifactPath(SOURCES_ARTIFACT_ID));
        distrib.goTo("libs-javadoc").copyIn(maker.artifactPath(JAVADOC_ARTIFACT_ID));
        final Path distripZipFile = maker.artifactPath(DISTRIB_FILE_ID);
        distrib.zipTo(distripZipFile);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class).doDefault();
    }

    // Applies settings common to all projects within org.jerkar
    public static void applyCommonSettings(JkJavaProject project, String moduleName) {

        // Fork to avoid compile failure bug on github/travis
        project.maker().setCompiler(JkJavaCompiler.of().fork(true));
        project.maker().setTestCompiler(JkJavaCompiler.of().fork(true));

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

    private static JkRepoSet publishRepos() {
        return JkRepoSet.local();
    }

}
