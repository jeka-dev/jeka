import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

public class SpringbootBuild extends JkBean {

    final ProjectJkBean projectBean = getRuntime().getBean(ProjectJkBean.class).configure(this::configure);

    private void configure(JkProject project) {
       project.simpleFacade()
                .setJvmTargetVersion(JkJavaVersion.V8)
                .configureCompileDeps(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                )
                .configureRuntimeDeps(deps -> deps
                        .minus(JkFileSystemDependency.of(JkLocator.getJekaJarPath()))
                );
        project.getConstruction().getTesting().setSkipped(true);
        project.getPublication()
            .setModuleId("dev.jeka:springboot-plugin")
            .getMaven()
                .getPomMetadata()
                    .setProjectName("Jeka plugin for Spring Boot")
                    .setProjectDescription("A Jeka plugin for Spring boot application")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        clean(); projectBean.pack(); projectBean.publishLocal();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(SpringbootBuild.class, args).cleanPack();
    }

    static class Iml {
        public static void main(String[] args) {
            JkInit.instanceOf(IntellijJkBean.class,args).iml();
        }
    }

}