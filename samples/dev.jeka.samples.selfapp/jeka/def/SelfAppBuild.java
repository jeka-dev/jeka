import dev.jeka.core.api.tooling.intellij.JkIml;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.ide.IntellijKBean;
import dev.jeka.core.tool.builtins.self.SelfAppKBean;
import dev.jeka.plugins.springboot.JkSpringbootJars;

@JkInjectClasspath("dev.jeka:springboot-plugin")
class SelfAppBuild extends SelfAppKBean {

    SelfAppBuild() {

        // setup intellij project to depends on module sources instead of jar
        // Only relevant for developing JeKa itself
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
                .replaceLibByModule("springboot-plugin-selfApp-SNAPSHOT.jar", "dev.jeka.plugins.springboot")
                .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null)
                .setModuleAttributes("dev.jeka.plugins.springboot", JkIml.Scope.COMPILE, null);
    }

    @Override
    public void packJar() {
        JkSpringbootJars.createBootJar(
                classTree(),
                libs(),
                getRuntime().getDependencyResolver().getRepos(),
                jarPath());
    }

    public void clean() {
        cleanOutput();
    }

}