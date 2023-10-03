import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

public class JacocoBuild extends JkBean {

    private final ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).lately(this::configure);

    final IntellijJkBean intellijJkBean = getBean(IntellijJkBean.class)
            .configureIml(jkIml -> {
                jkIml.component.replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
            });

    private void configure(JkProject project) {
        project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .configureCompileDependencies(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );
        project.publication
                .setModuleId("dev.jeka:jacoco-plugin")
                .maven
                    .pomMetadata
                        .setProjectName("Jeka plugin for Jacoco")
                        .setProjectDescription("A Jeka plugin for Jacoco coverage tool")
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        cleanOutput(); projectPlugin.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(JacocoBuild.class, args).cleanPack();
    }

}
