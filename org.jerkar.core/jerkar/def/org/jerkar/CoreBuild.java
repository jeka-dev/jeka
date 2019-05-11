package org.jerkar;

import org.jerkar.api.depmanagement.JkArtifactId;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkRepoSet;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkJavaProjectMaker;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.builtins.java.JkPluginJava;

import java.nio.file.Path;
import java.util.List;

import static org.jerkar.api.java.project.JkJavaProjectMaker.JAVADOC_ARTIFACT_ID;
import static org.jerkar.api.java.project.JkJavaProjectMaker.SOURCES_ARTIFACT_ID;

/**
 * Build class for Jerkar. Run main method to create full distrib.
 */
public class CoreBuild extends JkRun {

    public static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final String VERSION = "0.7.0-SNAPSHOT";

    final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    private Path distribFolder;

    public String ossrhPwd = "";  // Must be injected by command line

    protected CoreBuild() {
        javaPlugin.tests.fork = false;
        javaPlugin.pack.javadoc = true;
    }

    @Override
    protected void setup()  {
        JkJavaProject project = javaPlugin.getProject();
        project.setVersionedModule(JkModuleId.of("org.jerkar:core").withVersion(VERSION));
        project.setSourceVersion(JkJavaVersion.V8);
        project.setMavenPublicationInfo(mavenPublication());
        JkJavaProjectMaker maker = project.getMaker();
        maker.getTasksForCompilation().setFork(true);  // Fork to avoid compile failure bug on github/travis
        maker.getTasksForTesting().setFork(true);
        maker.addArtifact(DISTRIB_FILE_ID, this::doDistrib);
        this.distribFolder = maker.getOutLayout().getOutputPath().resolve("distrib");
    }

    @Override
    protected void setupAfterPluginActivations() {
        javaPlugin.getProject().getMaker().getTasksForPublishing().setPublishRepos(publishRepos());
    }

    private void doDistrib() {
        final JkJavaProjectMaker maker = javaPlugin.getProject().getMaker();
        maker.makeMissingArtifacts(maker.getMainArtifactId(), SOURCES_ARTIFACT_ID);
        final JkPathTree distrib = JkPathTree.of(distribFolder);
        distrib.deleteContent();
        JkLog.startTask("Create distrib");
        distrib.bring(getBaseDir().getParent().resolve("LICENSE"));
        distrib.merge(getBaseDir().resolve("src/main/dist"));
        distrib.merge(getBaseDir().resolve("src/main/java/META-INF/bin"));
        distrib.bring(maker.getArtifactPath(maker.getMainArtifactId()));
        final List<Path> ivySourceLibs = getBaseTree().goTo("build/libs-sources")
                .andMatching(true, "apache-ivy*.jar").getFiles();
        distrib.goTo("libs-sources")
            .bring(ivySourceLibs)
            .bring(maker.getArtifactPath(SOURCES_ARTIFACT_ID));
        if (javaPlugin.pack.javadoc) {
            maker.makeMissingArtifacts(maker.getMainArtifactId(), JAVADOC_ARTIFACT_ID);
            distrib.goTo("libs-javadoc").bring(maker.getArtifactPath(JAVADOC_ARTIFACT_ID));
        }
        JkLog.execute("Making documentation", () -> new DocMaker(getBaseDir(), distribFolder,
                javaPlugin.getProject().getVersionedModule().getVersion().getValue()).assembleAllDoc());
        if (javaPlugin.tests.runIT) {
            testSamples();
        }
        JkLog.info("Distribution created in " + distrib.getRoot());
        final Path distripZipFile = maker.getArtifactPath(DISTRIB_FILE_ID);
        distrib.zipTo(distripZipFile);
        JkLog.info("Distribution zipped in " + distripZipFile);
        JkLog.endTask();
    }

    // Necessary to publish on OSSRH
    private static JkMavenPublicationInfo mavenPublication() {
        return JkMavenPublicationInfo
                .of("Jerkar", "Build simpler, stronger, faster", "http://jerkar.github.io")
                .withScm("https://github.com/jerkar/jerkar.git")
                .andApache2License()
                .andGitHubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    private JkRepoSet publishRepos() {
        return JkRepoSet.ofOssrhSnapshotAndRelease("djeang", ossrhPwd);
    }

    void testSamples()  {
        JkLog.startTask("Launching integration tests on samples");
        SampleTester sampleTester = new SampleTester(this.getBaseTree());
        sampleTester.restoreEclipseClasspathFile = true;
        sampleTester.doTest();
        JkLog.endTask();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).doDefault();
    }

    public void doDefault() {
        clean();
        doDistrib();
        javaPlugin.publishLocal();
    }

}
