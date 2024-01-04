import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

class SonarqubeBuild extends KBean {

    private final ProjectKBean projectKBean = load(ProjectKBean.class);

    SonarqubeBuild() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    protected void init() {
        JkProject project = projectKBean.project;
        project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade()
                .setModuleId("dev.jeka:sonarqube-plugin")
                .mixResourcesAndSources()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .configureCompileDependencies(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                );
        project.mavenPublication
                    .pomMetadata
                        .setProjectName("Jeka plugin for Sonarqube")
                        .setProjectDescription("A Jeka plugin for Jacoco coverage tool")
                        .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        projectKBean.cleanPack();
    }

    public static void main(String[] args) {
        SonarqubeBuild build = JkInit.instanceOf(SonarqubeBuild.class);
        build.cleanPack();
        build.projectKBean.publishLocal();
    }


}