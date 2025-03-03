import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

public class JacocoBuild extends KBean {

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("core", JkIml.Scope.COMPILE, null);
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade
                .setModuleId("dev.jeka:jacoco-plugin")
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .dependencies.compile.add(JkLocator.getJekaJarPath());
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.customizePublication(mavenPublication -> mavenPublication
                .pomMetadata
                    .setProjectName("Jeka plugin for Jacoco")
                    .setProjectDescription("A Jeka plugin for Jacoco coverage tool")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr"));
    }

}
