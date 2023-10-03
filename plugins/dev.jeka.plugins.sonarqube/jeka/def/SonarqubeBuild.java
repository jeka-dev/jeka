import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

class SonarqubeBuild extends JkBean {

    private final ProjectJkBean projectPlugin = getBean(ProjectJkBean.class).lately(this::configure);

    final IntellijJkBean intellijJkBean = getBean(IntellijJkBean.class)
            .configureIml(jkIml -> {
                jkIml.component.replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
            });
    private void configure(JkProject project) {
        project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade()
                .mixResourcesAndSources()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .configureCompileDependencies(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );
        project.publication
                .setModuleId("dev.jeka:sonarqube-plugin")
                .maven
                    .pomMetadata
                        .setProjectName("Jeka plugin for Sonarqube")
                        .setProjectDescription("A Jeka plugin for Jacoco coverage tool")
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        cleanOutput(); projectPlugin.pack();
    }


}