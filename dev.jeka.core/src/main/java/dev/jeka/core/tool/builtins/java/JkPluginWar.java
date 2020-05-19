package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.api.depmanagement.JkResolveResult;
import dev.jeka.core.api.depmanagement.JkScope;
import dev.jeka.core.api.depmanagement.JkStandardFileArtifactProducer;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectPublication;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkDocPluginDeps;
import dev.jeka.core.tool.JkPlugin;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plugin for building WAR file (Jee Web Archive).
 */
@JkDoc("Basic plugin for building war file artifact (Java Web archive).")
@JkDocPluginDeps({JkPluginJava.class})
public class JkPluginWar extends JkPlugin {

    private Path staticResourceDir;

    private JkRunnables staticResourceComputation = JkRunnables.of();

    private final JkStandardFileArtifactProducer artifactProducer;

    public JkPluginWar(JkCommandSet commandSet) {
        super(commandSet);
        this.staticResourceDir = commandSet.getBaseDir().resolve("src/main/webapp/static");
        this.artifactProducer = commandSet.getPlugin(JkPluginJava.class).getProject().publication.getArtifactProducer();

    }

    @Override
    protected void init() {
        this.artifactProducer
            .removeArtifact(JkJavaProjectPublication.JAVADOC_ARTIFACT_ID)
            .removeArtifact(JkJavaProjectPublication.SOURCES_ARTIFACT_ID)
            .setMainArtifactExt("war")
            .putMainArtifact(path -> doWarFile((Path) path));
    }

    public static void generateWarDir(JkJavaProject project, Path dest, Path staticResourceDir) {
        project.getProduction().getCompilation().runIfNecessary();
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
        JkPathTree.of(project.getProduction().getCompilation().getLayout().resolveClassDir()).copyTo(root.get("WEB-INF/classes"));
        JkResolveResult resolveResult = project.getProduction().getDependencyManagement().fetchDependencies(JkScope.RUNTIME);
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
        JkPluginJava pluginJava = this.getCommandSet().getPlugin(JkPluginJava.class);
        JkJavaProject project = pluginJava.getProject();
        staticResourceComputation.run();
        Path temp = JkUtilsPath.createTempDirectory("jeka-war");
        generateWarDir(project, temp, staticResourceDir);
        JkPathTree.of(temp).zipTo(file);
        JkPathTree.of(temp).deleteRoot();
    }



}
