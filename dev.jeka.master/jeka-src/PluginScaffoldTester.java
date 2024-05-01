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
        String buildCmd = withJavaVersionArgs("project: info pack version=0.0.1");
        RunChecker runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldCmd;
        runChecker.buildCmd = buildCmd;
        runChecker.runCmd = withJavaVersionArgs("project: runJar");
        runChecker.run();

        // Project with simple layout
        scaffoldCmd = scaffoldArgs("springboot: project: layout.style=SIMPLE scaffold");
        buildCmd = withJavaVersionArgs("project: pack");
        runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldCmd;
        runChecker.buildCmd = buildCmd;
        runChecker.run();

        // Project with self springboot
        scaffoldCmd = scaffoldArgs("base: scaffold springboot:");
        buildCmd = withJavaVersionArgs("base: test pack");
        runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldCmd;
        runChecker.buildCmd = buildCmd;
        runChecker.cleanup = false;
        runChecker.runCmd = withJavaVersionArgs("project: runJar");
        Path baseDir = runChecker.run();
        JkUtilsAssert.state(!Files.exists(baseDir.resolve("src")),
                "Self Springboot was scaffolded with project structure !");
        JkPathTree.of(baseDir).deleteRoot();
    }

    private class RunChecker {
        String scaffoldCmd;
        String buildCmd;
        String runCmd;
        boolean cleanup = true;
        int checkHttpTimeout = 8000;
        int checkHttpSleep = 2000;
        String url = "http://localhost:8080";

        Path run() {

            Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
            runWithDistribJekaShell(path, scaffoldCmd);
            runWithDistribJekaShell(path, buildCmd);

            if (runCmd != null) {
                JkUtilsAssert.state(!JkUtilsNet.isStatusOk(url, true), "A server is already listening to %s", url);

                System.out.println("======= Checking health with HTTP ================== ");
                System.out.println("Run command " + runCmd);

                // launch checker in separate process
                JkProcHandler handler = prepareWithBaseDirJekaShell(path, runCmd)
                        .setCollectStdout(true)
                        .setCollectStderr(true)
                        .execAsync();

                // try to get a Ok response
                try {
                    JkUtilsNet.checkUntilOk(url, checkHttpTimeout, checkHttpSleep);
                } catch (RuntimeException e) {
                    System.out.println("=======Std out ================= ");
                    System.out.println(handler.getOutput());
                    System.out.println("=======Std err ================= ");
                    System.out.println(handler.getOutput());
                    throw e;
                }

                // destroy the sub-process
                handler.getProcess().destroyForcibly();
                boolean ended = handler.waitFor(2000, TimeUnit.MILLISECONDS);
                JkUtilsAssert.state(ended, "Can't kill process");

            } /*else {
                runJeka(!JkUtilsSystem.IS_WINDOWS, path, buildCmd);
            }
            */
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

    private String withJavaVersionArgs(String original)  {

        // Needed to force starting springboot process with java 17
        return original + " -Djeka.java.version=17";
    }


}
