package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectConstruction;
import dev.jeka.core.api.project.JkProjectPublication;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Plugin for building WAR file (Jee Web Archive).
 */
@JkDoc("Basic plugin for building war file artifact (Java Web archive). \n" +
        "When initialized, it modifies the Java project hold by the Project plugin in order to \n" +
        "generate the web.xml file and build an extra artifact (war).")
@JkDocPluginDeps({JkPluginProject.class})
public class JkWarArchiver {

    private Path staticResourceDir;

    private JkRunnables staticResourceComputation = JkRunnables.of();

    private JkStandardFileArtifactProducer artifactProducer;

    public void init(JkProject project) {
        this.staticResourceDir = project.getBaseDir().resolve("src/main/webapp/static");
        this.artifactProducer = project.getPublication().getArtifactProducer();

    }


    protected void beforeSetup(JkProject project) {
        this.artifactProducer
            .removeArtifact(JkProjectPublication.JAVADOC_ARTIFACT_ID)
            .removeArtifact(JkProjectPublication.SOURCES_ARTIFACT_ID)
            .setMainArtifactExt("war")
            .putMainArtifact(path -> doWarFile(project, (Path) path));
    }



    private static void generateWarDir(Path destDir, Optional<Path> webInfSrcDir, Path classDir, List<Path> libs,
                                       Optional<Path> staticResourceDir) {
        JkPathTree webInf = webInfSrcDir.isPresent() ? JkPathTree.of(webInfSrcDir.get()) : null;
        if (webInf == null || !webInf.exists() || !webInf.containFiles()) {
            JkLog.warn(webInf.getRoot().toString() + " is empty or does not exists.");
        } else {
            webInf.copyTo(destDir.resolve("WEB-INF"));
        }
        if (staticResourceDir.isPresent() && Files.exists(staticResourceDir.get())) {
            JkPathTree.of(staticResourceDir.get()).copyTo(destDir);
        }
        JkPathTree.of(classDir).copyTo(destDir.resolve("WEB-INF/classes"));
        Path libDir = destDir.resolve("lib");
        JkPathTree.of(libDir).deleteContent();
        libs.forEach(path -> JkPathFile.of(path).copyToDir(libDir));
    }

    public void setStaticResourceDir(Path staticResourceDir) {
        this.staticResourceDir = staticResourceDir;
    }

    public JkRunnables getStaticResourceComputation() {
        return staticResourceComputation;
    }

    private void doWarFile(JkProject project, Path file) {
        staticResourceComputation.run();
        Path temp = JkUtilsPath.createTempDirectory("jeka-war");
        generateWarDir(project, temp, staticResourceDir);
        JkPathTree.of(temp).zipTo(file);
        JkPathTree.of(temp).deleteRoot();
    }

}
