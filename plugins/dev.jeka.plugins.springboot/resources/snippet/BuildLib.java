import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.plugins.springboot.JkSpringbootProject;

@JkInjectClasspath("dev.jeka:springboot-plugin")
class Build extends KBean {

    ProjectKBean projectKBean = load(ProjectKBean.class);

    @Override
    protected void init() {
        JkProject project = projectKBean.project;
        JkSpringbootProject.of(project)
                .configure()
                .includeParentBom("${springbootVersion}");
        project.flatFacade()
                .addCompileDeps(
                        "org.springframework.boot:spring-boot-starter-web"
                )
                .addCompileOnlyDeps(
                        "org.projectlombok:lombok:1.18.30"
                )
                .addTestDeps(
                        "org.springframework.boot:spring-boot-starter-test"
                )
                .setVersionFromGitTag();  // Infer version from Git
    }

}
