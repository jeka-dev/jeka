import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class PluginScaffoldTester extends JekaCommandLineExecutor {

    private final String sprinbootPluginJar = Paths.get("../plugins/dev.jeka.plugins.springboot/jeka-output/" +
            "dev.jeka.springboot-plugin.jar").toAbsolutePath().normalize().toString();

    PluginScaffoldTester(JkProperties properties) {
        super(Paths.get(".."), properties);
    }

    void run() {
        Path dir = scaffold(
                "-lsu scaffold#run springboot# springboot#scaffoldDefClasspath=" + sprinbootPluginJar
                        + " @" + sprinbootPluginJar,
                "project#info project#pack -lsu project#version=0.0.1"
                , false);
        String jdk17 = properties.get("jeka.jdk.17");
        if (!JkUtilsString.isBlank(jdk17)) {   // No JDK 17 set on github actions
            runDistribJeka(dir.toString(), "intellij#iml", jdk17);
        }
    }

    private Path scaffold(String scaffoldCmdLine, String checkCommandLine, boolean checkWithWrapper) {
        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runDistribJeka(path.toString(), scaffoldCmdLine);
        String jdk17 = properties.get("jeka.jdk.17");
        if (!JkUtilsString.isBlank(jdk17)) {   // No JDK 17 set on github actions
            runJeka(checkWithWrapper, path.toString(), checkCommandLine, jdk17);
        }
        return path;
    }

}