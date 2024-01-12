package _dev;

import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.self.SelfAppKBean;
import dev.jeka.core.tool.builtins.tools.DockerKBean;
import dev.jeka.plugins.springboot.SpringbootKBean;

@JkInjectClasspath("org.springframework.boot:spring-boot-starter-test")

@JkInjectClasspath("dev.jeka:springboot-plugin")
class SelfAppBuild extends SelfAppKBean {

    SelfAppBuild() {

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

        // configure image
        load(DockerKBean.class).dockerBuild
                .setBaseImage("eclipse-temurin:21.0.1_12-jre-jammy")
                .addExtraFile(getBaseDir().resolve("jeka/local.properties"), "/toto.txt")
                .addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.32.0", "");
    }

    public void clean() {
        cleanOutput();
    }

}