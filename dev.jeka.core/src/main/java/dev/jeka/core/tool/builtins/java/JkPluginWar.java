package dev.jeka.core.tool.builtins.java;

import dev.jeka.core.api.depmanagement.JkArtifactId;
import dev.jeka.core.api.depmanagement.JkJavaDepScopes;
import dev.jeka.core.api.depmanagement.JkResolveResult;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectMaker;
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

    private static final JkArtifactId WAR_ARTIFACT_ID = JkArtifactId.of(null, "war");

    private Path staticResourceDir;

    private JkRunnables staticResourceComputation = JkRunnables.noOp();

    private final JkJavaProjectMaker maker;

    public JkPluginWar(JkCommandSet run) {
        super(run);
        this.staticResourceDir = run.getBaseDir().resolve("src/main/webapp/static");
        this.maker = run.getPlugin(JkPluginJava.class).getProject().getMaker();
    }

    @JkDoc("Add a war file to the generated artifacts.")
    @Override  
    protected void activate() {
        JkPluginJava pluginJava = this.getCommandSet().getPlugin(JkPluginJava.class);
        JkJavaProject project = pluginJava.getProject();
        JkArtifactId warArtifactId = JkArtifactId.of(null, "war");
        maker.putArtifact(warArtifactId, () -> {
            staticResourceComputation.run();
            Path temp = JkUtilsPath.createTempDirectory("jeka-war");
            generateWarDir(project, temp, staticResourceDir);
            JkPathTree.of(temp).zipTo(maker.getArtifactPath(warArtifactId));
            JkPathTree.of(temp).deleteRoot();
        });
    }

    public static void generateWarDir(JkJavaProject project, Path dest, Path staticResouceDir) {
        project.getMaker().getTasksForCompilation().runIfNecessary();
        JkPathTree root = JkPathTree.of(dest);
        JkPathTree.of(project.getBaseDir().resolve("src/main/webapp/WEB-INF")).copyTo(root.get("WEB-INF"));
        if (Files.exists(staticResouceDir)) {
            JkPathTree.of(staticResouceDir).copyTo(root.getRoot());
        }
        JkPathTree.of(project.getMaker().getOutLayout().getClassDir()).copyTo(root.get("classes"));
        JkResolveResult resolveResult = project.getMaker().getDependencyResolver().resolve(project.getDependencies(),
                JkJavaDepScopes.RUNTIME);
        JkPathTree lib = root.goTo("lib");
        resolveResult.getFiles().withoutDuplicates().getEntries().forEach(path ->  lib.importFiles(path));
    }

    public void setStaticResourceDir(Path staticResourceDir) {
        this.staticResourceDir = staticResourceDir;
    }

    public JkRunnables getStaticResouceComputation() {
        return staticResourceComputation;
    }

    public Path getWarFile() {
        return maker.getArtifactPath(WAR_ARTIFACT_ID);
    }
}
