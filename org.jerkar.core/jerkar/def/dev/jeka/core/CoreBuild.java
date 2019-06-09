package dev.jeka.core;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectMaker;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkPrompt;
import dev.jeka.core.api.tooling.JkGitWrapper;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkEnv;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkRun;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.repos.JkPluginPgp;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static dev.jeka.core.api.java.project.JkJavaProjectMaker.JAVADOC_ARTIFACT_ID;
import static dev.jeka.core.api.java.project.JkJavaProjectMaker.SOURCES_ARTIFACT_ID;

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
    }

    @Override
    protected void setup()  {
        JkJavaProject project = javaPlugin.getProject();

        // Module version is driven by git repository info
        project.setVersionedModule(JkModuleId.of("dev.jeka:jeka-core").withVersion(git.getVersionWithTagOrSnapshot()));

        project.setSourceVersion(JkJavaVersion.V8);
        project.setMavenPublicationInfo(mavenPublication());
        JkJavaProjectMaker maker = project.getMaker();
        maker.getTasksForCompilation().setFork(true);  // Fork to avoid compile failure bug on github/travis
        maker.addArtifact(DISTRIB_FILE_ID, this::doDistrib);
        this.distribFolder = maker.getOutLayout().getOutputPath().resolve("distrib");
        JkVersion version = javaPlugin.getProject().getVersionedModule().getVersion();
        if (!version.isSnapshot()) {
            javaPlugin.pack.javadoc = true;
        }
        maker.getTasksForJavadoc().setJavadocOptions("-notimestamp");

        // Use embedded secret ring protected by a passphrase to deploy releases on ossrh
        JkPluginPgp pgpPlugin = getPlugins().get(JkPluginPgp.class);
        pgpPlugin.secretRingPath = getBaseDir().resolve("jerkar/secring.gpg").toString();
    }

    @Override
    protected void setupAfterPluginActivations() {
        javaPlugin.getProject().getMaker().getTasksForPublishing().setPublishRepos(publishRepos());

    }

    public void publishDocsOnGithubPage() {
        JkJavaProject project = javaPlugin.getProject();
        Path javadocSourceDir = project.getMaker().getOutLayout().getJavadocDir();
        Path tempRepo = getOutputDir().resolve("pagesGitRepo");
        String userPrefix = githubToken == null ? "" : githubToken + "@";
        git.exec("clone", "--depth=1", "https://" + userPrefix + "github.com/jeka-dev/jeka-dev.github.io.git",
                tempRepo.toString());
        project.getMaker().getTasksForJavadoc().runIfNecessary();
        Path javadocTarget = tempRepo.resolve(tempRepo.resolve("docs/javadoc"));
        JkPathTree.of(javadocSourceDir).copyTo(javadocTarget, StandardCopyOption.REPLACE_EXISTING);
        makeDocs();
        JkPathTree.of(distribFolder.resolve("doc")).copyTo(tempRepo.resolve("docs"), StandardCopyOption.REPLACE_EXISTING);
        JkGitWrapper gitTemp = JkGitWrapper.of(tempRepo).withLogCommand(true);
        gitTemp.exec("add", "*");
        gitTemp.withFailOnError(false).exec("commit", "-am", "Doc");
        gitTemp.exec("push");
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
        if (git.isDirty()) {
            throw new JkException("Git workspace is dirty. Cannot put tag.");
        }
        git.tagAndPush(newTag);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).javaPlugin.clean().pack();
    }

}
