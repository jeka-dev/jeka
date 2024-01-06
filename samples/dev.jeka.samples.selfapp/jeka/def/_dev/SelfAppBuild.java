package _dev;

import app.Application;
import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.self.SelfAppKBean;

@JkInjectClasspath("dev.jeka:springboot-plugin")
class SelfAppBuild extends SelfAppKBean {

    SelfAppBuild() {

        //load(SpringbootKBean.class);  // Needed only to produce bootable Spring-Boot jar.

        // setup intellij project to depends on module sources instead of jar
        // Only relevant for developing JeKa itself
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .replaceLibByModule("springboot-plugin-selfApp-SNAPSHOT.jar", "dev.jeka.plugins.springboot")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null)
                .setModuleAttributes("dev.jeka.plugins.springboot", JkIml.Scope.COMPILE, null);
    }

    @Override
    protected void init() {

        setMainClass(Application.class);

        dockerBuildCustomizers.add(dockerBuild -> dockerBuild
                .setBaseImage("eclipse-temurin:21.0.1_12-jre-jammy")
                .addExtraFile(getBaseDir().resolve("jeka/local.properties"), "/toto.txt")

                // The complete agent with all dependencies hasn't been found on
                // Maven central, so we use a local file.
                .addAgent(getBaseDir().resolve("jeka/opentelemetry-javaagent.jar"), "")
        );
    }

    public void clean() {
        cleanOutput();
    }

}