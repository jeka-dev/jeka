package dev.jeka.core;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactProducer;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.testing.JkTestProcessor;
import dev.jeka.core.api.java.testing.JkTestSelection;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkConstants;
import dev.jeka.core.tool.JkEnv;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;
import dev.jeka.core.tool.builtins.release.JkPluginVersionFromGit;
import dev.jeka.core.tool.builtins.repos.JkPluginGpg;
import dev.jeka.core.tool.builtins.repos.JkPluginNexus;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static dev.jeka.core.api.java.project.JkJavaProjectPublication.JAVADOC_ARTIFACT_ID;
import static dev.jeka.core.api.java.project.JkJavaProjectPublication.SOURCES_ARTIFACT_ID;

/**
 * Build class for Jeka. Run main method to create full distrib.
 * For publishing in OSSRH the following options must be set : -ossrhPwd=Xxxxxx -pgp#secretKeyPassword=Xxxxxxx
 */
public class CoreBuild extends JkClass {

    private static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final JkArtifactId WRAPPER_ARTIFACT_ID = JkArtifactId.of("wrapper", "jar");

    final JkPluginJava java = getPlugin(JkPluginJava.class);

    final JkPluginGpg gpg = getPlugin(JkPluginGpg.class);

    final JkPluginNexus nexus = getPlugin(JkPluginNexus.class);

    final JkPluginVersionFromGit versionFromGit = getPlugin(JkPluginVersionFromGit.class);

    public boolean runIT;

    @JkEnv("OSSRH_USER")
    public String ossrhUser;

    @JkEnv("OSSRH_PWD")
    public String ossrhPwd;

    @JkEnv("GH_TOKEN")
    public String githubToken;

    @Override
    protected void setup()  {
        if (!versionFromGit.version().isSnapshot()) {
            java.pack.javadoc = true;
        }
        java.getProject()
            .getConstruction()
                .getManifest()
                    .addMainClass("dev.jeka.core.tool.Main").__
                .getCompiler()
                    .setForkedWithDefaultProcess()
                .__
                .setJvmTargetVersion(JkJavaVersion.V8)
                .getCompilation()
                    .getLayout()
                        .mixResourcesAndSources()
                    .__
                    .addJavaCompilerOptions("-Xlint:none","-g")
                .__
                .getTesting()
                    .getCompilation()
                        .getLayout()
                            .mixResourcesAndSources()
                        .__
                    .__
                    .getTestProcessor()
                        .getEngineBehavior()
                            .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.BAR)
                        .__
                    .__
                    .getTestSelection()
                        .addIncludePatterns(JkTestSelection.STANDARD_INCLUDE_PATTERN)
                        .addIncludePatternsIf(runIT, JkTestSelection.IT_INCLUDE_PATTERN)
                    .__
                .__
            .__
            .getDocumentation()
                .getJavadocProcessor()
                    .setDisplayOutput(false)
                    .addOptions("-notimestamp")
                .__
            .__
            .getPublication()
                .getArtifactProducer()
                    .putMainArtifact(this::doPackWithEmbedded)
                    .putArtifact(DISTRIB_FILE_ID, this::doDistrib)
                    .putArtifact(WRAPPER_ARTIFACT_ID, this::doWrapper)
                .__
                .getMaven()
                    .setModuleId("dev.jeka:jeka-core")
                    .setRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd, gpg.get().getSigner("")))
                    .getPomMetadata()
                        .getProjectInfo()
                            .setName("jeka")
                            .setUrl("https://jeka.dev")
                            .setDescription("Automate with plain Java code and nothing else.")
                        .__
                        .getScm()
                            .setUrl("https://github.com/jerkar/jeka.git")
                        .__
                        .addApache2License()
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }



    private Path distribFolder() {
        return java.getProject().getOutputDir().resolve("distrib");
    }

    private void doDistrib(Path distribFile) {
        final JkArtifactProducer artifactProducer = java.getProject().getPublication().getArtifactProducer();
        artifactProducer.makeMissingArtifacts(artifactProducer.getMainArtifactId(),
                SOURCES_ARTIFACT_ID, WRAPPER_ARTIFACT_ID);
        final JkPathTree distrib = JkPathTree.of(distribFolder());
        distrib.deleteContent();
        JkLog.startTask("Create distrib");
        final List<Path> ivySourceLibs = getBaseTree().goTo("jeka/libs-sources")
                .andMatching(true, "ivy-*.jar").getFiles();
        distrib
            .importFiles(getBaseDir().toAbsolutePath().getParent().resolve("LICENSE"))
            .importDir(getBaseDir().resolve("src/main/dist"))
            .importDir(getBaseDir().resolve("src/main/java/META-INF/bin"))
            .importFiles(artifactProducer.getArtifactPath(artifactProducer.getMainArtifactId()))
            .importFiles(artifactProducer.getArtifactPath(WRAPPER_ARTIFACT_ID))
            .goTo("libs-sources")
                .importFiles(ivySourceLibs)
                .importFiles(artifactProducer.getArtifactPath(SOURCES_ARTIFACT_ID));
        if (java.pack.javadoc == null || java.pack.javadoc) {
            artifactProducer.makeMissingArtifacts(artifactProducer.getMainArtifactId(), JAVADOC_ARTIFACT_ID);
            distrib.importFiles(artifactProducer.getArtifactPath(JAVADOC_ARTIFACT_ID));
        }
        JkPathFile.of(distribFile.resolve("jeka")).addExecPerm();
        makeDocs();
        if (runIT) {
            testScaffolding();
        }
        JkLog.info("Distribution created in " + distrib.getRoot());
        //distrib.zipTo(distribFile);
        zipDistrib(distrib.getRoot(), distribFile);
        JkLog.info("Distribution zipped in " + distribFile);
        JkLog.endTask();
    }

    // see example here https://www.tabnine.com/code/java/methods/org.apache.commons.compress.archivers.zip.ZipArchiveEntry/setUnixMode
    private static void zipDistrib(Path distribDir, Path zipFile)  {
        try {
            ZipArchiveOutputStream out = new ZipArchiveOutputStream(zipFile);
            appendRecursively(distribDir, "", out);
            out.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void appendRecursively(final Path file, String relativeFile, final ZipArchiveOutputStream out) throws IOException {
        boolean isDirectory = Files.isDirectory(file);
        final ZipArchiveEntry entry = new ZipArchiveEntry(file, relativeFile);
        if (!isDirectory && Files.isExecutable(file)) {
            entry.setUnixMode(0777);  // necessary to mark it as executable inside the archive
        }
        boolean isRoot = isDirectory && relativeFile.isEmpty();
        if (!isRoot) {
            out.putArchiveEntry(entry);
            if (!isDirectory) {
                Files.copy(file, out);
            }
            out.closeArchiveEntry();
        }
        if (entry.isDirectory()) {
            String prefix = relativeFile.isEmpty() ? "" : relativeFile + "/";
            for (final String filename : file.toFile().list()) {
                appendRecursively(file.resolve(filename), prefix + filename, out);
            }
        }
    }

    private void makeDocs() {
        JkLog.startTask("Make documentation");
        String version = java.getProject().getPublication().getMaven().getVersion();
        new DocMaker(getBaseDir(), distribFolder(), version).assembleAllDoc();
        JkLog.endTask();
    }

    void testScaffolding()  {
        JkLog.startTask("Run scaffold tests");
        new ScaffoldTester().run();
        JkLog.endTask();
    }

    private void doPackWithEmbedded(Path targetJar) {

        // Main jar
        JkJavaProject project = java.getProject();
        project.getConstruction().createBinJar(targetJar);
        JkPathTree jarTree = JkPathTree.ofZip(targetJar);

        // Create an embedded jar containing all 3rd party libs + embedded part code in jeka project
        Path embeddedJar = project.getOutputDir().resolve("embedded.jar");
        JkPathTree classTree = JkPathTree.of(project.getConstruction().getCompilation().getLayout().resolveClassDir());
        Path providedLibs = getBaseDir().resolve(JkConstants.JEKA_DIR).resolve("libs/compile");
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

        // Copy embedded jar into temp folder and remove embedded part code from jeka classes
        jarTree.goTo("META-INF").importFile(embeddedJar, embeddedFinalName);
        Path embeddedNaneFile = jarTree.get("META-INF/jeka-embedded-name");
        JkUtilsPath.deleteIfExists(embeddedNaneFile);
        JkPathFile.of(embeddedNaneFile).write(embeddedFinalName.getBytes(Charset.forName("utf-8")));
        jarTree.andMatching( "**/embedded/**").deleteContent();
        jarTree.close();

        // Cleanup
        JkUtilsPath.deleteIfExists(embeddedJar);
    }

    private void doWrapper(Path wrapperJar) {
        java.getProject().getConstruction().getCompilation().runIfNecessary();
        JkPathTree.of(java.getProject().getConstruction().getCompilation().getLayout()
                .resolveClassDir()).andMatching("dev/jeka/core/wrapper/**").zipTo(wrapperJar);
    }

    public void publishDocsOnGithubPage() {
        clean();
        JkJavaProject project = java.getProject();
        Path javadocSourceDir = project.getDocumentation().getJavadocDir();
        Path tempRepo = getOutputDir().resolve("pagesGitRepo");
        String userPrefix = githubToken == null ? "" : githubToken + "@";
        versionFromGit.git().setLogCommand(false).exec("clone", "--depth=1", "https://"
                + userPrefix + "github.com/jerkar/jeka-dev-site.git", tempRepo.toString());
        project.getDocumentation().runIfNecessary();
        Path javadocTarget = tempRepo.resolve(tempRepo.resolve("docs/javadoc"));
        JkPathTree.of(javadocSourceDir).copyTo(javadocTarget, StandardCopyOption.REPLACE_EXISTING);
        makeDocs();
        JkPathTree.of(distribFolder().resolve("doc")).copyTo(tempRepo.resolve("docs"), StandardCopyOption.REPLACE_EXISTING);
        JkGitProcess gitTemp = JkGitProcess.of(tempRepo).setLogCommand(true).setFailOnError(true);
        gitTemp.exec("add", "*");
        gitTemp.setFailOnError(false).exec("commit", "-am", "Doc");
        gitTemp.exec("push");
    }

    public void cleanPack() {
        clean(); java.pack();
    }

    // This method has to be run in dev.jeka.core (this module root) working directory
    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).cleanPack();
    }

    public static class RunBuildAndIT {

        public static void main(String[] args) {
            CoreBuild coreBuild = JkInit.instanceOf(CoreBuild.class, args, "-runIT");
            coreBuild.java.pack();
        }
    }
}
