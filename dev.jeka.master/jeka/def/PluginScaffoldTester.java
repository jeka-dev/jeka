import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.system.JkLog;
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

    PluginScaffoldTester() {
        super("..");
    }

    void run() {
        Path dir = scaffold("scaffold#run springboot#  -springboot#scaffoldDefClasspath="
                        + sprinbootBluginJar + " @" + sprinbootBluginJar,
                "java#pack", false);
    }

    private Path scaffold(String scaffoldCmdLine, String checkCommandLine, boolean checkWithWrapper) {
        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runjeka(path.toString(), scaffoldCmdLine);
        runjeka(checkWithWrapper, path.toString(), checkCommandLine);
        return path;
    }

    public static void main(String[] args) throws Exception {
        JkLog.setDecorator(JkLog.Style.INDENT);
        new PluginScaffoldTester().run();
    }

}
