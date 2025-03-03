import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.plugins.springboot.JkSpringbootProject;

@JkDep("${dependencyDescription}")
class Build extends KBean {

    JkProject project = load(ProjectKBean.class).project;

    @Override
    protected void init() {
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
                );
    }

}
