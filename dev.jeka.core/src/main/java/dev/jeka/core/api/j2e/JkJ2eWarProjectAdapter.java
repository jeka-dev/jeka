package dev.jeka.core.api.j2e;

import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectPublication;
import dev.jeka.core.api.utils.JkUtilsAssert;

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
     * Configures a project in order it publishes war archive.
     * It impacts {@link JkProjectPublication#pack()} as jar archive may be created or not.
     * @param publishedAsMainArtifact if true, war will be published as the main artifact, so without any dependencies.
     * @param keepJar if false, no jar archive will be created/deployed.
     */
    public void configure(JkProject project, boolean publishedAsMainArtifact, boolean keepJar) {
        JkUtilsAssert.argument(publishedAsMainArtifact || keepJar,
                "Both publishedAsMainArtifact and keepJar cannot be false.");
        Path staticResourceDir = project.getBaseDir().resolve("src/main/webapp/static");
        JkStandardFileArtifactProducer artifactProducer = project.getArtifactProducer();
        Consumer<Path> originalJarMaker = path -> artifactProducer.makeMainArtifact();
        Consumer<Path> warMaker = path -> generateWar(path, project);
        if (publishedAsMainArtifact) {
            JkArtifactId originalMainArtifactId = artifactProducer.getMainArtifactId();
            artifactProducer
                    .setMainArtifactExt("war")
                    .putMainArtifact(warMaker);
            if (!keepJar) {
                artifactProducer
                        .removeArtifact(originalMainArtifactId);
            }
        } else {
            artifactProducer
                    .putArtifact(JkArtifactId.of("", ".war"), warMaker);
        }
    }

    public void configure(JkProject project) {
        configure(project, true, false);
    }

    public void generateWar(Path dist, JkProject project) {
        final Path effectiveWebappPath;
        if (webappPath != null) {
            effectiveWebappPath = project.getBaseDir().resolve(webappPath);
        } else {
            Path src =  project.getConstruction().getCompilation().getLayout().getSources().toList().get(0).getRoot();
            effectiveWebappPath = src.resolveSibling("webapp");
        }
        generateWar(project, dist, effectiveWebappPath, extraStaticResourcePath, generateExploded);
    }

    private static void generateWar(JkProject project, Path destFile, Path webappPath, Path extraStaticResourcePath,
                                    boolean generateDir) {

        JkJ2eWarArchiver archiver = JkJ2eWarArchiver.of()
                .setClassDir(project.getConstruction().getCompilation().getLayout().resolveClassDir())
                .setExtraStaticResourceDir(extraStaticResourcePath)
                .setLibs(project.getConstruction().resolveRuntimeDependencies().getFiles().getEntries())
                .setWebappDir(webappPath);
        project.getConstruction().getCompilation().runIfNeeded();
        project.getConstruction().getTesting().runIfNeeded();
        if (generateDir) {
            Path dirPath = project.getOutputDir().resolve("j2e-war");
            archiver.generateWarDir(dirPath);
            JkPathTree.of(dirPath).zipTo(destFile);
        } else {
            archiver.generateWarFile(destFile);
        }
    }



}
