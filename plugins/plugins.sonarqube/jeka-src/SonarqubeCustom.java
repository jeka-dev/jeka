import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class SonarqubeCustom extends KBean {

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.flatFacade
                .setModuleId("dev.jeka:sonarqube-plugin")
                .setMixResourcesAndSources()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .dependencies.test
                    .add("org.junit.jupiter:junit-jupiter:5.13.3")
                    .add("org.junit.platform:junit-platform-launcher:1.13.3")
        ;
        project.flatFacade.dependencies.compile
                    .add(JkLocator.getJekaJarPath());
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.getPublication()
                .pomMetadata
                    .setProjectName("Jeka plugin for Sonarqube")
                    .setProjectDescription("A Jeka plugin for Jacoco coverage tool");
    }

}