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

public class SpringbootCustom extends KBean {

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("core", JkIml.Scope.COMPILE, null);
    }

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectBean) {
        JkProject project = projectBean.project;
        project.setModuleId("dev.jeka:springboot-plugin");
        project.setJvmTargetVersion(JkJavaVersion.V8);
        project.flatFacade.setLayoutStyle(JkCompileLayout.Style.SIMPLE);
        project.flatFacade.dependencies.compile.add(JkLocator.getJekaJarPath());
        project.flatFacade.dependencies.runtime.add(JkLocator.getJekaJarPath());
        project.flatFacade.dependencies.test
                .add("org.junit.platform:junit-platform-launcher:1.12.0")
                .add("org.junit.jupiter:junit-jupiter:5.12.0");
        project.e2eTest.setupBasic();
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        mavenKBean.getMavenPublication()
                .pomMetadata
                    .setProjectName("Jeka plugin for Spring Boot")
                    .setProjectDescription("A Jeka plugin for Spring boot application");
    }

}