import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

public class JacocoBuild extends KBean {

    private final ProjectKBean projectKBean = load(ProjectKBean.class);

    JacocoBuild() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    @Override
    protected void init() {
        JkProject project = projectKBean.project;
        project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade()
                .setModuleId("dev.jeka:jacoco-plugin")
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .customizeCompileDeps(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );
        load(MavenKBean.class).getMavenPublication()
                    .pomMetadata
                        .setProjectName("Jeka plugin for Jacoco")
                        .setProjectDescription("A Jeka plugin for Jacoco coverage tool")
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        cleanOutput(); projectKBean.pack();
    }

    public static void main(String[] args) {
        JacocoBuild jacocoBuild = JkInit.instanceOf(JacocoBuild.class, args);
        jacocoBuild.cleanPack();
        //jacocoBuild.projectKBean.publishLocal();
    }

}
