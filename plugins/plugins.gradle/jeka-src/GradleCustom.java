import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIntellijIml;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class GradleCustom extends KBean {

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("core", JkIntellijIml.Scope.COMPILE, null);
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.flatFacade
                .setModuleId("dev.jeka:gradle-plugin")
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .dependencies.compile.add(JkLocator.getJekaJarPath());
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.getPublication()
                .pomMetadata
                    .setProjectName("Jeka plugin for Gradle")
                    .setProjectDescription("A Jeka plugin for running Gradle builds from JeKa");
    }

}
