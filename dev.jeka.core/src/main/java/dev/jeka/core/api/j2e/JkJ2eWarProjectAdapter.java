package dev.jeka.core.api.j2e;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;

import java.nio.file.Path;
import java.util.function.Consumer;

public class JkJ2eWarProjectAdapter {

    private Path extraStaticResourcePath;

    private String webappPath;

    private boolean generateExploded;

    private JkJ2eWarProjectAdapter() {
    }

    public static JkJ2eWarProjectAdapter of() {
        return new JkJ2eWarProjectAdapter();
    }

    public JkJ2eWarProjectAdapter setExtraStaticResourcePath(Path extraStaticResourcePath) {
        this.extraStaticResourcePath = extraStaticResourcePath;
        return this;
    }

    public JkJ2eWarProjectAdapter setWebappPath(String webappPath) {
        this.webappPath = webappPath;
        return this;
    }


    public JkJ2eWarProjectAdapter setGenerateExploded(boolean generateExploded) {
        this.generateExploded = generateExploded;
        return this;
    }

    /**
     * Configures the specified project to produce and publish WAR archive.
     */
    public void configure(JkProject project) {
        JkArtifactId warArtifact = JkArtifactId.ofMainArtifact("war");
        Path warFile = project.artifactLocator.getArtifactPath(warArtifact);
        Consumer<Path> warMaker = path -> generateWar(project, path);
        project.setPackAction(() -> warMaker.accept(warFile));
        project.mavenPublication
                .removeArtifact(JkArtifactId.MAIN_JAR_ARTIFACT_ID)
                .putArtifact(warArtifact, warMaker);
    }

    public void generateWar(JkProject project, Path targetPath) {
        final Path effectiveWebappPath;
        if (webappPath != null) {
            effectiveWebappPath = project.getBaseDir().resolve(webappPath);
        } else {
            Path src =  project.compilation.layout.getSources().toList().get(0).getRoot();
            effectiveWebappPath = src.resolveSibling("webapp");
        }
        generateWar(project, targetPath, effectiveWebappPath, extraStaticResourcePath, generateExploded);
    }

    private static void generateWar(JkProject project, Path targetFile, Path webappPath, Path extraStaticResourcePath,
                                    boolean generateDir) {

        JkJ2eWarArchiver archiver = JkJ2eWarArchiver.of()
                .setClassDir(project.compilation.layout.resolveClassDir())
                .setExtraStaticResourceDir(extraStaticResourcePath)
                .setLibs(project.packaging.resolveRuntimeDependencies().getFiles().getEntries())
                .setWebappDir(webappPath);
        project.compilation.runIfNeeded();
        project.testing.runIfNeeded();
        if (generateDir) {
            Path dirPath = project.getOutputDir().resolve("j2e-war");
            archiver.generateWarDir(dirPath);
            JkPathTree.of(dirPath).zipTo(targetFile);
        } else {
            archiver.generateWarFile(targetFile);
        }
    }



}
