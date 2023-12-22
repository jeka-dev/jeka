import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.plugins.springboot.JkSpringbootProject;

@JkInjectClasspath("${dependencyDescription}")
class Build extends KBean {

    final ProjectKBean projectKBean = load(ProjectKBean.class);

    protected void init() {
        JkProject project = projectKBean.project;
        JJkSpringbootProject.of(project)
                .setSpringbootVersion("${springbootVersion}")
                .configure();
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
                .setPublishedVersionFromGitTag();  // Infer version from Git
    }

}
