package _dev;

import dev.jeka.core.api.tooling.docker.JkDockerJvmBuild;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;
import dev.jeka.plugins.springboot.SpringbootKBean;

@JkDep("org.springframework.boot:spring-boot-starter-test")

@JkDep("../../plugins/dev.jeka.plugins.springboot/jeka-output/dev.jeka.springboot-plugin.jar")
class BaseAppBuild extends KBean {

    BaseAppBuild() {

        load(SpringbootKBean.class);  // Needed only to produce bootable Spring-Boot jar and add default port

        // setup intellij project to depends on module sources instead of jar
        // Only relevant for developing JeKa itself
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .replaceLibByModule("springboot-plugin-SNAPSHOT.jar", "dev.jeka.plugins.springboot")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null)
                .setModuleAttributes("dev.jeka.plugins.springboot", JkIml.Scope.COMPILE, null);
    }

    @Override
    protected void init() {
        this.find(DockerKBean.class).ifPresent(dockerKBean ->
            dockerKBean.customizeJvmImage(this::customizeDockerBuild)
        );
    }

    public void clean() {
        cleanOutput();
    }

    private void customizeDockerBuild(JkDockerJvmBuild dockerBuild) {
        dockerBuild.addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.32.0", "");
        dockerBuild.setBaseImage("eclipse-temurin:21.0.1_12-jre-jammy");

        // Customize Dockerfile
        dockerBuild.dockerfileTemplate
                .moveCursorBefore("ENTRYPOINT ")
                .add("## my comment");
    }

}