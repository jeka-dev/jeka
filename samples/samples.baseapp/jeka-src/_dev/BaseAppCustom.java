package _dev;

import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.plugins.springboot.SpringbootKBean;

@JkDep("org.springframework.boot:spring-boot-starter-test")

@JkDep("../../plugins/plugins.springboot/jeka-output/dev.jeka.springboot-plugin.jar")
class BaseAppCustom extends KBean {

    public void clean() {
        cleanOutput();
    }

    @JkPostInit(required = true)
    private void postInit(SpringbootKBean springbootKBean) {
    }

    @JkPostInit
    private void postInit(IntellijKBean intellijKBean) {
        intellijKBean
                .replaceLibByModule("dev.jeka.jeka-core.jar", "core")
                .setModuleAttributes("core", JkIml.Scope.COMPILE, null)
                .replaceLibByModule("springboot-plugin-SNAPSHOT.jar", "plugins.springboot")
                .setModuleAttributes("plugins.springboot", JkIml.Scope.COMPILE, null);
    }

    @JkPostInit
    private void postInit(DockerKBean dockerKBean) {
        dockerKBean.customizeJvmImage(dockerBuild -> {
            dockerBuild.addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.32.0", "");
            dockerBuild.setBaseImage("eclipse-temurin:21.0.1_12-jre-jammy");

            // Customize Dockerfile
            dockerBuild.dockerfileTemplate
                    .moveCursorBefore("ENTRYPOINT ")
                    .add("## my comment");
        });
    }

}