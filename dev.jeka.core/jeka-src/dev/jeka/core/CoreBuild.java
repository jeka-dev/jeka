/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.git.JkGit;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Run main method to create full distrib.
 * For publishing in OSSRH the following options must be set : -ossrhPwd=Xxxxxx -pgp#secretKeyPassword=Xxxxxxx
 */
public class CoreBuild extends KBean {

    private static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final JkArtifactId SDKMAN_FILE_ID = JkArtifactId.of("sdkman", "zip");

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

    public final JkProject project = load(ProjectKBean.class).project;

    public boolean runIT = false;

    @Override
    protected void init()  {

        project
            .setJvmTargetVersion(JkJavaVersion.V8)
            .setModuleId("dev.jeka:jeka-core")
            //.packActions.set(this::doPackWithEmbeddedJar, this::doDistrib);
            .packActions.append("include-embedded-jar", this::doPackWithEmbeddedJar)
                        .append("create-distrib", this::doDistrib)
                        .appendIf(!JkUtilsSystem.IS_WINDOWS, "create-sdkman-distrib", this::doSdkmanDistrib);
        project
            .compilation
                .addJavaCompilerOptions("-Xlint:none","-g")
                .layout
                    .mixResourcesAndSources();
        project.compilerToolChain.setForkCompiler(true);

        project
            .testing
                .compilation
                    .layout
                        .mixResourcesAndSources();
        project
            .testing
                .testSelection
                    .addExcludePatternsIf(!runIT, JkTestSelection.IT_INCLUDE_PATTERN);

        project
            .packaging
                .setMainClass("dev.jeka.core.tool.Main")
                .javadocProcessor
                    .setDisplayOutput(false)
                    .addOptions("-notimestamp");

        // Configure Maven publication
        load(MavenKBean.class).getMavenPublication()
                .putArtifact(DISTRIB_FILE_ID)
                .putArtifactIf(!JkUtilsSystem.IS_WINDOWS, SDKMAN_FILE_ID)
                .pomMetadata
                    .setProjectName("JeKa")
                    .addApache2License()
                    .setProjectDescription("Build and Run Java Code from Everywhere")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        project.clean().pack();
    }

    public void addJavadocToGhPages() {
        String gitUrl = "https://github.com/jeka-dev/jeka.git";
        String ghPageBranch = "gh-pages";

        Path ghPageDir = project.getOutputDir().resolve("gh-pages");
        JkPathTree.of(ghPageDir).deleteContent();
        JkUtilsPath.createDirectories(ghPageDir);

        Path javadocPath = getOutputDir().resolve("javadoc");
        JkGit git = JkGit.of(ghPageDir).setFailOnError(true).setLogCommand(true);
        git.execCmdLine("clone --depth 1 --branch %s %s .", ghPageBranch, gitUrl);
        JkPathTree.of(ghPageDir.resolve("javadoc")).importDir(javadocPath, StandardCopyOption.REPLACE_EXISTING);

        // commit-push will be done by a specific github actions
        /*
        git
                .execCmdLine("add .")
                .execCmdLine("config user.name  jeka-bot")
                .execCmdLine("config user.email jeka-bot@github-action.com")
                .execCmdLine("commit -m update-javadoc --allow-empty")
                .execCmdLine("push");
                */
    }

    void testScaffolding()  {
        JkLog.startTask("Run scaffold tests");
        new ScaffoldTester().run();
        JkLog.endTask();
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
            .importFiles(getBaseDir().toAbsolutePath().normalize().getParent().resolve("LICENSE"));
        Path binDir = distribFolder().resolve("bin");
        JkPathTree.of(binDir)
            .importDir(getBaseDir().resolve("src/main/shell"))
            .importFiles(project.artifactLocator.getArtifactPath(JkArtifactId.MAIN_JAR_ARTIFACT_ID))
            .importFiles(project.artifactLocator.getArtifactPath(JkArtifactId.SOURCES_ARTIFACT_ID));

        JkPathFile.of(binDir.resolve("jeka")).setPosixExecPermissions();
        JkPathFile.of(binDir.resolve("jeka-update")).setPosixExecPermissions();
        if (!project.testing.isSkipped() && runIT) {
            testScaffolding();
            new DockerTester().run();
            new ShellRemoteTester().run();

        }
        JkLog.info("Distribution created in " + distrib.getRoot());
        zipDistrib(distrib.getRoot(), distribFile);
        JkLog.info("Distribution zipped in " + distribFile);
        JkLog.endTask();
    }

    // We create a specific archive for sdkman to conform to the constraints
    // that the zip must have a root entry having the same name than the archive.
    private void doSdkmanDistrib() {
        final JkPathTree distrib = JkPathTree.of(distribFolder());
        String entryName = "jeka-core-" + project.getVersion() + "-sdkman";
        Path sdkmanDistribDir = getOutputDir().resolve(entryName);
        distrib.copyTo(sdkmanDistribDir);

        // Zipping with Java does npt preserve permissions.
        // We need to use native unix tool.
        Path zipFile = project.artifactLocator.getArtifactPath("sdkman", "zip");
        JkProcess.of("zip", "-r", zipFile.getFileName().toString(), entryName)
                .setWorkingDir(getOutputDir())
                .exec();
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

    private void doPackWithEmbeddedJar() {

        Path targetJar = project.artifactLocator.getMainArtifactPath();

        // Main jar
        //project.packaging.createBinJar(targetJar);
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

    public void print() {
        System.out.println("\uD83D\uDE800Booting JeKa...");
    }



}
