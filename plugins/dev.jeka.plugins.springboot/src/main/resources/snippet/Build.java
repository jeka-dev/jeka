import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.plugins.springboot.SpringbootJkBean;

@JkDefClasspath("${dependencyDescription}")
class Build extends JkClass {

    private final SpringbootJkBean springboot = getJkBean(SpringbootJkBean.class);

    @Override
    protected void setup() {
        springboot.setSpringbootVersion("${springbootVersion}");
        springboot.projectPlugin().getProject().simpleFacade()
            .setCompileDependencies(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-web")
            )
            .setTestDependencies(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-test")
                    .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
            );
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanPack() {
        clean(); springboot.projectPlugin().pack();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(Build.class, args).cleanPack();
    }

}
