import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

public class NodeJsBuild extends JkBean {

    private final ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).configure(this::configure);

    private void configure(JkProject project) {
        project.simpleFacade()
                .setJvmTargetVersion(JkJavaVersion.V8)
                .mixResourcesAndSources()
                .useSimpleLayout()
                .configureCompileDeps(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );
        project.getPublication()
                .setGroupAndName("dev.jeka:nodejs-plugin")
                .getMaven()
                    .getPomMetadata()
                        .setProjectName("Jeka plugin for NodeJs")
                        .setProjectDescription("A Jeka plugin to integrate with NodeJs")
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        cleanOutput(); projectPlugin.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(NodeJsBuild.class).cleanPack();
    }


}
