import dev.jeka.core.api.depmanagement.JkFileSystemDependency;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

public class SpringbootBuild extends KBean {

    final ProjectKBean projectBean = load(ProjectKBean.class);

    SpringbootBuild() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    @Override
    protected void init() {
        JkProject project = projectBean.project;
        project
                .setJvmTargetVersion(JkJavaVersion.V8)
                .flatFacade()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .customizeCompileDeps(deps -> deps
                        .andFiles(JkLocator.getJekaJarPath())
                )
                .customizeRuntimeDeps(deps -> deps
                        .minus(JkFileSystemDependency.of(JkLocator.getJekaJarPath()))
                );
        project.compilation.layout.setResources(JkPathTreeSet.ofRoots("resources"));
        project.testing.setSkipped(true);
        project.setModuleId("dev.jeka:springboot-plugin");

        load(MavenKBean.class).getMavenPublication()
                .pomMetadata
                    .setProjectName("Jeka plugin for Spring Boot")
                    .setProjectDescription("A Jeka plugin for Spring boot application")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    public void cleanPack() {
        cleanOutput();
        projectBean.pack();
        load(MavenKBean.class).publishLocal();
    }

}