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
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPostInit;
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
import java.nio.file.StandardCopyOption;

/**
 * Defines build for <i>core</i> module..
 */
public class CoreCustom extends KBean {

    private static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final JkArtifactId SDKMAN_FILE_ID = JkArtifactId.of("sdkman", "zip");

    @JkDoc("If true, Integration tests, including some Docker tests will be included in junit tests.")
    public boolean runIT = true;

    @JkDoc("Creates and publishes Jeka Docker image on Dockerhub")
    public void publishJekaDockerImage() {
        DockerImageMaker.createImage();
        String version = load(ProjectKBean.class).project.getVersion().getValue();
        DockerImageMaker.pushImage(version, System.getenv("DOCKER_HUB_TOKEN"));
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;

        project.setJvmTargetVersion(JkJavaVersion.V8);
        project.setModuleId("dev.jeka:jeka-core");
        project.pack.actions
                    .append("include-embedded-jar", this::doPackWithEmbeddedJar)
                    .append("create-distrib", this::doDistrib)
                    .appendIf(!JkUtilsSystem.IS_WINDOWS, "create-sdkman-distrib", this::doSdkmanDistrib);
        project.compilerToolChain.setForkCompiler(true);

        project.compilation.addJavaCompilerOptions("-Xlint:none");
        project.compilation.layout.setMixResourcesAndSources();

        project.test.selection.addExcludePatterns(JkTestSelection.E2E_PATTERN);
        project.test.selection.addExcludePatternsIf(!runIT, JkTestSelection.IT_PATTERN);
        project.test.compilation.layout.setMixResourcesAndSources();

        project.pack.setMainClass("dev.jeka.core.tool.Main");
        project.pack.javadocProcessor.addOptions("-notimestamp");

        project.e2eTest.setupBasic();
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.getMavenPublication()
                .putArtifact(DISTRIB_FILE_ID)
                .putArtifactIf(!JkUtilsSystem.IS_WINDOWS, SDKMAN_FILE_ID)
                .pomMetadata
                    .setProjectName("JeKa")
                    .setProjectDescription("Build and Run Java Code from Everywhere");
    }

    // Call from GitHub actions
    @JkDoc("Publish javadoc on the Github page")
    public void addJavadocToGhPages() {
        String gitUrl = "https://github.com/jeka-dev/jeka.git";
        String ghPageBranch = "gh-pages";

        Path ghPageDir = load(ProjectKBean.class).project.getOutputDir().resolve("gh-pages");
        JkPathTree.of(ghPageDir).deleteContent();
        JkUtilsPath.createDirectories(ghPageDir);

        Path javadocPath = getOutputDir().resolve("javadoc");
        JkGit git = JkGit.of(ghPageDir).setFailOnError(true).setLogCommand(true);
        git.execCmdLine("clone --depth 1 --branch %s %s .", ghPageBranch, gitUrl);
        JkPathTree.of(ghPageDir.resolve("javadoc")).importDir(javadocPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void doDistrib() {
        JkProject project = load(ProjectKBean.class).project;
        Path distribFile = project.artifactLocator.getArtifactPath(DISTRIB_FILE_ID);
        project.pack.createSourceJar(); // Sources should be included in distrib

        final JkPathTree distrib = JkPathTree.of(distribFolder());
        distrib.deleteContent();

        distrib
            .importFiles(getBaseDir().toAbsolutePath().normalize().getParent().resolve("LICENSE"));
        Path binDir = distribFolder().resolve("bin");

        JkPathTree.of(binDir)
            .importDir(getBaseDir().resolve("src/main/shell"))
            .importFiles(project.artifactLocator.getArtifactPath(JkArtifactId.MAIN_JAR_ARTIFACT_ID))
            .importFiles(project.artifactLocator.getArtifactPath(JkArtifactId.SOURCES_ARTIFACT_ID));

        JkPathFile.of(binDir.resolve("jeka")).setPosixExecPermissions();
        JkPathFile.of(binDir.resolve("jeka-update")).setPosixExecPermissions();
        JkLog.info("Distribution created in " + distrib.getRoot());
        zipDistrib(distrib.getRoot(), distribFile);
        JkLog.info("Distribution zipped in " + distribFile);
    }

    // We create a specific archive for sdkman to conform to the constraints
    // that the zip must have a root entry having the same name than the archive.
    private void doSdkmanDistrib() {
        JkProject project = load(ProjectKBean.class).project;
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

    private Path distribFolder() {
        return load(ProjectKBean.class).project.getOutputDir().resolve("distrib");
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
        JkProject project = load(ProjectKBean.class).project;
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

}
