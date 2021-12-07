import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

public class SpringbootBuild extends JkBean {

    final ProjectJkBean projectBean = getRuntime().getBean(ProjectJkBean.class);

    @Override
    protected void init() {
        projectBean.getProject().simpleFacade()
                .setJvmTargetVersion(JkJavaVersion.V8)
                .setCompileDependencies(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                )
                .setRuntimeDependencies(deps -> deps
                        .minus(JkFileSystemDependency.of(JkLocator.getJekaJarPath()))
                );
        projectBean.getProject().getPublication()
            .getMaven()
                .setModuleId("dev.jeka:springboot-plugin")
                .getPomMetadata()
                    .setProjectName("Jeka plugin for Spring Boot")
                    .setProjectDescription("A Jeka plugin for Spring boot application")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        clean(); projectBean.pack();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(SpringbootBuild.class, args).cleanPack();
    }

}