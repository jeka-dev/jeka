package dev.jeka.core;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectMaker;
import dev.jeka.core.api.system.JkLocator;
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
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static dev.jeka.core.api.java.project.JkJavaProjectMaker.JAVADOC_ARTIFACT_ID;
import static dev.jeka.core.api.java.project.JkJavaProjectMaker.SOURCES_ARTIFACT_ID;

/**
 * Build class for Jerkar. Run main method to create full distrib.
 * For publishing in OSSRH the following options must be set : -ossrhPwd=Xxxxxx -pgp#secretKeyPassword=Xxxxxxx
 */
public class CoreBuild extends JkCommands {

    private static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final JkArtifactId WRAPPER_ARTIFACT_ID = JkArtifactId.of("wrapper", "jar");

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
        project.getCompileSpec().addOptions("-Xlint:none");
        maker.getTasksForCompilation().setFork(true);  // Fork to avoid compile failure bug on github/travis
                maker.putArtifact(DISTRIB_FILE_ID, this::doDistrib);
        this.distribFolder = maker.getOutLayout().getOutputPath().resolve("distrib");
        maker.getTasksForJavadoc().setJavadocOptions("-notimestamp");
        maker.getTasksForPublishing()
                .setPublishRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd))
                .setMavenPublicationInfo(mavenPublication());

        // include embedded jar
        maker.putArtifact(maker.getMainArtifactId(), this::doPackWithEmbedded);

        // define wrapper
        maker.putArtifact(WRAPPER_ARTIFACT_ID, this::doWrapper);
    }

    public void publishDocsOnGithubPage() {
        JkJavaProject project = javaPlugin.getProject();
        Path javadocSourceDir = project.getMaker().getOutLayout().getJavadocDir();
        Path tempRepo = getOutputDir().resolve("pagesGitRepo");
        String userPrefix = githubToken == null ? "" : githubToken + "@";
        git.exec("clone", "--depth=1", "https://" + userPrefix + "github.com/jerkar/jeka-dev-site.git",
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
        maker.makeMissingArtifacts(maker.getMainArtifactId(), SOURCES_ARTIFACT_ID, WRAPPER_ARTIFACT_ID);
        final JkPathTree distrib = JkPathTree.of(distribFolder);
        distrib.deleteContent();
        JkLog.startTask("Create distrib");
        distrib.importFiles(getBaseDir().getParent().resolve("LICENSE"));
        distrib.importDir(getBaseDir().resolve("src/main/dist"));
        distrib.importDir(getBaseDir().resolve("src/main/java/META-INF/bin"));
        distrib.importFiles(maker.getArtifactPath(maker.getMainArtifactId()));
        distrib.importFiles(maker.getArtifactPath(WRAPPER_ARTIFACT_ID));
        final List<Path> ivySourceLibs = getBaseTree().goTo("build/libs-sources")
                .andMatching(true, "apache-ivy*.jar").getFiles();
        distrib.goTo("libs-sources")
            .importFiles(ivySourceLibs)
            .importFiles(maker.getArtifactPath(SOURCES_ARTIFACT_ID));
        if (javaPlugin.pack.javadoc) {
            maker.makeMissingArtifacts(maker.getMainArtifactId(), JAVADOC_ARTIFACT_ID);
            distrib.goTo("libs-javadoc").importFiles(maker.getArtifactPath(JAVADOC_ARTIFACT_ID));
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
        JkLog.endTask("It tests done");
    }

    private void doPackWithEmbedded() {
        JkLog.startTask("Creating main jar");
        JkJavaProjectMaker maker = javaPlugin.getProject().getMaker();

        // Main jar
        Path targetJar = maker.getMainArtifactPath();
        maker.getTasksForPackaging().createBinJar(targetJar);
        JkPathTree jarTree = JkPathTree.ofZip(targetJar);


        // Create an embedded jar containing all 3rd party libs + embedded part code in jeka project
        Path embeddedJar = maker.getOutLayout().getOutputPath().resolve("embedded.jar");
        JkPathTree classTree = JkPathTree.of(maker.getOutLayout().getClassDir());
        Path providedLibs = getBaseDir().resolve(JkConstants.JEKA_DIR).resolve("libs/provided");
        JkPathTreeSet.of(classTree.andMatching("**/embedded/**/*"))
                .andZips(providedLibs.resolve("bouncycastle-pgp-152.jar"))
                .andZip(providedLibs.resolve("classgraph-4.8.41.jar"))
                .andZip(providedLibs.resolve("ivy-2.4.0.jar"))
                .zipTo(embeddedJar);
        JkPathTree.ofZip(embeddedJar).andMatching( "META-INF/*.SF", "META-INF/*.RSA")
                .deleteContent().close();

        // Name uniquely this embedded jar according its content
        String checksum = JkPathFile.of(embeddedJar).getChecksum("MD5");
        String embeddedFinalName = "jeka-embedded-" + checksum + ".jar";

        // Copy embbeded jar into temp folder and remove embedded part code from jeka classes
        jarTree.goTo("META-INF").importFile(embeddedJar, embeddedFinalName);
        JkPathFile.of(jarTree.get("META-INF/jeka-embedded-name"))
                .write(embeddedFinalName.getBytes(Charset.forName("utf-8")), StandardOpenOption.TRUNCATE_EXISTING);
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

    private void copyToWrapper() {
        Path target = javaPlugin.getProject().getMaker().getMainArtifactPath();
        Path wrapper =  JkLocator.getJekaRepositoryCache().getParent().resolve("wrapper/0.8.2.RELEASE").resolve(target.getFileName());
        JkUtilsPath.copy(target, wrapper, StandardCopyOption.REPLACE_EXISTING);
    }


    public static void main(String[] args) {
        CoreBuild coreBuild = JkInit.instanceOf(CoreBuild.class, args);
        coreBuild.javaPlugin.clean().pack();
        //coreBuild.copyToWrapper();
    }

}
