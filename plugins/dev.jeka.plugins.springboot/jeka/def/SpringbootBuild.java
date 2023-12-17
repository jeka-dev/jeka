import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

public class SpringbootBuild extends JkBean {

    final ProjectJkBean projectBean = load(ProjectJkBean.class).lazily(this::configure);

    final IntellijJkBean intellijJkBean = load(IntellijJkBean.class)
            .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");

    private void configure(JkProject project) {
       project
           .setJvmTargetVersion(JkJavaVersion.V8)
           .flatFacade()
               .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
               .configureCompileDependencies(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                )
                .configureRuntimeDependencies(deps -> deps
                        .minus(JkFileSystemDependency.of(JkLocator.getJekaJarPath()))
                );
       project.compilation.layout.setResources(JkPathTreeSet.ofRoots("resources"));
       project.testing.setSkipped(true);
       project.publication
            .setModuleId("dev.jeka:springboot-plugin")
            .maven
                .pomMetadata
                    .setProjectName("Jeka plugin for Spring Boot")
                    .setProjectDescription("A Jeka plugin for Spring boot application")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        cleanOutput(); projectBean.pack(); projectBean.publishLocal();
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