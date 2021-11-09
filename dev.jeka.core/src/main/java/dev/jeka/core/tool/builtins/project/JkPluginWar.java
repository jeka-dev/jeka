package dev.jeka.core.tool.builtins.project;

import dev.jeka.core.api.depmanagement.artifact.JkStandardFileArtifactProducer;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.project.JkJavaProject;
import dev.jeka.core.api.project.JkJavaProjectConstruction;
import dev.jeka.core.api.project.JkJavaProjectPublication;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plugin for building WAR file (Jee Web Archive).
 */
@JkDoc("Basic plugin for building war file artifact (Java Web archive). \n" +
        "When initialized, it modifies the Java project hold by the Project plugin in order to \n" +
        "generate the web.xml file and build an extra artifact (war).")
@JkDocPluginDeps({JkPluginProject.class})
public class JkPluginWar extends JkPlugin {

    private Path staticResourceDir;

    private JkRunnables staticResourceComputation = JkRunnables.of();

    private final JkStandardFileArtifactProducer artifactProducer;

    public JkPluginWar(JkClass jkClass) {
        super(jkClass);
        this.staticResourceDir = jkClass.getBaseDir().resolve("src/main/webapp/static");
        this.artifactProducer = jkClass.getPlugin(JkPluginProject.class).getProject().getPublication().getArtifactProducer();

    }

    @Override
    protected void beforeSetup() {
        this.artifactProducer
            .removeArtifact(JkJavaProjectPublication.JAVADOC_ARTIFACT_ID)
            .removeArtifact(JkJavaProjectPublication.SOURCES_ARTIFACT_ID)
            .setMainArtifactExt("war")
            .putMainArtifact(path -> doWarFile((Path) path));
    }

    private static void generateWarDir(JkJavaProject project, Path dest, Path staticResourceDir) {
        JkJavaProjectConstruction construction = project.getConstruction();
        construction.getCompilation().runIfNecessary();
        JkPathTree root = JkPathTree.of(dest);
        JkPathTree webinf = JkPathTree.of(project.getBaseDir().resolve("src/main/webapp/WEB-INF"));
        if (!webinf.exists() || webinf.count(1, false) == 0) {
            JkLog.warn(webinf.getRoot().toString() + " is empty or does not exists.");
        } else {
            webinf.copyTo(root.get("WEB-INF"));
        }
        if (Files.exists(staticResourceDir)) {
            JkPathTree.of(staticResourceDir).copyTo(root.getRoot());
        }
        JkPathTree.of(construction.getCompilation().getLayout().resolveClassDir()).copyTo(root.get("WEB-INF/classes"));
        JkResolveResult resolveResult = construction.getDependencyResolver()
                .resolve(construction.getRuntimeDependencies());
        JkPathTree lib = root.goTo("lib");
        resolveResult.getFiles().withoutDuplicates().getEntries().forEach(path ->  lib.importFiles(path));
    }

    public void setStaticResourceDir(Path staticResourceDir) {
        this.staticResourceDir = staticResourceDir;
    }

    public JkRunnables getStaticResourceComputation() {
        return staticResourceComputation;
    }

    private void doWarFile(Path file) {
        JkPluginProject projectPlugin = this.getJkClass().getPlugin(JkPluginProject.class);
        JkJavaProject project = projectPlugin.getProject();
        staticResourceComputation.run();
        Path temp = JkUtilsPath.createTempDirectory("jeka-war");
        generateWarDir(project, temp, staticResourceDir);
        JkPathTree.of(temp).zipTo(file);
        JkPathTree.of(temp).deleteRoot();
    }

}
