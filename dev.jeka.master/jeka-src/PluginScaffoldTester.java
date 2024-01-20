import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.plugins.springboot.JkSpringbootProject;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class PluginScaffoldTester extends JekaCommandLineExecutor {

    private final String sprinbootPluginJar = Paths.get("../plugins/dev.jeka.plugins.springboot/jeka-output/" +
            "dev.jeka.springboot-plugin.jar").toAbsolutePath().normalize().toString();

    void run() {

        // inject springboot-plugin.jar both as a jeka dependencies (for running plugin)
        // And as a substitute of  @JkInjectClasspath("${jeka.springboot.plugin.dependency}") in scaffolded project
        String scaffoldCmd = String.format("-lsu scaffold#run springboot# -D%s=%s @%s"
                + " scaffold#jekaPropsExtraValues=%s",
                JkSpringbootProject.OVERRIDE_SCAFFOLDED_SPRINGBOOT_PLUGIN_DEPENDENCY_PROP_NAME,
                sprinbootPluginJar,
                sprinbootPluginJar,
                "jeka.java.version=17");
        String checkCmd = "project#info project#pack -lsu project#version=0.0.1";

        Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
        runWithDistribJekaShell(path, scaffoldCmd);
        runWithBaseDirJekaShell(path, checkCmd);
        JkPathTree.of(path).deleteRoot();
    }

}
