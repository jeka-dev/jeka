import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

class ProtobufBuild extends KBean {

    private final ProjectKBean projectKBean = load(ProjectKBean.class);

    ProtobufBuild() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    @Override
    protected void init() {
        JkProject project = projectKBean.project;
        project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .configureCompileDependencies(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );
        project.publication
                .setModuleId("dev.jeka:protobuf-plugin")
                .maven.pomMetadata
                    .setProjectName("Jeka plugin for NodeJs")
                    .setProjectDescription("A Jeka plugin to integrate with NodeJs")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        projectKBean.cleanPack();
    }


}