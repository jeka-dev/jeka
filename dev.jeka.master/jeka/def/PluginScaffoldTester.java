import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class PluginScaffoldTester extends JekaCommandLineExecutor {

    private String sprinbootBluginJar = Paths.get("../plugins/dev.jeka.plugins.springboot/jeka/"
            + "output/dev.jeka.springboot-plugin.jar").toAbsolutePath().normalize().toString();

    PluginScaffoldTester(JkProperties properties) {
        super("..", properties);
    }

    void run() {
        Path dir = scaffold("-lsu scaffold#run springboot#  springboot#scaffoldDefClasspath="
                        + sprinbootBluginJar + " @" + sprinbootBluginJar,
                "project#pack -lsu", false);
        if (properties.get("jeka.jdk.17") != null) {   // No JDK 17 set on github actions
            runJeka(dir.toString(), "intellij#iml");
        }
    }

    private Path scaffold(String scaffoldCmdLine, String checkCommandLine, boolean checkWithWrapper) {
        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runJeka(path.toString(), scaffoldCmdLine);
        runJeka(checkWithWrapper, path.toString(), checkCommandLine);
        return path;
    }

}
