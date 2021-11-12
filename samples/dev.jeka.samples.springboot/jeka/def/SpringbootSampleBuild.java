import dev.jeka.core.tool.*;
import dev.jeka.plugins.springboot.SpringbootJkBean;
import dev.jeka.plugins.springboot.JkSpringModules.Boot;


@JkDefClasspath("../../plugins/dev.jeka.plugins.springboot/jeka/output/dev.jeka.springboot-plugin.jar")
class SpringbootSampleBuild extends JkClass {

    private final SpringbootJkBean springboot = getJkBean(SpringbootJkBean.class);

    @Override
    protected void setup() {
        springboot.setSpringbootVersion("2.5.5");
        springboot.projectPlugin().getProject().simpleFacade()
                .setCompileDependencies(deps -> deps
                    .and(Boot.STARTER_WEB)  // Same as .and("org.springframework.boot:spring-boot-starter-web")
                    .and(Boot.STARTER_DATA_JPA)
                    .and(Boot.STARTER_DATA_REST)
                    .and("com.google.guava:guava:30.0-jre")
                )
                .setRuntimeDependencies(deps -> deps
                    .and("com.h2database:h2:1.4.200")
                )
                .setTestDependencies(deps -> deps
                    .and(Boot.STARTER_TEST)
                );
    }

    public void cleanPack() {
        clean(); springboot.createBootJar();
    }

    public void testRun() {
        cleanPack();
        springboot.run();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(SpringbootSampleBuild.class, args).cleanPack();
    }

}
