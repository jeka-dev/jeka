import dev.jeka.core.api.file.JkPathTreeSet;
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

public class SpringbootBuild extends KBean {

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectBean) {
        JkProject project = projectBean.project;
        project.setJvmTargetVersion(JkJavaVersion.V8)
                .flatFacade.setLayoutStyle(JkCompileLayout.Style.SIMPLE);
        project.compilation.dependencies
                        .add(JkLocator.getJekaJarPath());
        project.packaging.runtimeDependencies
                        .remove(JkLocator.getJekaJarPath());
        project.compilation.layout.setResources(JkPathTreeSet.ofRoots("resources"));
        project.testing.setSkipped(true);
        project.setModuleId("dev.jeka:springboot-plugin");
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.customizePublication(mavenPublication -> mavenPublication
                .pomMetadata
                .setProjectName("Jeka plugin for Spring Boot")
                .setProjectDescription("A Jeka plugin for Spring boot application")
                .addGithubDeveloper("djeang", "djeangdev@yahoo.fr"));
    }

}