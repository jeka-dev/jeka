import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.plugins.springboot.SpringbootJkBean;

@JkInjectClasspath("${dependencyDescription}")
class Build extends JkBean {

    private final SpringbootJkBean springboot = getRuntime().getBean(SpringbootJkBean.class);

    @Override
    protected void init() {
        springboot.setSpringbootVersion("${springbootVersion}");
        springboot.projectBean().getProject().simpleFacade()
            .configureCompileDeps(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-web")
            )
            .configureTestDeps(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-test")
                    .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
            );
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanPack() {
        clean(); springboot.projectBean().pack();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(Build.class, args).cleanPack();
    }

}
