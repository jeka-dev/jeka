import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.ide.IntellijJkBean;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;
import dev.jeka.plugins.springboot.SpringbootJkBean;
import dev.jeka.plugins.springboot.JkSpringModules.Boot;


@JkInjectClasspath("../../plugins/dev.jeka.plugins.springboot/jeka/output/dev.jeka.springboot-plugin.jar")
class SpringbootSampleBuild extends JkBean {

    private final SpringbootJkBean springboot = getBean(SpringbootJkBean.class);

    private final IntellijJkBean intellijJkBean = getBean(IntellijJkBean.class);

    SpringbootSampleBuild() {
        springboot.setSpringbootVersion("2.5.5");
        springboot.projectBean().configure(this::configure);
        intellijJkBean.configureImlGenerator(imlGenerator -> imlGenerator.setExcludeJekaLib(true));
        intellijJkBean.configureIml(this::configure);
    }

    private void configure(JkProject project) {
        project.simpleFacade()
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
                )
                .setPublishedModuleId("dev.jeka:samples-springboot")
                .setPublishedVersion("1.0-SNAPSHOT");

    }

    private void configure(JkIml iml) {
        iml.getComponent().replaceLibByModule("dev.jeka.springboot-plugin.jar",
                "dev.jeka.plugins.springboot");
    }

    public void cleanPack() {
        clean(); springboot.projectBean().pack();
    }

    public void testRun() {
        cleanPack();
        springboot.run();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        SpringbootSampleBuild build = JkInit.instanceOf(SpringbootSampleBuild.class, args, "-ls=BRACE", "-lb");
        //build.getBean(SpringbootJkBean.class).createWar = true;
        build.cleanPack();
        build.getBean(ProjectJkBean.class).publishLocal();
    }

    // debug purpose
    static class Iml {
        public static void main(String[] args) {
            JkInit.instanceOf(SpringbootSampleBuild.class, args, "-ls=BRACE", "-lb")
                    .getRuntime().getBean(IntellijJkBean.class).iml();
        }
    }

}
