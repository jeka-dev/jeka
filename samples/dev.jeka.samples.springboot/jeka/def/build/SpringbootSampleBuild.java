package build;

import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.plugins.springboot.JkSpringbootProject;
import dev.jeka.plugins.springboot.SpringbootKBean;

@JkInjectClasspath("../../plugins/dev.jeka.plugins.springboot/jeka/output/dev.jeka.springboot-plugin.jar")
public class SpringbootSampleBuild extends KBean {

    public String aa;

    ProjectKBean projectKBean = load(ProjectKBean.class);

    SpringbootKBean springbootKBean = load(SpringbootKBean.class);

    SpringbootSampleBuild() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.springboot-plugin.jar", "dev.jeka.plugins.springboot")
                .setModuleAttributes("dev.jeka.plugins.springboot", JkIml.Scope.COMPILE, null)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null)
                .setSuggestedJdk("17");
    }

    @Override
    protected void init() {
        projectKBean.project.flatFacade()
                .setJvmTargetVersion(JkJavaVersion.V17)
                .addCompileDeps(
                        "org.springframework.boot:spring-boot-starter-web",
                        "org.springframework.boot:spring-boot-starter-data-jpa"
                )
                .addRuntimeDeps(
                        "com.h2database:h2:1.4.200"
                )
                .addTestDeps(
                        "org.springframework.boot:spring-boot-starter-test"
                )
                .setModuleId("dev.jeka:samples-springboot")
                .setVersion("1.0-SNAPSHOT");
        JkSpringbootProject.of(projectKBean.project)
                .configure()
                .includeParentBom("3.2.0");
    }


    public void cleanPack() {
        projectKBean.cleanPack();
    }

    public void testRun() {
        System.out.println(this.aa);
        cleanPack();
        springbootKBean.load(ProjectKBean.class).runJar();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        SpringbootSampleBuild build = JkInit.instanceOf(SpringbootSampleBuild.class, args, "-ls=BRACE");
        build.cleanPack();
        //build.load(ProjectKBean.class).publishLocal();
    }

}
