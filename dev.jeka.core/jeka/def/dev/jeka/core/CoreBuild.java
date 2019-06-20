package dev.jeka.core;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectMaker;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkGitWrapper;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkEnv;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static dev.jeka.core.api.java.project.JkJavaProjectMaker.JAVADOC_ARTIFACT_ID;
import static dev.jeka.core.api.java.project.JkJavaProjectMaker.SOURCES_ARTIFACT_ID;

/**
 * Build class for Jerkar. Run main method to create full distrib.
 * For publishing in OSSRH the following options must be set : -ossrhPwd=Xxxxxx -pgp#secretKeyPassword=Xxxxxxx
 */
public class CoreBuild extends JkCommands {

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
        String jekaVersion = git.getVersionWithTagOrSnapshot();
        project.setVersionedModule(JkModuleId.of("dev.jeka:jeka-core").withVersion(jekaVersion));
        project.setSourceVersion(JkJavaVersion.V8);
        if (!JkVersion.of(jekaVersion).isSnapshot()) {
            javaPlugin.pack.javadoc = true;
        }
        project.getManifest().addMainClass("dev.jeka.core.tool.Main");

        JkJavaProjectMaker maker = project.getMaker();
        maker.getTasksForCompilation().setFork(true);  // Fork to avoid compile failure bug on github/travis
        maker.putArtifact(DISTRIB_FILE_ID, this::doDistrib);
        this.distribFolder = maker.getOutLayout().getOutputPath().resolve("distrib");
        maker.getTasksForJavadoc().setJavadocOptions("-notimestamp");
        maker.getTasksForPublishing()
                .setPublishRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd))
                .setMavenPublicationInfo(mavenPublication());

        // include embedded jar
        maker.putArtifact(maker.getMainArtifactId(), this::doPackWithEmbedded);
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
                .of("Jeka", "Automate with plain Java code and nothing else.", "https://jeka.dev")
                .withScm("https://github.com/jerkar/jeka.git")
                .andApache2License()
                .andGitHubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    void testSamples()  {
        JkLog.startTask("Launching integration tests on samples");
        SampleTester sampleTester = new SampleTester(this.getBaseTree());
        sampleTester.restoreEclipseClasspathFile = true;
        try {
            sampleTester.doTest();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        JkLog.endTask();
    }

    private void doPackWithEmbedded() {
        JkJavaProjectMaker maker = javaPlugin.getProject().getMaker();
        Path tempJar = maker.getOutLayout().getOutputPath().resolve("tempJar");
        maker.getTasksForPackaging().createBinJar(tempJar);
        JkLog.startTask("Creating jar");
        Path tempClasses = maker.getOutLayout().getOutputPath().resolve("tempClasses");
        JkPathTree.ofZip(tempJar).copyTo(tempClasses);
        Path embededJar = maker.getOutLayout().getOutputPath().resolve("embedded.jar");
        Path classDir = maker.getOutLayout().getClassDir();
        JkPathTree classTree = JkPathTree.of(classDir);
        Path embeddedFolder = maker.getOutLayout().getOutputPath().resolve("embeddedFolder");
        JkPathTreeSet.of(classTree.andMatching("**/embedded/**/*"))
                .andZip(getBaseDir().resolve(JkConstants.JEKA_DIR).resolve("libs/provided/bouncycastle-pgp-152.jar"))
                .andZip(getBaseDir().resolve(JkConstants.JEKA_DIR).resolve("libs/provided/classgraph-4.8.41.jar"))
                .andZip(getBaseDir().resolve(JkConstants.JEKA_DIR).resolve("libs/provided/ivy-2.4.0.jar"))
                .copyTo(embeddedFolder, StandardCopyOption.REPLACE_EXISTING);
        JkPathTree.of(embeddedFolder)
                .andMatcher(JkPathMatcher.of(false, "META-INF/*.SF", "META-INF/*.RSA"))
                .zipTo(embededJar);
        String checksum = JkPathFile.of(embededJar).getChecksum("MD5");
        String embeddedFinalName = "jeka-embedded-" + checksum + ".jar";
        JkUtilsPath.copy(embededJar, tempClasses.resolve("META-INF/"+ embeddedFinalName));
        JkPathFile.of(tempClasses.resolve("META-INF/jeka-embedded-name")).write(embeddedFinalName.getBytes(Charset.forName("utf-8")));
        JkPathTreeSet.of(JkPathTree.of(tempClasses).andMatching(false, "**/embedded/**"))
                .zipTo(maker.getMainArtifactPath());
        JkLog.endTask();
    }


    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).javaPlugin.clean().pack();
    }

}
