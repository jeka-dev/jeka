import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.system.JkProcHandler;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end tests about scaffolding.
 * Supposed to be run in dev.jeka.core working dir.
 */
class PluginScaffoldTester extends JekaCommandLineExecutor {

    private final String sprinbootPluginJar = Paths.get("../plugins/dev.jeka.plugins.springboot/jeka-output/" +
            "dev.jeka.springboot-plugin.jar").toAbsolutePath().normalize().toString();

    void run() {

        System.out.println("=================================");
        System.out.println("Scaffold Springboot tests =======");
        System.out.println("=================================");

        // Regular project
        String scaffoldCmd = scaffoldArgs("project: scaffold");
        String checkCmd = checkArgs("project: info pack version=0.0.1");
        RunChecker runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldCmd;
        runChecker.checkCmd = checkCmd;
        runChecker.run();

        // Project with simple layout
        scaffoldCmd = scaffoldArgs("springboot: project: layout.style=SIMPLE scaffold");
        checkCmd = checkArgs("project: pack");
        runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldCmd;
        runChecker.checkCmd = checkCmd;
        runChecker.run();

        // Project with self springboot
        scaffoldCmd = scaffoldArgs("base: scaffold springboot:");
        checkCmd = checkArgs("base: test pack");
        runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldCmd;
        runChecker.checkCmd = checkCmd;
        runChecker.cleanup = false;
        Path baseDir = runChecker.run();
        JkUtilsAssert.state(!Files.exists(baseDir.resolve("src")),
                "Self Springboot was scaffolded with project structure !");
        JkPathTree.of(baseDir).deleteRoot();
    }

    private class RunChecker {
        String scaffoldCmd;
        String checkCmd;
        boolean cleanup = true;
        boolean checkHttp = false;
        int checkHttpTimeout = 8000;
        int checkHttpSleep = 2000;
        String url = "http://localhost:8080";

        Path run() {

            Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
            runWithDistribJekaShell(path, scaffoldCmd);

            if (checkHttp) {
                JkUtilsAssert.state(!JkUtilsNet.isStatusOk(url, true), "A server is already listening to %s", url);

                System.out.println("======= Checking health with HTTP ================== ");
                System.out.println("Scaffold command " + scaffoldCmd);

                // launch checker in separate process
                JkProcHandler handler = prepareWithBaseDirJekaShell(path, checkCmd).execAsync();

                // try to get a Ok response
                JkUtilsNet.checkUntilOk(url, checkHttpTimeout, checkHttpSleep);

                // destroy the sub-process
                handler.getProcess().destroyForcibly();
                boolean ended = handler.waitFor(2000, TimeUnit.MILLISECONDS);
                JkUtilsAssert.state(ended, "Can't kill process");

            } else {
                runWithBaseDirJekaShell(path, checkCmd);
            }

            if (checkHttp) {
                JkPathTree.of(path).deleteRoot();
            }
            return path;
        }

    }

    private String scaffoldArgs(String original)  {

        // inject springboot-plugin.jar both as a jeka dependencies (for running plugin)
        // And as a substitute of  @JkInjectClasspath("${jeka.springboot.plugin.dependency}") in scaffolded project
        return String.format(original + " -Djeka.springboot.plugin.dependency=%s -cp=%s",
                sprinbootPluginJar,
                sprinbootPluginJar);
    }

    private String checkArgs(String original)  {

        // Needed to force starting springboot process with java 17
        return original + " -Djeka.java.version=17";
    }


}
