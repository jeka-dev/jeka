import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class ProtobufBuild extends KBean {

    private final ProjectKBean projectKBean = load(ProjectKBean.class);

    ProtobufBuild() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);
    }

    @Override
    protected void init() {
        JkProject project = projectKBean.project;
        project.setJvmTargetVersion(JkJavaVersion.V8).flatFacade
                .setModuleId("dev.jeka:protobuf-plugin")
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .dependencies.compile.add(JkLocator.getJekaJarPath());
        load(MavenKBean.class).getMavenPublication()
                .pomMetadata
                    .setProjectName("Jeka plugin for NodeJs")
                    .setProjectDescription("A Jeka plugin to integrate with NodeJs")
                    .addGithubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

}