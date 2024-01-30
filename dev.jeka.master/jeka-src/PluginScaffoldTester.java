import dev.jeka.core.JekaCommandLineExecutor;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkProcHandler;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsHttp;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsSystem;
import sun.java2d.loops.ProcessPath;

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
        String scaffoldCmd = scaffoldArgs("project# springboot#scaffold");
        String checkCmd = checkArgs("project#info project#pack -lsu project#version=0.0.1");
        RunChecker runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldCmd;
        runChecker.checkCmd = checkCmd;
        runChecker.checkHttpTimeout = 25*1000;
        runChecker.run();

        // Project with simple layout
        scaffoldCmd = scaffoldArgs("project#layout.style=SIMPLE springboot#scaffold");
        checkCmd = checkArgs("project#pack");
        runChecker = new RunChecker();
        runChecker.scaffoldCmd = scaffoldCmd;
        runChecker.checkCmd = checkCmd;
        runChecker.checkHttpTimeout = 8*1000;
        runChecker.run();
    }

    private class RunChecker {
        String scaffoldCmd;
        String checkCmd;
        boolean checkHttp = false;
        int checkHttpTimeout = 8000;
        int checkHttpSleep = 2000;
        String url = "http://localhost:8080";

        void run() {

            Path path = JkUtilsPath.createTempDirectory("jeka-scaffold-test-");
            runWithDistribJekaShell(path, scaffoldCmd);

            JkUtilsAssert.state(!JkUtilsHttp.isStatusOk(url), "A server is already listening to %s", url);


            if (checkHttp) {
                System.out.println("======= Checking health with HTTP ================== ");
                System.out.println("Scaffold command " + scaffoldCmd);

                // launch checker in separate process
                JkProcHandler handler = prepareWithBaseDirJekaShell(path, checkCmd).execAsync();

                // try to get a Ok response
                JkUtilsHttp.checkUntilOk(url, checkHttpTimeout, checkHttpSleep);

                // destroy the sub-process
                handler.getProcess().destroyForcibly();
                boolean ended = handler.waitFor(2000, TimeUnit.MILLISECONDS);
                JkUtilsAssert.state(ended, "Can't kill process");

            } else {
                runWithBaseDirJekaShell(path, checkCmd);
            }


            JkPathTree.of(path).deleteRoot();
        }

    }

    private String scaffoldArgs(String original)  {

        // inject springboot-plugin.jar both as a jeka dependencies (for running plugin)
        // And as a substitute of  @JkInjectClasspath("${jeka.springboot.plugin.dependency}") in scaffolded project
        return String.format(original + " -Djeka.springboot.plugin.dependency=%s +%s",
                sprinbootPluginJar,
                sprinbootPluginJar);
    }

    private String checkArgs(String original)  {

        // Needed to force starting springboot process with java 17
        return original + " -Djeka.java.version=17";
    }


}
