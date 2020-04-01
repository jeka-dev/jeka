package dev.jeka.core;

import dev.jeka.core.api.depmanagement.JkArtifactId;
import dev.jeka.core.api.depmanagement.JkMavenPublicationInfo;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectMaker;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.java.testing.JkTestSelection;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkGitWrapper;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkEnv;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static dev.jeka.core.api.java.project.JkJavaProjectMaker.JAVADOC_ARTIFACT_ID;
import static dev.jeka.core.api.java.project.JkJavaProjectMaker.SOURCES_ARTIFACT_ID;

/**
 * Build class for Jeka. Run main method to create full distrib.
 * For publishing in OSSRH the following options must be set : -ossrhPwd=Xxxxxx -pgp#secretKeyPassword=Xxxxxxx
 */
public class CoreBuild extends JkCommandSet {

    private static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final JkArtifactId WRAPPER_ARTIFACT_ID = JkArtifactId.of("wrapper", "jar");

    final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    private Path distribFolder;

    private final JkGitWrapper git;

    public boolean runIT;

    @JkEnv("OSSRH_USER")
    public String ossrhUser;

    @JkEnv("OSSRH_PWD")
    public String ossrhPwd;

    @JkEnv("GH_TOKEN")
    public String githubToken;

    @JkEnv("TRAVIS_BRANCH")
    public String travisBranch;

    protected CoreBuild() {
        javaPlugin.tests.fork = false;
        javaPlugin.pack.javadoc = true;
        git = JkGitWrapper.of(this.getBaseDir());
    }

    @Override
    protected void setup()  {

        // Module version is driven by git repository info
        String jekaVersion = git.getVersionFromTags();
        if (!JkVersion.of(jekaVersion).isSnapshot()) {
            javaPlugin.pack.javadoc = true;
        }
        JkJavaProject project = javaPlugin.getProject();
        project.setVersionedModule("dev.jeka:jeka-core", jekaVersion);

        JkJavaProjectMaker maker = project.getMaker();
        this.distribFolder = maker.getOutLayout().getOutputPath().resolve("distrib");
        maker
            .putArtifact(DISTRIB_FILE_ID, this::doDistrib)
            .putArtifact(maker.getMainArtifactId(), this::doPackWithEmbedded)
            .putArtifact(WRAPPER_ARTIFACT_ID, this::doWrapper); // define wrapper
        maker.getSteps()
            .getCompilation()
                .addOptions("-Xlint:none","-g")
                .setJavaVersion(JkJavaVersion.V8)
                .getCompiler()
                    .setForkingWithJavac()._._
            .getTesting()
                .getCompilation()
                    .getCompiler()
                        .setDefault()._._
                .getTestProcessor()
                    .setForkingProcess(true)
                    .getEngineBehavior()
                        .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.FULL)._._
                .getTestSelection()
                    .addIncludePatternsIf(runIT, JkTestSelection.IT_INCLUDE_PATTERN)._._
            .getPackaging()
                .getManifest()
                    .addMainClass("dev.jeka.core.tool.Main")._._
            .getDocumentation()
                .getJavadocProcessor()
                    .addOptions("-notimestamp")._._
            .getPublishing()
                .setPublishRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd))
                .setMavenPublicationInfo(mavenPublication())
                .getPostActions()
                    .append(() -> createGithubRelease(jekaVersion));
    }

    private void createGithubRelease(String version) {
        if (version.endsWith(".RELEASE")) {
            GithubReleaseContentEditor githubReleaseContentEditor =
                    new GithubReleaseContentEditor("jerkar/jeka", travisBranch, githubToken);
            String releaseNote = githubReleaseContentEditor.getReleaseNoteForTag(
                    this.getBaseDir().resolve("../release-note.md"), version);
            if (releaseNote!= null) {
                githubReleaseContentEditor.createRelease(version, releaseNote);
            }
        }
    }

    public void publishDocsOnGithubPage() {
        JkJavaProject project = javaPlugin.getProject();
        Path javadocSourceDir = project.getMaker().getOutLayout().getJavadocDir();
        Path tempRepo = getOutputDir().resolve("pagesGitRepo");
        String userPrefix = githubToken == null ? "" : githubToken + "@";
        git.exec("clone", "--depth=1", "https://" + userPrefix + "github.com/jerkar/jeka-dev-site.git",
                tempRepo.toString());
        project.getMaker().getSteps().getDocumentation().runIfNecessary();
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
        maker.makeMissingArtifacts(maker.getMainArtifactId(), SOURCES_ARTIFACT_ID, WRAPPER_ARTIFACT_ID);
        final JkPathTree distrib = JkPathTree.of(distribFolder);
        distrib.deleteContent();
        JkLog.startTask("Create distrib");
        final List<Path> ivySourceLibs = getBaseTree().goTo("jeka/libs-sources")
                .andMatching(true, "ivy-*.jar").getFiles();
        distrib
                .importFiles(getBaseDir().getParent().resolve("LICENSE"))
                .importDir(getBaseDir().resolve("src/main/dist"))
                .importDir(getBaseDir().resolve("src/main/java/META-INF/bin"))
                .importFiles(maker.getArtifactPath(maker.getMainArtifactId()))
                .importFiles(maker.getArtifactPath(WRAPPER_ARTIFACT_ID))
                .goTo("libs-sources")
                    .importFiles(ivySourceLibs)
                    .importFiles(maker.getArtifactPath(SOURCES_ARTIFACT_ID));
        if (javaPlugin.pack.javadoc) {
            maker.makeMissingArtifacts(maker.getMainArtifactId(), JAVADOC_ARTIFACT_ID);
            distrib.importFiles(maker.getArtifactPath(JAVADOC_ARTIFACT_ID));
        }
        makeDocs();
        if (runIT) {
            testSamples();
        }
        JkLog.info("Distribution created in " + distrib.getRoot());
        final Path distripZipFile = maker.getArtifactPath(DISTRIB_FILE_ID);
        distrib.zipTo(distripZipFile);
        JkLog.info("Distribution zipped in " + distripZipFile);
        JkLog.endTask();
    }

    private void makeDocs() {
        JkLog.startTask("Making documentation");
        new DocMaker(getBaseDir(), distribFolder,
                javaPlugin.getProject().getVersionedModule().getVersion().getValue()).assembleAllDoc();
        JkLog.endTask();
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
        JkLog.startTask("Creating main jar");
        JkJavaProjectMaker maker = javaPlugin.getProject().getMaker();

        // Main jar
        Path targetJar = maker.getMainArtifactPath();
        maker.getSteps().getPackaging().createBinJar(targetJar);
        JkPathTree jarTree = JkPathTree.ofZip(targetJar);


        // Create an embedded jar containing all 3rd party libs + embedded part code in jeka project
        Path embeddedJar = maker.getOutLayout().getOutputPath().resolve("embedded.jar");
        JkPathTree classTree = JkPathTree.of(maker.getOutLayout().getClassDir());
        Path providedLibs = getBaseDir().resolve(JkConstants.JEKA_DIR).resolve("libs/provided");
        JkPathTreeSet.of(classTree.andMatching("**/embedded/**/*"))
                .andZips(providedLibs.resolve("bouncycastle-pgp-152.jar"))
                .andZip(providedLibs.resolve("classgraph-4.8.41.jar"))
                .andZip(providedLibs.resolve("ivy-2.5.0.jar"))
                .zipTo(embeddedJar);
        JkPathTree.ofZip(embeddedJar).andMatching( "META-INF/*.SF", "META-INF/*.RSA")
                .deleteContent().close();

        // Name uniquely this embedded jar according its content
        String checksum = JkPathFile.of(embeddedJar).getChecksum("MD5");
        String embeddedFinalName = "jeka-embedded-" + checksum + ".jar";

        // Copy embbeded jar into temp folder and remove embedded part code from jeka classes
        jarTree.goTo("META-INF").importFile(embeddedJar, embeddedFinalName);
        Path embeddedNaneFile = jarTree.get("META-INF/jeka-embedded-name");
        JkUtilsPath.deleteIfExists(embeddedNaneFile);
        JkPathFile.of(embeddedNaneFile).write(embeddedFinalName.getBytes(Charset.forName("utf-8")));
        jarTree.andMatching( "**/embedded/**").deleteContent();
        jarTree.close();

        // Cleanup
        JkUtilsPath.deleteIfExists(embeddedJar);
        JkLog.endTask();
    }

    private void doWrapper() {
        JkJavaProjectMaker maker = javaPlugin.getProject().getMaker();
        Path wrapperJar = maker.getArtifactPath(WRAPPER_ARTIFACT_ID);
        JkPathTree.of(maker.getOutLayout().getClassDir()).andMatching("dev/jeka/core/wrapper/**").zipTo(wrapperJar);
    }

    public static void main(String[] args) {
        CoreBuild coreBuild = JkInit.instanceOf(CoreBuild.class, args);
        coreBuild.javaPlugin.clean().pack();
        //coreBuild.copyToWrapper();
    }

}
