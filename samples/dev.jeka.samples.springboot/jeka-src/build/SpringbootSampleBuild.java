package build;

import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.plugins.springboot.JkSpringbootProject;
import dev.jeka.plugins.springboot.SpringbootKBean;

@JkDep("../../plugins/dev.jeka.plugins.springboot/jeka-output/dev.jeka.springboot-plugin.jar")
//@JkDep("dev.jeka:springboot-plugin")
public class SpringbootSampleBuild extends KBean {

    public String aa;

    @JkInjectRunbase
    ProjectKBean projectKBean;

    @JkInjectRunbase
    SpringbootKBean springbootKBean;

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.springboot-plugin.jar", "dev.jeka.plugins.springboot")
                .setModuleAttributes("dev.jeka.plugins.springboot", JkIml.Scope.COMPILE, null)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null)
                .setSuggestedJdk("17");
    }

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.compilation.dependencies
                .add("org.springframework.boot:spring-boot-starter-web")
                .add("org.springframework.boot:spring-boot-starter-data-jpa")
                .add("com.h2database:h2");

        projectKBean.project.testing.compilation.dependencies
                .add("org.springframework.boot:spring-boot-starter-test");

        projectKBean.project
                .setModuleId("dev.jeka:samples-springboot")
                .setVersion("0.0.1-SNAPSHOT");  // Snapshot is necessary otherwise it can not deploy twice in maven local repo
        JkSpringbootProject.of(projectKBean.project)
                .configure()
                .includeParentBom("3.3.4");
    }

    public void cleanPack() {
        projectKBean.clean();
        projectKBean.pack();
    }

    public void testRun() {
        System.out.println(this.aa);
        cleanPack();
        springbootKBean.load(ProjectKBean.class).runJar();
    }

    public static void main(String[] args) {
        JkInit.kbean(SpringbootSampleBuild.class, args).projectKBean.info();
    }

}
