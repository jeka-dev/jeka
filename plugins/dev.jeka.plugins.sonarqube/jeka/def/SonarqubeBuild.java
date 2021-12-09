import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

class SonarqubeBuild extends JkBean {

    private final ProjectJkBean projectPlugin = getRuntime().getBean(ProjectJkBean.class);

    @Override
    protected void init() {
        projectPlugin.getProject().simpleFacade()
                .setJvmTargetVersion(JkJavaVersion.V8)
                .mixResourcesAndSources()
                .useSimpleLayout()
                .configureCompileDeps(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );

        projectPlugin.getProject().getPublication()
                .setModuleId("dev.jeka:sonarqube-plugin")
                .getMaven()
                    .getPomMetadata()
                        .setProjectName("Jeka plugin for Sonarqube")
                        .setProjectDescription("A Jeka plugin for Jacoco coverage tool")
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        clean(); projectPlugin.pack();
    }


}