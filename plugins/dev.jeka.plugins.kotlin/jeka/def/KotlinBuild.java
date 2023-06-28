import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

class KotlinBuild extends JkBean {

    final ProjectJkBean projectBean = getBean(ProjectJkBean.class).configure(this::configure);

    KotlinBuild() {
        IntellijJkBean intellij = getBean(IntellijJkBean.class);
        intellij.jekaModuleName = "dev.jeka.core";
    }

    private void configure(JkProject project) {
        project.flatFacade()
            .setJvmTargetVersion(JkJavaVersion.V8)
            .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
            .mixResourcesAndSources()
            .configureCompileDependencies(deps -> deps
                    .andFiles(JkLocator.getJekaJarPath())
            );
        // This section is necessary to publish on a public repository
        project.publication
                .setModuleId("dev.jeka:kotlin-plugin")
                .maven
                    .pomMetadata
                    .setProjectName("Jeka plugin for Kotlin")
                    .setProjectDescription("A Jeka plugin for Kotlin language support")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        cleanOutput(); projectBean.pack();
    }

}