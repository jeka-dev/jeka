import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

public class NodeJsBuild extends JkBean {

    private final ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).lately(this::configure);

    NodeJsBuild() {
        getBean(IntellijJkBean.class).jekaModuleName = "dev.jeka.core";
    }

    private void configure(JkProject project) {
        project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade()
                .mixResourcesAndSources()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .configureCompileDependencies(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );
        project.publication
                .setModuleId("dev.jeka:nodejs-plugin")
                .maven
                    .pomMetadata
                        .setProjectName("Jeka plugin for NodeJs")
                        .setProjectDescription("A Jeka plugin to integrate with NodeJs")
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        cleanOutput(); projectPlugin.pack();
    }

}
