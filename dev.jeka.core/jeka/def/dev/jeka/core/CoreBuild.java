package dev.jeka.core;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
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

import static dev.jeka.core.api.java.project.JkJavaProject.JAVADOC_ARTIFACT_ID;
import static dev.jeka.core.api.java.project.JkJavaProject.SOURCES_ARTIFACT_ID;

/**
 * Build class for Jeka. Run main method to create full distrib.
 * For publishing in OSSRH the following options must be set : -ossrhPwd=Xxxxxx -pgp#secretKeyPassword=Xxxxxxx
 */
public class CoreBuild extends JkCommandSet {

    private static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final JkArtifactId WRAPPER_ARTIFACT_ID = JkArtifactId.of("wrapper", "jar");

    final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

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
        javaPlugin.getProject()
            .getArtifactProducer()
                .putMainArtifact(this::doPackWithEmbedded)
                .putArtifact(DISTRIB_FILE_ID, this::doDistrib)
                .putArtifact(WRAPPER_ARTIFACT_ID, this::doWrapper).__ // define wrapper
            .getCompilation()
                .getLayout()
                    .includeSourceDirsInResources().__
                .addOptions("-Xlint:none","-g")
                .setJavaVersion(JkJavaVersion.V8)
                .getCompiler()
                    .setForkingWithJavac().__.__
            .getTesting()
                .getCompilation()
                    .getLayout()
                        .includeSourceDirsInResources().__
                    .getCompiler()
                        .setDefault().__.__
                .getTestProcessor()
                    .setForkingProcess(true)
                    .getEngineBehavior()
                        .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.TREE).__.__
                .getTestSelection()
                    .addIncludePatterns(JkTestSelection.STANDARD_INCLUDE_PATTERN)
                    .addIncludePatternsIf(runIT, JkTestSelection.IT_INCLUDE_PATTERN).__.__
            .getPackaging()
                .getManifest()
                    .addMainClass("dev.jeka.core.tool.Main").__.__
            .getDocumentation()
                .getJavadocProcessor()
                    .setDisplayOutput(false)
                    .addOptions("-notimestamp").__.__
            .getPublication()
                .setVersionedModule("dev.jeka:jeka-core", jekaVersion)
                .setPublishRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd))
                .getMavenPublicationInfo()
                    .getProjectInfo()
                        .setName("jeka")
                        .setUrl("https://jeka.dev")
                        .setDescription("Automate with plain Java code and nothing else.").__
                    .getScm()
                        .setUrl("https://github.com/jerkar/jeka.git").__
                    .addApache2License()
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr").__
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

    private Path distribFolder() {
        return javaPlugin.getProject().getOutputDir().resolve("distrib");
    }

    private void doDistrib(Path distribFile) {
        final JkArtifactProducer artifactProducer = javaPlugin.getProject().getArtifactProducer();
        artifactProducer.makeMissingArtifacts(artifactProducer.getMainArtifactId(),
                SOURCES_ARTIFACT_ID, WRAPPER_ARTIFACT_ID);
        final JkPathTree distrib = JkPathTree.of(distribFolder());
        distrib.deleteContent();
        JkLog.startTask("Create distrib");
        final List<Path> ivySourceLibs = getBaseTree().goTo("jeka/libs-sources")
                .andMatching(true, "ivy-*.jar").getFiles();
        distrib
            .importFiles(getBaseDir().getParent().resolve("LICENSE"))
            .importDir(getBaseDir().resolve("src/main/dist"))
            .importDir(getBaseDir().resolve("src/main/java/META-INF/bin"))
            .importFiles(artifactProducer.getArtifactPath(artifactProducer.getMainArtifactId()))
            .importFiles(artifactProducer.getArtifactPath(WRAPPER_ARTIFACT_ID))
            .goTo("libs-sources")
                .importFiles(ivySourceLibs)
                .importFiles(artifactProducer.getArtifactPath(SOURCES_ARTIFACT_ID));
        if (javaPlugin.pack.javadoc) {
            artifactProducer.makeMissingArtifacts(artifactProducer.getMainArtifactId(), JAVADOC_ARTIFACT_ID);
            distrib.importFiles(artifactProducer.getArtifactPath(JAVADOC_ARTIFACT_ID));
        }
        makeDocs();
        if (runIT) {
            testSamples();
        }
        JkLog.info("Distribution created in " + distrib.getRoot());
        distrib.zipTo(distribFile);
        JkLog.info("Distribution zipped in " + distribFile);
        JkLog.endTask();
    }

    private void makeDocs() {
        JkLog.startTask("Making documentation");
        String version = javaPlugin.getProject().getPublication().getVersionedModule()
                .getVersion().getValue();
        new DocMaker(getBaseDir(), distribFolder(), version).assembleAllDoc();
        JkLog.endTask();
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

    private void doPackWithEmbedded(Path targetJar) {
        JkLog.startTask("Creating main jar");

        // Main jar
        JkJavaProject project = javaPlugin.getProject();
        project.getPackaging().createBinJar(targetJar);
        JkPathTree jarTree = JkPathTree.ofZip(targetJar);

        // Create an embedded jar containing all 3rd party libs + embedded part code in jeka project
        Path embeddedJar = project.getOutputDir().resolve("embedded.jar");
        JkPathTree classTree = JkPathTree.of(project.getCompilation().getLayout().resolveClassDir());
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

    private void doWrapper(Path wrapperJar) {
        JkPathTree.of(javaPlugin.getProject().getCompilation().getLayout()
                .resolveClassDir()).andMatching("dev/jeka/core/wrapper/**").zipTo(wrapperJar);
    }

    public void publishDocsOnGithubPage() {
        JkJavaProject project = javaPlugin.getProject();
        Path javadocSourceDir = project.getDocumentation().getJavadocDir();
        Path tempRepo = getOutputDir().resolve("pagesGitRepo");
        String userPrefix = githubToken == null ? "" : githubToken + "@";
        git.exec("clone", "--depth=1", "https://" + userPrefix + "github.com/jerkar/jeka-dev-site.git",
                tempRepo.toString());
        project.getDocumentation().runIfNecessary();
        Path javadocTarget = tempRepo.resolve(tempRepo.resolve("docs/javadoc"));
        JkPathTree.of(javadocSourceDir).copyTo(javadocTarget, StandardCopyOption.REPLACE_EXISTING);
        makeDocs();
        JkPathTree.of(distribFolder().resolve("doc")).copyTo(tempRepo.resolve("docs"), StandardCopyOption.REPLACE_EXISTING);
        JkGitWrapper gitTemp = JkGitWrapper.of(tempRepo).withLogCommand(true);
        gitTemp.exec("add", "*");
        gitTemp.withFailOnError(false).exec("commit", "-am", "Doc");
        gitTemp.exec("push");
    }

    public void cleanPack() {
        clean(); javaPlugin.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).cleanPack();
    }

}
