import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.plugins.springboot.SpringbootJkBean;
import dev.jeka.plugins.springboot.JkSpringModules.Boot;


@JkInjectClasspath("../../plugins/dev.jeka.plugins.springboot/jeka/output/dev.jeka.springboot-plugin.jar")
class SpringbootSampleBuild extends JkBean {

    private final SpringbootJkBean springboot = getRuntime().getBean(SpringbootJkBean.class);

    private final IntellijJkBean intellijJkBean = getRuntime().getBean(IntellijJkBean.class);

    @Override
    protected void init() {
        springboot.setSpringbootVersion("2.5.5");
        springboot.projectBean().getProject().simpleFacade()
                .configureCompileDeps(deps -> deps
                    .and(Boot.STARTER_WEB)  // Same as .and("org.springframework.boot:spring-boot-starter-web")
                    .and(Boot.STARTER_DATA_JPA)
                    .and(Boot.STARTER_DATA_REST)
                    .and("com.google.guava:guava:30.0-jre")
                )
                .configureRuntimeDeps(deps -> deps
                    .and("com.h2database:h2:1.4.200")
                )
                .configureTestDeps(deps -> deps
                    .and(Boot.STARTER_TEST)
                );
        intellijJkBean.configureImlGenerator(imlGenerator -> imlGenerator.setSkipJeka(true));
        intellijJkBean.configureIml(this::configure);
    }

    private void configure(JkIml iml) {
        iml.getComponent().replaceLibByModule("dev.jeka.springboot-plugin.jar",
                "dev.jeka.plugins.springboot");
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
        JkInit.instanceOf(SpringbootSampleBuild.class, args, "-ls=BRACE", "-lb").cleanPack();
    }

    // debug purpose
    static class Iml {
        public static void main(String[] args) {
            JkInit.instanceOf(SpringbootSampleBuild.class, args, "-ls=BRACE", "-lb")
                    .getRuntime().getBean(IntellijJkBean.class).iml();
        }
    }

}
