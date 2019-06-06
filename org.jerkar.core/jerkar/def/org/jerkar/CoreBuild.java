package org.jerkar;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkJavaProjectMaker;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.system.JkPrompt;
import org.jerkar.api.tooling.JkGitWrapper;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkEnv;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.builtins.java.JkPluginJava;
import org.jerkar.tool.builtins.repos.JkPluginPgp;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.jerkar.api.java.project.JkJavaProjectMaker.JAVADOC_ARTIFACT_ID;
import static org.jerkar.api.java.project.JkJavaProjectMaker.SOURCES_ARTIFACT_ID;

/**
 * Build class for Jerkar. Run main method to create full distrib.
 * For publishing in OSSRH the following options must be set : -ossrhPwd=Xxxxxx -pgp#secretKeyPassword=Xxxxxxx
 */
public class CoreBuild extends JkRun {

    public static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    private Path distribFolder;

    private final JkGitWrapper git;

    @JkEnv("OSSRH_USER")
    public String ossrhUser;

    @JkEnv("OSSRH_PWD")
    public String ossrhPwd;

    @JkEnv("GH_TOKEN")
    public String githubToken;

    protected CoreBuild() {
        javaPlugin.tests.fork = false;
        javaPlugin.pack.javadoc = true;
        git = JkGitWrapper.of(this.getBaseDir());
        getPlugin(JkPluginPgp.class).keyName = "jerkar.org";
    }

    @Override
    protected void setup()  {
        JkJavaProject project = javaPlugin.getProject();

        // Module version is driven by git repository info
        project.setVersionedModule(JkModuleId.of("org.jerkar:core").withVersion(git.getVersionWithTagOrSnapshot()));
        javaPlugin.publish.signArtifacts = project.getVersionedModule().getVersion().isSnapshot();
        project.setSourceVersion(JkJavaVersion.V8);
        project.setMavenPublicationInfo(mavenPublication());
        JkJavaProjectMaker maker = project.getMaker();
        maker.getTasksForCompilation().setFork(true);  // Fork to avoid compile failure bug on github/travis
        maker.addArtifact(DISTRIB_FILE_ID, this::doDistrib);
        this.distribFolder = maker.getOutLayout().getOutputPath().resolve("distrib");
        JkVersion version = javaPlugin.getProject().getVersionedModule().getVersion();
        if (!version.isSnapshot()) {
            javaPlugin.pack.javadoc = true;
            javaPlugin.publish.signArtifacts = true;
            maker.getTasksForPublishing().getPostActions().chain(this::tagGit);
        }
        maker.getTasksForJavadoc().setJavadocOptions("-notimestamp");
    }

    @Override
    protected void setupAfterPluginActivations() {
        javaPlugin.getProject().getMaker().getTasksForPublishing().setPublishRepos(publishRepos());

    }

    public void publishDocsOnGithubPage() {
        JkJavaProject project = javaPlugin.getProject();
        Path javadocSourceDir = project.getMaker().getOutLayout().getJavadocDir();
        Path tempRepo = getOutputDir().resolve("pagesGitRepo");
        JkProcess git = JkProcess.of("git").withFailOnError(true);
        String userPrefix = githubToken == null ? "" : githubToken + "@";
        git.andParams("clone", "--depth=1", "https://" + userPrefix + "github.com/jeka-dev/jeka-dev.github.io.git",
                tempRepo.toString()).runSync();
        project.getMaker().getTasksForJavadoc().runIfNecessary();
        Path javadocTarget = tempRepo.resolve(tempRepo.resolve("docs/javadoc"));
        JkPathTree.of(javadocSourceDir).copyTo(javadocTarget, StandardCopyOption.REPLACE_EXISTING);
        makeDocs();
        JkPathTree.of(distribFolder.resolve("doc")).copyTo(tempRepo.resolve("docs"), StandardCopyOption.REPLACE_EXISTING);
        git = git.withWorkingDir(tempRepo).withLogCommand(true);
        git.andParams("add", "*").runSync();
        git.andParams("commit", "-am", "Doc").withFailOnError(false).runSync();
        git.andParams("push").runSync();
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
        makeDocs();
        if (javaPlugin.tests.runIT) {
            testSamples();
        }
        JkLog.info("Distribution created in " + distrib.getRoot());
        final Path distripZipFile = maker.getArtifactPath(DISTRIB_FILE_ID);
        distrib.zipTo(distripZipFile);
        JkLog.info("Distribution zipped in " + distripZipFile);
        JkLog.endTask();
    }

    private void makeDocs() {
        JkLog.execute("Making documentation", () -> new DocMaker(getBaseDir(), distribFolder,
                javaPlugin.getProject().getVersionedModule().getVersion().getValue()).assembleAllDoc());
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
        return JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd);
    }

    void testSamples()  {
        JkLog.startTask("Launching integration tests on samples");
        SampleTester sampleTester = new SampleTester(this.getBaseTree());
        sampleTester.restoreEclipseClasspathFile = true;
        sampleTester.doTest();
        JkLog.endTask();
    }

    @JkDoc("Create a new git tag for release purpose.")
    public void tagGit() {
        JkGitWrapper git = JkGitWrapper.of(getBaseDir());
        System.out.println("Existing tags :");
        git.exec("tag");
        String newTag = JkPrompt.ask("Enter new tag : ");
        JkUtilsAssert.isTrue(!git.isDirty(), "Git workspace is dirty. Cannot put tag");
        git.tagAndPush(newTag);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).javaPlugin.clean().pack();
    }

}
