package dev.jeka.core;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Run main method to create full distrib.
 * For publishing in OSSRH the following options must be set : -ossrhPwd=Xxxxxx -pgp#secretKeyPassword=Xxxxxxx
 */
public class CoreBuild extends KBean {

    private static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    public final JkProject project = load(ProjectKBean.class).project;

    public boolean runIT;

    @Override
    protected void init()  {

        project
            .setJvmTargetVersion(JkJavaVersion.V8)
            .setModuleId("dev.jeka:jeka-core")
            .packActions.set(this::doPackWithEmbeddedJar, this::doDistrib);
        project
            .compilerToolChain
                .setForkedWithDefaultProcess();
        project
            .compilation
                .addJavaCompilerOptions("-Xlint:none","-g")
                .layout
                    .mixResourcesAndSources();
        project
            .testing
                .compilation
                    .layout
                        .mixResourcesAndSources();
        project
            .testing
                .testSelection
                    .addIncludePatterns(JkTestSelection.STANDARD_INCLUDE_PATTERN)
                    .addIncludePatternsIf(runIT, JkTestSelection.IT_INCLUDE_PATTERN);
        project
            .testing
                .testProcessor
                    .engineBehavior
                        .setProgressDisplayer(JkTestProcessor.JkProgressOutputStyle.BAR);
        project
            .packaging
                .setMainClass("dev.jeka.core.tool.Main")
                .javadocProcessor
                    .setDisplayOutput(false)
                    .addOptions("-notimestamp");

        // Configure Maven publication
        load(MavenKBean.class).getMavenPublication()
                .putArtifact(DISTRIB_FILE_ID)
                .pomMetadata
                    .setProjectName("jeka")
                    .addApache2License()
                    .setProjectDescription("Automate with plain Java code and nothing else.")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    private Path distribFolder() {
        return project.getOutputDir().resolve("distrib");
    }

    private void doDistrib() {
        Path distribFile = project.artifactLocator.getArtifactPath(DISTRIB_FILE_ID);
        project.packaging.createSourceJar(); // Sources should be included in distrib

        final JkPathTree distrib = JkPathTree.of(distribFolder());
        distrib.deleteContent();
        JkLog.startTask("Create distrib");
        distrib
            .importFiles(getBaseDir().toAbsolutePath().normalize().getParent().resolve("LICENSE"))
            .importDir(getBaseDir().resolve("src/main/shell"))
            .importFiles(project.artifactLocator.getArtifactPath(JkArtifactId.MAIN_JAR_ARTIFACT_ID))
            .importFiles(project.artifactLocator.getArtifactPath(JkArtifactId.SOURCES_ARTIFACT_ID));

        JkPathFile.of(distrib.get("jeka")).setPosixExecPermissions();
        JkPathFile.of(distrib.get("jekau")).setPosixExecPermissions();
        if (!project.testing.isSkipped() && runIT) {
            testScaffolding();
            new ShellRemoteTester().run();
        }
        JkLog.info("Distribution created in " + distrib.getRoot());
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

    void testScaffolding()  {
        JkLog.startTask("Run scaffold tests");
        new ScaffoldTester().run();
        JkLog.endTask();
    }

    private void doPackWithEmbeddedJar() {

        Path targetJar = project.artifactLocator.getMainArtifactPath();

        // Main jar
        project.packaging.createBinJar(targetJar);
        JkZipTree jarTree = JkZipTree.of(targetJar);

        // Create an embedded jar containing all 3rd party libs + embedded part code in jeka project
        Path embeddedJar = project.getOutputDir().resolve("embedded.jar");
        JkPathTree classTree = JkPathTree.of(project.compilation.layout.resolveClassDir());
        JkPathTreeSet.of(classTree.andMatching("**/embedded/**/*")).zipTo(embeddedJar);
        JkZipTree.of(embeddedJar).andMatching( "META-INF/*.SF", "META-INF/*.RSA").deleteContent().close();

        // Name uniquely this embedded jar according its content
        String checksum = JkPathFile.of(embeddedJar).getChecksum("MD5");
        String embeddedFinalName = "jeka-embedded-" + checksum + ".jar";

        // Copy embedded jar into temp folder and remove embedded part code from jeka classes
        jarTree.goTo("META-INF").importFile(embeddedJar, embeddedFinalName);
        Path embeddedNaneFile = jarTree.get("META-INF/jeka-embedded-name");
        JkUtilsPath.deleteIfExists(embeddedNaneFile);
        JkPathFile.of(embeddedNaneFile).write(embeddedFinalName.getBytes(StandardCharsets.UTF_8));
        jarTree.andMatching( "**/embedded/**").deleteContent();
        jarTree.close();

        // Cleanup
        JkUtilsPath.deleteIfExists(embeddedJar);
    }

    public void cleanPack() {
        project.clean().pack();
    }

    // This method has to be run in dev.jeka.core (this module root) working directory
    public static void main(String[] args) {
        JkInit.kbean(CoreBuild.class, args).cleanPack();
    }

    public static class RunBuildAndIT {
        public static void main(String[] args) {
            CoreBuild coreBuild = JkInit.kbean(CoreBuild.class, args, "-runIT");
            coreBuild.project.pack();
        }
    }

}
