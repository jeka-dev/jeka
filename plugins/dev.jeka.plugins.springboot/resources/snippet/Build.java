import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.plugins.springboot.SpringbootJkBean;

@JkInjectClasspath("${dependencyDescription}")
class Build extends JkBean {

    final SpringbootJkBean springbootBean = getBean(SpringbootJkBean.class);

    Build() {
        springbootBean.setSpringbootVersion("${springbootVersion}");
        getBean(ProjectJkBean.class).lately(this::configure);
    }

    private void configure(JkProject project) {
        project.flatFacade()
            .configureCompileDependencies(deps -> deps
                    .and("org.springframework.boot:spring-boot-starter-web")
                    // Add dependencies here
            )
            .configureTestDependencies(deps -> deps
                    .and("org.springframework.boot:spring-boot-starter-test")
            );
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanPack() {
        cleanOutput(); springbootBean.projectBean.pack();
    }

}
