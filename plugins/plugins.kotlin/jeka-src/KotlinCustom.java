import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class KotlinCustom extends KBean {

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("core", JkIml.Scope.COMPILE, null);
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.flatFacade
                .setModuleId("dev.jeka:kotlin-plugin")
                .setJvmTargetVersion(JkJavaVersion.V8)
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .setMixResourcesAndSources()
                .dependencies.compile
                .add(JkLocator.getJekaJarPath());
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.customizePublication(mavenPublication -> mavenPublication
                .pomMetadata
                .setProjectName("Jeka plugin for Kotlin")
                .setProjectDescription("A Jeka plugin for Kotlin language support")
                .addGithubDeveloper("djeang", "djeangdev@yahoo.fr"));
    }
}