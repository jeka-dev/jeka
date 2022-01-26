import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.plugins.springboot.SpringbootJkBean;

@JkInjectClasspath("${dependencyDescription}")
class Build extends JkBean {

    final SpringbootJkBean springbootBean = getBean(SpringbootJkBean.class);

    Build() {
        springbootBean.setSpringbootVersion("${springbootVersion}");
        getBean(ProjectJkBean.class).configure(this::configure);
    }

    private void configure(JkProject project) {
        project.simpleFacade()
            .configureCompileDeps(deps -> deps
                    .and("org.springframework.boot:spring-boot-starter-web")
                    // Add dependencies here
            )
            .configureTestDeps(deps -> deps
                    .and("org.springframework.boot:spring-boot-starter-test")
                        .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
            );
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanPack() {
        clean(); springbootBean.projectBean().pack();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(Build.class, args).cleanPack();
    }

}
