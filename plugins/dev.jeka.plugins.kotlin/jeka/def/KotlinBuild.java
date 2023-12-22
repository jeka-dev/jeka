import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

class KotlinBuild extends KBean {

    final ProjectKBean projectKBean = load(ProjectKBean.class);

    KotlinBuild() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    protected void init() {
        JkProject project = projectKBean.project;
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
        cleanOutput(); projectKBean.pack();
    }

}