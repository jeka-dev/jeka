import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.plugins.springboot.SpringbootJkBean;

@JkInjectClasspath("${dependencyDescription}")
class Build extends JkBean {

    final SpringbootJkBean springbootKBean = getBean(SpringbootJkBean.class);

    Build() {
        springbootKBean.setSpringbootVersion("${springbootVersion}");
        springbootKBean.projectBean.lately(this::configure);
    }

    private void configure(JkProject project) {
        project.flatFacade()
            .addCompileDeps(
                    "org.springframework.boot:spring-boot-starter-web"
            )
            .addTestDeps(
                    "org.springframework.boot:spring-boot-starter-test"
            );
    }

}
